const express = require("express");
const router = express.Router();

const { scanUrl } = require("./urlScanner");
const { scanPhone } = require("./phoneScanner");
const { scanEmail } = require("./emailScanner");
const { pool } = require("./db");

const SUPPORTED_TYPES = ["URL", "PHONE", "EMAIL"];

// POST /api/scan
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
                message: "Loại dữ liệu không được hỗ trợ"
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
                    ruleScore: 0,
                    reasons: scanResult?.reasons || []
                }
            });
        }

        // 5. Kiểm tra blacklist
        const [result] = await pool.execute(
            `INSERT INTO scan_requests
             (input_type, input_value, status)
             VALUES (?, ?, 'PROCESSING')`,
            [type, value]
        );

        const scanId = result.insertId;

        const [blacklist] = await pool.execute(
            `SELECT *
             FROM blacklist
             WHERE type = ?
               AND value = ?
               AND status = 'VERIFIED'
             LIMIT 1`,
            [type, value]
        );

        const blacklistMatch = blacklist.length > 0;

        // 6. Tính điểm cuối cùng
        const ruleScore = Math.min(
            100,
            Math.max(0, Number(scanResult.ruleScore) || 0)
        );

        const score = blacklistMatch ? 100 : ruleScore;

        // 7. Thống nhất ngưỡng rủi ro với các scanner
        let riskLevel = "LOW";

        if (score >= 80) {
            riskLevel = "CRITICAL";
        } else if (score >= 60) {
            riskLevel = "HIGH";
        } else if (score >= 30) {
            riskLevel = "MEDIUM";
        }

        // Chỉ đánh dấu là scam nếu có trong blacklist đã xác minh
        // hoặc điểm rule đạt ngưỡng CRITICAL.
        const isScam = blacklistMatch || score >= 80;

        // 8. Tạo giải thích
        let explanation;

        if (blacklistMatch) {
            explanation = "Dữ liệu đã được tìm thấy trong blacklist.";
        } else {
            explanation = (scanResult.reasons || []).join(" ") ||
                "Chưa phát hiện dấu hiệu đáng ngờ.";
        }

        // 9. Lưu kết quả phát hiện
        await pool.execute(
            `INSERT INTO detection_results
             (
                 scan_id,
                 blacklist_match,
                 matched_blacklist_id,
                 rule_score,
                 ai_score,
                 final_score,
                 risk_level,
                 is_scam,
                 explanation
             )
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [
                scanId,
                blacklistMatch ? 1 : 0,
                blacklistMatch ? blacklist[0].id_blacklist : null,
                ruleScore,
                0,
                score,
                riskLevel,
                isScam ? 1 : 0,
                explanation
            ]
        );

        // 10. Cập nhật trạng thái lượt quét
        await pool.execute(
            `UPDATE scan_requests
             SET status = 'COMPLETED'
             WHERE scan_id = ?`,
            [scanId]
        );

        // 11. Trả kết quả về Android
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
                    value,
                valid: scanResult.valid,
                blacklistMatch,
                ruleScore,
                score,
                riskLevel,
                isSuspicious: scanResult.isSuspicious,
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
            message: "Lỗi server khi kiểm tra dữ liệu"
        });
    }
});

module.exports = router;