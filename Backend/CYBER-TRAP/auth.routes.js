const express = require("express");
const router = express.Router();
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const { pool } = require("./db");

const JWT_SECRET = process.env.JWT_SECRET || "cyber_trap_jwt_secret_key_2026";

// =========================================================================
// 1. ĐĂNG KÝ TÀI KHOẢN (POST /api/v1/auth/register hoặc /api/auth/register)
// =========================================================================
router.post("/register", async (req, res) => {
    try {
        const { username, email, password } = req.body || {};

        // Kiểm tra dữ liệu đầu vào
        if (!username || !email || !password) {
            return res.status(400).json({
                success: false,
                message: "Vui lòng nhập đầy đủ tên đăng nhập, email và mật khẩu!"
            });
        }

        const cleanUsername = String(username).trim();
        const cleanEmail = String(email).trim().toLowerCase();
        const cleanPassword = String(password).trim();

        if (cleanUsername.length < 3) {
            return res.status(400).json({
                success: false,
                message: "Tên đăng nhập phải có ít nhất 3 ký tự!"
            });
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(cleanEmail)) {
            return res.status(400).json({
                success: false,
                message: "Định dạng email không hợp lệ!"
            });
        }

        if (cleanPassword.length < 6) {
            return res.status(400).json({
                success: false,
                message: "Mật khẩu phải có ít nhất 6 ký tự!"
            });
        }

        // Kiểm tra xem tên đăng nhập hoặc email đã tồn tại chưa
        const [existing] = await pool.execute(
            `SELECT id, username, email FROM users WHERE username = ? OR email = ? LIMIT 1`,
            [cleanUsername, cleanEmail]
        );

        if (existing.length > 0) {
            const isUsernameDup = existing[0].username.toLowerCase() === cleanUsername.toLowerCase();
            return res.status(409).json({
                success: false,
                message: isUsernameDup
                    ? "Tên đăng nhập này đã được sử dụng!"
                    : "Email này đã được đăng ký!"
            });
        }

        // Băm mật khẩu an toàn với bcrypt
        const salt = await bcrypt.genSalt(10);
        const passwordHash = await bcrypt.hash(cleanPassword, salt);

        // Lưu vào MySQL
        const [result] = await pool.execute(
            `INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)`,
            [cleanUsername, cleanEmail, passwordHash]
        );

        return res.status(201).json({
            success: true,
            message: "Đăng ký tài khoản thành công!",
            data: {
                id: result.insertId,
                username: cleanUsername,
                email: cleanEmail
            }
        });

    } catch (error) {
        console.error("Lỗi khi đăng ký:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ trong quá trình đăng ký: " + error.message
        });
    }
});

// =========================================================================
// 2. ĐĂNG NHẬP (POST /api/v1/auth/login hoặc /api/auth/login)
// =========================================================================
router.post("/login", async (req, res) => {
    try {
        const { username, password } = req.body || {};

        if (!username || !password) {
            return res.status(400).json({
                success: false,
                message: "Vui lòng nhập tên đăng nhập và mật khẩu!"
            });
        }

        const inputIdentifier = String(username).trim();
        const inputPassword = String(password).trim();

        // Tìm kiếm theo username hoặc email
        const [users] = await pool.execute(
            `SELECT id, username, email, password_hash FROM users WHERE username = ? OR email = ? LIMIT 1`,
            [inputIdentifier, inputIdentifier]
        );

        if (users.length === 0) {
            return res.status(401).json({
                success: false,
                message: "Tài khoản hoặc mật khẩu không chính xác!"
            });
        }

        const user = users[0];

        // So khớp mật khẩu với hash đã lưu
        const isMatch = await bcrypt.compare(inputPassword, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({
                success: false,
                message: "Tài khoản hoặc mật khẩu không chính xác!"
            });
        }

        // Tạo JWT Token
        const accessToken = jwt.sign(
            { id: user.id, username: user.username, email: user.email },
            JWT_SECRET,
            { expiresIn: "7d" }
        );

        const refreshToken = jwt.sign(
            { id: user.id },
            JWT_SECRET,
            { expiresIn: "30d" }
        );

        return res.status(200).json({
            success: true,
            message: "Đăng nhập thành công!",
            accessToken,
            refreshToken,
            user: {
                id: user.id,
                username: user.username,
                email: user.email
            }
        });

    } catch (error) {
        console.error("Lỗi khi đăng nhập:", error);
        return res.status(500).json({
            success: false,
            message: "Lỗi máy chủ trong quá trình đăng nhập: " + error.message
        });
    }
});

module.exports = router;
