const express = require("express");
const router = express.Router();
const { scanUrl } = require("./urlScanner");
const { pool } = require("../db");
const { scanPhone } = require("./phoneScanner");
const { scanEmail } = require("./emailScanner");
router.post("/", async (req, res) => {

    try {

        const {
            inputType,
            inputValue
        } = req.body;

        if (!inputType || !inputValue) {

            return res.status(400).json({
                success: false,
                message: "Thiếu dữ liệu"
            });

        }
        let scanResult;

        if (inputType === "URL") {
            scanResult = scanUrl(inputValue);
        }

        if (inputType === "PHONE") {
            scanResult = scanPhone(inputValue);
        }

        if (inputType === "EMAIL") {
            scanResult = scanEmail(inputValue);
        }


        // =================================================
        // KIỂM TRA LOẠI DỮ LIỆU
        // =================================================

        if (
            inputType !== "URL" && inputType !== "PHONE" && inputType !== "EMAIL") {
            return res.status(400).json({
                success: false,
                message: "Loại dữ liệu không được hỗ trợ"
            });
        }
        // Lưu yêu cầu quét
        const [result] = await pool.execute(
            `INSERT INTO scan_requests
            (input_type, input_value, status)
            VALUES (?, ?, 'PROCESSING')`,
            [
                inputType,
                inputValue
            ]
        );

        const scanId = result.insertId;

        // =========================
        // 1. KIỂM TRA BLACKLIST
        // =========================

        const [blacklist] = await pool.execute(
            `SELECT *
             FROM blacklist
             WHERE type = ?
             AND value = ?
             AND status = 'VERIFIED'
             LIMIT 1`,
            [
                inputType,
                inputValue
            ]
        );

        let blacklistMatch = blacklist.length > 0;

        // =========================
        // 2. TÍNH ĐIỂM
        // =========================

        let ruleScore = 0;

        if (
            inputType === "URL" ||
            inputType === "PHONE" ||
            inputType === "EMAIL"
        ) {
            ruleScore = scanResult.ruleScore;
        }


        // Điểm cuối cùng
        let score = ruleScore;


        // Nếu nằm trong blacklist thì cho 100 điểm
        if (blacklistMatch) {
            score = 100;
        }

        // =========================
        // 3. XÁC ĐỊNH RỦI RO
        // =========================

        let riskLevel = "LOW";
        let isScam = false;

        if (score >= 90) {

            riskLevel = "CRITICAL";
            isScam = true;

        } else if (score >= 70) {

            riskLevel = "HIGH";
            isScam = true;

        } else if (score >= 40) {

            riskLevel = "MEDIUM";

        } else if (score > 0) {

            riskLevel = "LOW";
        }

        // =========================
        // 4. LƯU KẾT QUẢ
        // =========================

        let explanation;

        if (blacklistMatch) {

            explanation = "Dữ liệu đã được tìm thấy trong blacklist.";

        } else if (
            (inputType === "URL" || inputType === "PHONE" || inputType === "EMAIL") && scanResult.reasons) {

            explanation = scanResult.reasons.join(" ");

        } else {

            explanation = "Chưa phát hiện dấu hiệu đáng ngờ.";

        }
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
                blacklistMatch
                    ? blacklist[0].id_blacklist
                    : null,
                ruleScore,
                0,
                score,
                riskLevel,
                isScam ? 1 : 0,
                explanation
            ]
        );

        // =========================
        // 5. UPDATE SCAN
        // =========================

        await pool.execute(
            `UPDATE scan_requests
             SET status = 'COMPLETED'
             WHERE scan_id = ?`,
            [scanId]
        );

        // =========================
        // 6. TRẢ KẾT QUẢ
        // =========================

        res.json({

            success: true,

            data: {
                ruleScore,

                scanId,

                inputType,

                inputValue,

                blacklistMatch,

                score,

                riskLevel,

                isScam,

                explanation
            }

        });

    } catch (error) {

        console.error(error);

        res.status(500).json({

            success: false,

            message: "Lỗi server"

        });

    }

});

module.exports = router;