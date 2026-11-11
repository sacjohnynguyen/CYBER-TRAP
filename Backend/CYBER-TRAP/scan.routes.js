const express = require("express");
const router = express.Router();

const { scanUrl } = require("./urlScanner");
const { scanPhone } = require("./phoneScanner");
const { scanEmail } = require("./emailScanner");
const { scanContent } = require("./contentScanner");
const { pool } = require("./db");

const SUPPORTED_TYPES = ["URL", "PHONE", "EMAIL", "CONTENT", "TEXT"];

// POST /api/scan hoặc /api/v1/scan
router.post("/", async (req, res) => {
    try {
        const { inputType, inputValue } = req.body || {};

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
                scanResult = scanPhone(value);
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
                    scanResult?.reasons?.join(" ") ||
                    "Dữ liệu không hợp lệ",
                data: {
                    inputType: type,
                    inputValue: value,
                    valid: false,
                    riskLevel: "UNKNOWN",
                    isSuspicious: false,
                    reasons: scanResult?.reasons || []
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

        // 6. Kiểm tra blacklist đã xác minh trong CSDL
        let blacklistMatch = false;
        try {
            const [blacklist] = await pool.execute(
                `SELECT *
                 FROM blacklist
                 WHERE type = ?
                   AND value = ?
                   AND status = 'VERIFIED'
                 LIMIT 1`,
                [type, value]
            );
            blacklistMatch = blacklist.length > 0;
        } catch (dbErr) {
            console.warn("⚠️ Không thể truy vấn bảng blacklist:", dbErr.message);
        }

        // 7. Xác định kết quả
        const riskLevel = blacklistMatch
            ? "CRITICAL"
            : (scanResult.riskLevel || "UNKNOWN");

        const isScam = blacklistMatch || Boolean(scanResult.isScam);

        const isSuspicious = blacklistMatch
            ? true
            : Boolean(scanResult.isSuspicious);

        // 8. Tạo giải thích
        let explanation;
        if (blacklistMatch) {
            explanation = "Dữ liệu đã được tìm thấy trong danh sách đen (Blacklist) đã xác minh nguy hiểm.";
        } else {
            explanation =
                (scanResult.reasons || []).join(" ") ||
                "Chưa có đủ thông tin để kết luận dữ liệu là lừa đảo.";
        }

        // 9. Trả kết quả đồng nhất về cho Android Retrofit
        return res.json({
            success: true,
            data: {
                scanId,
                inputType: type,
                inputValue: value,
                normalizedValue:
                    scanResult.normalizedUrl ||
                    scanResult.normalizedPhone ||
                    scanResult.normalizedEmail ||
                    scanResult.normalizedContent ||
                    value,
                valid: scanResult.valid,
                blacklistMatch,
                riskLevel,
                isSuspicious,
                isScam,
                reasons: scanResult.reasons || [],
                matchedKeywords: scanResult.matchedKeywords || [],
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
