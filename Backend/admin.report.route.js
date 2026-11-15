const express = require("express");
const jwt = require("jsonwebtoken");
const { pool } = require("./db");

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || "cyber_trap_jwt_secret_key_2026";
const TRUST_SCORE_PENALTY = 5;
const MIN_TRUST_SCORE = 0;

function getAuthenticatedUserId(req) {
    const authorization = req.get("authorization");
    const match = authorization && authorization.match(/^Bearer\s+(.+)$/i);
    if (!match) {
        return null;
    }

    try {
        const payload = jwt.verify(match[1], JWT_SECRET);
        return Number.isSafeInteger(payload.id) && payload.id > 0
            ? payload.id
            : null;
    } catch (error) {
        return null;
    }
}

async function getTrustScore(connection, userId) {
    if (userId === null) {
        return null;
    }
    const [scores] = await connection.execute(
        "SELECT trust_score FROM user_trust_score WHERE user_id = ? LIMIT 1",
        [userId]
    );
    return scores.length === 0 || scores[0].trust_score === null
        ? 100
        : Number(scores[0].trust_score);
}

router.put("/reports/:id/status", async (req, res) => {
    const reportIdString = req.params.id;
    if (!/^[1-9]\d*$/.test(reportIdString)) {
        return res.status(400).json({
            success: false,
            message: "ID báo cáo không hợp lệ."
        });
    }

    const reportId = Number(reportIdString);
    if (!Number.isSafeInteger(reportId)) {
        return res.status(400).json({
            success: false,
            message: "ID báo cáo không hợp lệ."
        });
    }

    const requestedStatus = req.body && req.body.status;
    if (typeof requestedStatus !== "string") {
        return res.status(400).json({
            success: false,
            message: "Trạng thái phải là VERIFIED hoặc REJECTED."
        });
    }

    const newStatus = requestedStatus.trim().toUpperCase();
    if (newStatus !== "VERIFIED" && newStatus !== "REJECTED") {
        return res.status(400).json({
            success: false,
            message: "Trạng thái phải là VERIFIED hoặc REJECTED."
        });
    }

    const userId = getAuthenticatedUserId(req);
    if (!userId) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập bằng tài khoản quản trị."
        });
    }

    let connection;
    let transactionStarted = false;
    try {
        const [admins] = await pool.execute(
            "SELECT role FROM users WHERE id = ? LIMIT 1",
            [userId]
        );
        if (admins.length === 0 || String(admins[0].role).toUpperCase() !== "ADMIN") {
            return res.status(403).json({
                success: false,
                message: "Bạn không có quyền cập nhật trạng thái báo cáo."
            });
        }

        connection = await pool.getConnection();
        await connection.beginTransaction();
        transactionStarted = true;

        const [reports] = await connection.execute(
            `SELECT id, user_id, target_value, target_type, scam_category,
                    description, lost_amount, status, scam_report_id
             FROM scam_report_review_queue
             WHERE id = ?
             FOR UPDATE`,
            [reportId]
        );
        if (reports.length === 0) {
            await connection.rollback();
            transactionStarted = false;
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy báo cáo."
            });
        }

        const report = reports[0];
        const previousStatus = String(report.status || "PENDING").toUpperCase();
        if (previousStatus !== "PENDING" && previousStatus !== newStatus) {
            await connection.rollback();
            transactionStarted = false;
            return res.status(409).json({
                success: false,
                message: "Báo cáo đã được xử lý; không thể đổi trạng thái vì trigger đã đồng bộ dữ liệu."
            });
        }

        let canonicalReportId = report.scam_report_id;
        if (previousStatus === "PENDING" && newStatus === "VERIFIED") {
            if (report.user_id !== null) {
                await connection.execute(
                    `INSERT INTO user_trust_score
                        (user_id, trust_score, total_reports, verified_reports)
                     VALUES (?, 100, 0, 0)
                     ON DUPLICATE KEY UPDATE
                        trust_score = COALESCE(trust_score, 100),
                        total_reports = COALESCE(total_reports, 0),
                        verified_reports = COALESCE(verified_reports, 0)`,
                    [report.user_id]
                );
            }

            const [result] = await connection.execute(
                `INSERT INTO scam_reports
                    (user_id, target_value, target_type, scam_category,
                     description, lost_amount, status)
                 VALUES (?, ?, ?, ?, ?, ?, 'VERIFIED')`,
                [
                    report.user_id,
                    report.target_value,
                    report.target_type,
                    report.scam_category,
                    report.description,
                    report.lost_amount
                ]
            );
            canonicalReportId = result.insertId;

            if (report.user_id !== null) {
                await connection.execute(
                    `UPDATE user_trust_score
                     SET verified_reports = COALESCE(verified_reports, 0) + 1
                     WHERE user_id = ?`,
                    [report.user_id]
                );
            }
        } else if (previousStatus === "PENDING" && newStatus === "REJECTED") {
            if (report.user_id !== null) {
                await connection.execute(
                    `INSERT INTO user_trust_score
                        (user_id, trust_score, total_reports, verified_reports)
                     VALUES (?, ?, 1, 0)
                     ON DUPLICATE KEY UPDATE
                        trust_score = GREATEST(?, COALESCE(trust_score, 100) - ?),
                        total_reports = COALESCE(total_reports, 0) + 1,
                        verified_reports = COALESCE(verified_reports, 0)`,
                    [
                        report.user_id,
                        100 - TRUST_SCORE_PENALTY,
                        MIN_TRUST_SCORE,
                        TRUST_SCORE_PENALTY
                    ]
                );
            }
        }

        if (previousStatus !== newStatus) {
            await connection.execute(
                `UPDATE scam_report_review_queue
                 SET status = ?, scam_report_id = ?
                 WHERE id = ?`,
                [newStatus, canonicalReportId, reportId]
            );
        }

        const trustScore = await getTrustScore(connection, report.user_id);
        await connection.commit();
        transactionStarted = false;

        return res.status(200).json({
            success: true,
            message: "Cập nhật trạng thái báo cáo thành công.",
            data: {
                reportId,
                scamReportId: canonicalReportId,
                previousStatus,
                status: newStatus,
                trustScore
            }
        });
    } catch (error) {
        if (connection && transactionStarted) {
            try {
                await connection.rollback();
            } catch (rollbackError) {
                console.error("Lỗi rollback cập nhật báo cáo:", rollbackError);
            }
        }
        console.error("Lỗi PUT /api/v1/admin/reports/:id/status:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi cập nhật trạng thái báo cáo."
        });
    } finally {
        if (connection) {
            connection.release();
        }
    }
});

module.exports = router;
