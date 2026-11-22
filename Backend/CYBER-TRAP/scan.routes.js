const express = require("express");
const router = express.Router();

const { scanUrl } = require("./urlScanner");
const { scanPhone } = require("./phoneScanner");
const { scanEmail } = require("./emailScanner");
const { scanContent } = require("./contentScanner");
const { pool } = require("./db");

const SUPPORTED_TYPES = ["URL", "PHONE", "EMAIL", "CONTENT", "TEXT"];

// GET /api/v1/scan/stats - Lấy số liệu thống kê thực tế từ MySQL
router.get("/stats", async (req, res) => {
    try {
        const [r1] = await pool.query("SELECT COUNT(*) AS total FROM scam_reports WHERE DATE(created_at) = CURDATE()");
        const [r2] = await pool.query("SELECT COUNT(*) AS total FROM blacklist");
        const [r3] = await pool.query("SELECT COUNT(*) AS total FROM users");

        return res.json({
            success: true,
            data: {
                todayReports: r1[0].total || 0,
                blockedNumbers: r2[0].total || 0,
                totalUsers: r3[0].total || 0
            }
        });
    } catch (err) {
        return res.json({
            success: true,
            data: { todayReports: 0, blockedNumbers: 0, totalUsers: 0 }
        });
    }
});

