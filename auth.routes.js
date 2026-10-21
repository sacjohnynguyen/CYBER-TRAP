const crypto = require("node:crypto");
const { promisify } = require("node:util");
const express = require("express");
const { pool } = require("./db");

const router = express.Router();
const scrypt = promisify(crypto.scrypt);
const PASSWORD_BYTES = 64;
const MIN_PASSWORD_LENGTH = 6;
const MAX_PASSWORD_LENGTH = 128;

async function hashPassword(password) {
    const salt = crypto.randomBytes(16).toString("hex");
    const hash = await scrypt(password, salt, PASSWORD_BYTES);
    return `scrypt$${salt}$${hash.toString("hex")}`;
}

async function verifyPassword(password, storedHash) {
    const [algorithm, salt, expectedHex] = storedHash.split("$");

    if (algorithm !== "scrypt" || !salt || !expectedHex || !/^[a-f0-9]{128}$/i.test(expectedHex)) {
        return false;
    }

    const expected = Buffer.from(expectedHex, "hex");
    const actual = await scrypt(password, salt, PASSWORD_BYTES);

    return crypto.timingSafeEqual(expected, actual);
}

router.post("/register", async (req, res) => {
    const { username, email, password } = req.body || {};

    if (
        typeof username !== "string" ||
        typeof email !== "string" ||
        typeof password !== "string"
    ) {
        return res.status(400).json({ message: "Vui lòng nhập tên, email và mật khẩu hợp lệ." });
    }

    const fullName = username.trim();
    const normalizedEmail = email.trim().toLowerCase();

    if (!fullName || fullName.length > 100) {
        return res.status(400).json({ message: "Tên người dùng không hợp lệ." });
    }

    if (normalizedEmail.length > 150 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
        return res.status(400).json({ message: "Email không hợp lệ." });
    }

    if (password.length < MIN_PASSWORD_LENGTH || password.length > MAX_PASSWORD_LENGTH) {
        return res.status(400).json({
            message: `Mật khẩu phải có từ ${MIN_PASSWORD_LENGTH} đến ${MAX_PASSWORD_LENGTH} ký tự.`
        });
    }

    try {
        const passwordHash = await hashPassword(password);
        await pool.execute(
            `INSERT INTO users (full_name, email, password_hash)
             VALUES (?, ?, ?)`,
            [fullName, normalizedEmail, passwordHash]
        );

        return res.status(201).json({ message: "Đăng ký thành công." });
    } catch (error) {
        if (error.code === "ER_DUP_ENTRY") {
            return res.status(409).json({ message: "Email này đã được đăng ký." });
        }

        console.error("Register route failed:", error.code || "unknown error");
        return res.status(500).json({ message: "Không thể đăng ký lúc này. Vui lòng thử lại." });
    }
});

router.post("/login", async (req, res) => {
    const { username, password } = req.body || {};

    if (typeof username !== "string" || typeof password !== "string" || !username.trim() || !password) {
        return res.status(400).json({ message: "Vui lòng nhập tên đăng nhập và mật khẩu." });
    }

    try {
        const identifier = username.trim();
        const [emailMatches] = await pool.execute(
            `SELECT user_id, full_name, email, password_hash
             FROM users
             WHERE email = ?
             LIMIT 1`,
            [identifier.toLowerCase()]
        );

        let user = emailMatches[0];
        if (!user) {
            const [nameMatches] = await pool.execute(
                `SELECT user_id, full_name, email, password_hash
                 FROM users
                 WHERE full_name = ?
                 LIMIT 2`,
                [identifier]
            );

            if (nameMatches.length > 1) {
                return res.status(400).json({
                    message: "Tên đăng nhập bị trùng. Vui lòng đăng nhập bằng email."
                });
            }
            user = nameMatches[0];
        }

        if (!user || !(await verifyPassword(password, user.password_hash))) {
            return res.status(401).json({ message: "Sai tên đăng nhập/email hoặc mật khẩu." });
        }

        return res.json({
            message: "Đăng nhập thành công.",
            user: {
                userId: user.user_id,
                username: user.full_name,
                email: user.email
            }
        });
    } catch (error) {
        console.error("Login route failed:", error.code || "unknown error");
        return res.status(500).json({ message: "Không thể đăng nhập lúc này. Vui lòng thử lại." });
    }
});

module.exports = router;
