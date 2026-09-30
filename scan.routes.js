const express = require("express");
const router = express.Router();

const { pool } = require("../db");

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

        let score = 0;

        if (blacklistMatch) {
            score = 100;
        }

        // =========================
        // 3. XÁC ĐỊNH RỦI RO
        // =========================

        let riskLevel = "UNKNOWN";
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

        const explanation = blacklistMatch
            ? "Dữ liệu đã được tìm thấy trong blacklist."
            : "Chưa phát hiện dữ liệu trong blacklist.";

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
                0,
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