// POST /api/scan hoặc /api/v1/scan
router.post("/", async (req, res) => {
    try {
        const { inputType, inputValue, content } = req.body || {};

        // 1. Kiểm tra dữ liệu đầu vào
        if (
            typeof inputType !== "string" ||
            typeof inputValue !== "string" ||
            !inputValue.trim()
        ) {
            return res.status(400).json({
                success: false,
                message: "Thiếu hoặc sai định dạng dữ liệu đầu vào"
            });
        }

        const type = inputType.trim().toUpperCase();
        const value = inputValue.trim();

        // 2. Kiểm tra loại dữ liệu
        if (!SUPPORTED_TYPES.includes(type)) {
            return res.status(400).json({
                success: false,
                message: "Loại dữ liệu không được hỗ trợ. Hỗ trợ: URL, PHONE, EMAIL, CONTENT"
            });
        }

        // 3. Chạy scanner tương ứng
        let scanResult;

        switch (type) {
            case "URL":
                scanResult = scanUrl(value);
                break;

            case "PHONE":
                scanResult = scanPhone(value, { content });
                break;

            case "EMAIL":
                scanResult = scanEmail(value);
                break;

            case "CONTENT":
            case "TEXT":
                scanResult = scanContent(value);
                break;
        }

        // 4. Không lưu dữ liệu sai định dạng vào database
        if (!scanResult || scanResult.valid !== true) {
            return res.status(400).json({
                success: false,
                message:
                    (scanResult?.detection_reasons || scanResult?.reasons || []).join(" ") ||
                    "Dữ liệu không hợp lệ",
                data: {
                    inputType: type,
                    inputValue: value,
                    valid: false,
                    riskLevel: "UNKNOWN",
                    isSuspicious: false,
                    reasons: scanResult?.detection_reasons || scanResult?.reasons || []
                }
            });
        }

        // 5. Lưu yêu cầu quét vào database
        let scanId = null;
        try {
            const [result] = await pool.execute(
                `INSERT INTO scan_requests (input_type, input_value, status)
                 VALUES (?, ?, 'PROCESSING')`,
                [type, value]
            );
            scanId = result.insertId;
        } catch (dbErr) {
            console.warn("⚠️ Không thể ghi vào bảng scan_requests:", dbErr.message);
        }

        // Tạo biến altValue để tìm kiếm linh hoạt cả định dạng 0x lẫn +84x cho SĐT
        let altValue = value;
        if (type === "PHONE") {
            if (value.startsWith("0")) {
                altValue = "+84" + value.substring(1);
            } else if (value.startsWith("+84")) {
                altValue = "0" + value.substring(3);
            }
        }

        // 6. Kiểm tra danh sách đen Blacklist đã xác minh trong CSDL
        let blacklistMatch = false;
        let blacklistReason = null;
        try {
            const [blacklist] = await pool.execute(
                `SELECT *
                 FROM blacklist
                 WHERE type = ?
                   AND (value = ? OR value = ?)
                   AND status = 'VERIFIED'
                 LIMIT 1`,
                [type, value, altValue]
            );
            if (blacklist.length > 0) {
                blacklistMatch = true;
                blacklistReason = blacklist[0].reason || "Dữ liệu có trong danh sách đen (Blacklist) đã xác minh nguy hiểm.";
            }
        } catch (dbErr) {
            console.warn("⚠️ Không thể truy vấn bảng blacklist:", dbErr.message);
        }

        // 7. Kiểm tra báo cáo lừa đảo tập trung (scam_reports_centralized)
        let totalReportsCount = 0;
        let centralizedCategory = null;
        try {
            const [centralized] = await pool.execute(
                `SELECT total_reports_count, category
                 FROM scam_reports_centralized
                 WHERE type = ? AND (value = ? OR value = ?)
                 LIMIT 1`,
                [type, value, altValue]
            );
            if (centralized.length > 0) {
                totalReportsCount = centralized[0].total_reports_count || 1;
                centralizedCategory = centralized[0].category;
            }
        } catch (dbErr) {
            console.warn("⚠️ Không thể truy vấn scam_reports_centralized:", dbErr.message);
        }

        // 8. Tổng hợp điểm rủi ro & Lý do phát hiện (Combine Scanner + DB Rules)
        let riskScore = scanResult.risk_score !== undefined ? scanResult.risk_score : 10;
        let detectionReasons = Array.from(scanResult.detection_reasons || scanResult.reasons || []);
        let dataStatus = scanResult.data_status || "UNVERIFIED";

        if (blacklistMatch) {
            riskScore = 95;
            dataStatus = "VERIFIED";
            if (!detectionReasons.includes("Số điện thoại nằm trong danh sách đen (Blacklist) đã xác minh")) {
                detectionReasons.unshift("Số điện thoại nằm trong danh sách đen (Blacklist) đã xác minh");
            }
            if (blacklistReason && !detectionReasons.includes(blacklistReason)) {
                detectionReasons.push(blacklistReason);
            }
        }

        if (totalReportsCount > 0) {
            riskScore = Math.max(riskScore, totalReportsCount >= 5 ? 90 : 70);
            if (centralizedCategory) {
                detectionReasons.push(`Có ${totalReportsCount} báo cáo vi phạm từ cộng đồng (${centralizedCategory}).`);
            } else {
                detectionReasons.push(`Có ${totalReportsCount} báo cáo vi phạm từ cộng đồng.`);
            }
        }

        // Xếp loại Risk Level & IsScam
        let riskLevel = "LOW";
        let isScam = false;
        let isSuspicious = false;

        if (riskScore >= 70 || blacklistMatch) {
            riskLevel = "CRITICAL";
            isScam = true;
            isSuspicious = true;
        } else if (riskScore >= 40) {
            riskLevel = "HIGH";
            isSuspicious = true;
        } else if (riskScore >= 20) {
            riskLevel = "MEDIUM";
            isSuspicious = true;
        }

        // Tạo lời khuyên (Recommendation)
        let recommendation = scanResult.recommendation || "Chưa phát hiện dấu hiệu lừa đảo. Cần cảnh giác khi giao dịch.";
        if (riskLevel === "CRITICAL") {
            recommendation = "Không nghe máy hoặc gọi lại. Không cung cấp OTP/mật khẩu hay chuyển tiền. Tắt máy, chặn số và báo cáo ngay.";
        } else if (riskLevel === "HIGH") {
            recommendation = "Số điện thoại có nhiều dấu hiệu nghi vấn lừa đảo. Cần thận trọng xác minh danh tính người gọi.";
        }

        // Tạo giải thích tổng hợp cho Android
        let explanation = detectionReasons.join(" ");

        // 9. Trả về kết quả JSON đáp ứng 100% định dạng Agent Specification
        return res.json({
            success: true,
            phone_number: scanResult.phone_number || value,
            country: scanResult.country || "Vietnam",
            phone_type: scanResult.phone_type || "Mobile",
            risk_score: riskScore,
            risk_level: riskLevel,
            is_scam: isScam,
            detection_reasons: detectionReasons,
            recommendation: recommendation,
            data_status: dataStatus,
            // Cấu hình bổ sung tương thích với Retrofit Client Android
            data: {
                scanId,
                inputType: type,
                inputValue: value,
                normalizedValue: scanResult.phone_number || scanResult.normalizedUrl || scanResult.normalizedEmail || value,
                valid: scanResult.valid,
                blacklistMatch,
                totalReports: totalReportsCount,
                category: centralizedCategory || "Số điện thoại lừa đảo",
                riskLevel,
                riskScore,
                isSuspicious,
                isScam,
                reasons: detectionReasons,
                explanation
            }
        });
    } catch (error) {
        console.error("Lỗi POST /api/scan:", error);

        return res.status(500).json({
            success: false,
            message: "Lỗi server khi kiểm tra dữ liệu: " + error.message
        });
    }
});

module.exports = router;
