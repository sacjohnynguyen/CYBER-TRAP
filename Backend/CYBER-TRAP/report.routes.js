const express = require("express");
const router = express.Router();
const jwt = require("jsonwebtoken");
const { pool } = require("./db");

const SUPPORTED_TYPES = ["URL", "PHONE", "EMAIL", "CONTENT", "TEXT"];
const JWT_SECRET = process.env.JWT_SECRET || "cyber_trap_jwt_secret_key_2026";

function getAuthenticatedUserId(req) {
    const authorization = req.get("authorization");
    if (!authorization) {
        return null;
    }

    const match = authorization.match(/^Bearer\s+(.+)$/i);
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

router.get("/user/:id", async (req, res) => {
    const userId = Number(req.params.id);
    if (!Number.isSafeInteger(userId) || userId <= 0) {
        return res.status(400).json({
            success: false,
            message: "ID người dùng không hợp lệ."
        });
    }

    const authenticatedUserId = getAuthenticatedUserId(req);
    if (!authenticatedUserId) {
        return res.status(401).json({
            success: false,
            message: "Vui lòng đăng nhập để xem báo cáo."
        });
    }

    if (authenticatedUserId !== userId) {
        return res.status(403).json({
            success: false,
            message: "Bạn không có quyền xem báo cáo của người dùng này."
        });
    }

    try {
        const [users] = await pool.execute(
            "SELECT id FROM users WHERE id = ? LIMIT 1",
            [userId]
        );
        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy người dùng."
            });
        }

        const [reports] = await pool.execute(
            `SELECT id, input_type, input_value, reason, status, created_at
             FROM reports
             WHERE user_id = ?
             ORDER BY created_at DESC, id DESC`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            data: reports.map((report) => ({
                reportId: report.id,
                inputType: report.input_type,
                inputValue: report.input_value,
                reason: report.reason,
                status: report.status,
                createdAt: report.created_at
            }))
        });
    } catch (error) {
        console.error("Lỗi GET /api/v1/reports/user/:id:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi lấy danh sách báo cáo."
        });
    }
});

router.post("/", async (req, res) => {
    const { inputType, inputValue, reason } = req.body || {};

    if (
        typeof inputType !== "string" ||
        typeof inputValue !== "string" ||
        typeof reason !== "string" ||
        !inputType.trim() ||
        !inputValue.trim() ||
        !reason.trim()
    ) {
        return res.status(400).json({
            success: false,
            message: "Vui lòng cung cấp inputType, inputValue và reason hợp lệ."
        });
    }

    const type = inputType.trim().toUpperCase();
    const value = inputValue.trim();
    const reportReason = reason.trim();

    if (!SUPPORTED_TYPES.includes(type)) {
        return res.status(400).json({
            success: false,
            message: "Loại dữ liệu không được hỗ trợ. Hỗ trợ: URL, PHONE, EMAIL, CONTENT, TEXT."
        });
    }

    if (value.length > 5000 || reportReason.length > 2000) {
        return res.status(400).json({
            success: false,
            message: "inputValue tối đa 5000 ký tự và reason tối đa 2000 ký tự."
        });
    }

    const authorization = req.get("authorization");
    const userId = getAuthenticatedUserId(req);
    if (authorization && !userId) {
        return res.status(401).json({
            success: false,
            message: "Access token không hợp lệ."
        });
    }

    try {
        if (userId) {
            const [users] = await pool.execute(
                "SELECT id FROM users WHERE id = ? LIMIT 1",
                [userId]
            );
            if (users.length === 0) {
                return res.status(401).json({
                    success: false,
                    message: "Tài khoản trong access token không còn tồn tại."
                });
            }
        }

        const [result] = await pool.execute(
            `INSERT INTO reports (user_id, input_type, input_value, reason)
             VALUES (?, ?, ?, ?)`,
            [userId, type, value, reportReason]
        );

        return res.status(201).json({
            success: true,
            message: "Gửi báo cáo thành công.",
            data: {
                reportId: result.insertId,
                inputType: type,
                inputValue: value,
                reason: reportReason,
                status: "PENDING"
            }
        });
    } catch (error) {
        console.error("Lỗi POST /api/v1/reports:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ khi lưu báo cáo."
        });
    }
});

module.exports = router;
