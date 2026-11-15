const express = require("express");
const router = express.Router();
const jwt = require("jsonwebtoken");
const { pool } = require("./db");

const SUPPORTED_TYPES = [
    "URL",
    "PHONE",
    "EMAIL",
    "BANK_ACCOUNT",
    "CONTENT",
    "TEXT"
];
const JWT_SECRET = process.env.JWT_SECRET || "cyber_trap_jwt_secret_key_2026";
const MAX_LOST_AMOUNT = 9999999999999.99;

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

function getLostAmount(value) {
    if (value === undefined || value === null || value === "") {
        return 0;
    }
    if (typeof value !== "number" && typeof value !== "string") {
        return null;
    }

    const amountText = String(value).trim();
    if (!/^\d+(?:\.\d{1,2})?$/.test(amountText)) {
        return null;
    }

    const amount = Number(amountText);
    return Number.isFinite(amount) && amount <= MAX_LOST_AMOUNT ? amount : null;
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
            `SELECT u.id, COALESCE(uts.trust_score, 100) AS trust_score
             FROM users u
             LEFT JOIN user_trust_score uts ON uts.user_id = u.id
             WHERE u.id = ?
             LIMIT 1`,
            [userId]
        );
        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                message: "Không tìm thấy người dùng."
            });
        }

        const [reports] = await pool.execute(
            `SELECT id, target_value, target_type, scam_category, description,
                    lost_amount, status, scam_report_id, created_at
             FROM scam_report_review_queue
             WHERE user_id = ?
             ORDER BY created_at DESC, id DESC`,
            [userId]
        );

        return res.status(200).json({
            success: true,
            trustScore: Number(users[0].trust_score),
            data: reports.map((report) => ({
                reportId: report.id,
                scamReportId: report.scam_report_id,
                targetValue: report.target_value,
                targetType: report.target_type,
                scamCategory: report.scam_category,
                description: report.description,
                lostAmount: report.lost_amount,
                inputType: report.target_type,
                inputValue: report.target_value,
                reason: report.description,
                status: String(report.status).toUpperCase(),
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
    const body = req.body || {};
    const targetValue = typeof body.targetValue === "string"
        ? body.targetValue.trim()
        : typeof body.inputValue === "string"
            ? body.inputValue.trim()
            : "";
    const targetTypeValue = typeof body.targetType === "string"
        ? body.targetType
        : body.inputType;
    const rawScamCategory = body.scamCategory === undefined
        ? body.category
        : body.scamCategory;
    const scamCategoryValue = rawScamCategory === undefined
        ? "Khác"
        : rawScamCategory;
    const descriptionValue = body.description === undefined
        ? body.reason
        : body.description;

    if (
        !targetValue ||
        typeof targetTypeValue !== "string" ||
        !targetTypeValue.trim()
    ) {
        return res.status(400).json({
            success: false,
            message: "Vui lòng cung cấp targetValue và targetType hợp lệ."
        });
    }

    const targetType = targetTypeValue.trim().toUpperCase();
    if (typeof scamCategoryValue !== "string") {
        return res.status(400).json({
            success: false,
            message: "scamCategory phải là chuỗi."
        });
    }
    if (
        descriptionValue !== undefined &&
        descriptionValue !== null &&
        typeof descriptionValue !== "string"
    ) {
        return res.status(400).json({
            success: false,
            message: "description phải là chuỗi."
        });
    }

    const scamCategory = scamCategoryValue.trim();
    const description = typeof descriptionValue === "string"
        ? descriptionValue.trim()
        : null;
    const lostAmount = getLostAmount(body.lostAmount);

    if (!SUPPORTED_TYPES.includes(targetType)) {
        return res.status(400).json({
            success: false,
            message: "targetType không được hỗ trợ."
        });
    }
    if (
        targetValue.length > 255 ||
        scamCategory.length === 0 ||
        scamCategory.length > 100 ||
        (description !== null && description.length > 1000)
    ) {
        return res.status(400).json({
            success: false,
            message: "Đối tượng tối đa 255 ký tự, hình thức 100 ký tự và mô tả 1.000 ký tự."
        });
    }
    if (lostAmount === null) {
        return res.status(400).json({
            success: false,
            message: "lostAmount phải là số không âm và có tối đa 2 chữ số thập phân."
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
            `INSERT INTO scam_report_review_queue
                (user_id, target_value, target_type, scam_category, description, lost_amount)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [userId, targetValue, targetType, scamCategory, description, lostAmount]
        );

        return res.status(201).json({
            success: true,
            message: "Báo cáo đã được gửi và đang chờ duyệt.",
            data: {
                reportId: result.insertId,
                targetValue,
                targetType,
                scamCategory,
                description,
                lostAmount,
                inputType: targetType,
                inputValue: targetValue,
                reason: description,
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
