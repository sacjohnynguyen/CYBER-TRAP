const path = require("path");
const fs = require("fs");
const dotenv = require("dotenv");

// Nạp file cấu hình môi trường (.env hoặc root.env)
const envPath = fs.existsSync(path.join(__dirname, ".env"))
    ? path.join(__dirname, ".env")
    : path.join(__dirname, "root.env");
dotenv.config({ path: envPath });

const mysql = require("mysql2/promise");

const pool = mysql.createPool({
    host: process.env.DB_HOST || "localhost",
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || "root",
    password: process.env.DB_PASSWORD || "",
    database: process.env.DB_NAME || "cybertrap_db",
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

async function testConnection() {
    const connection = await pool.getConnection();
    try {
        await connection.ping();
        await connection.query("SELECT 1");
        console.log(`✅ Kết nối MySQL thành công: Database '${process.env.DB_NAME}'`);
    } finally {
        connection.release();
    }
}

// Tự động khởi tạo bảng nếu chưa có
async function initDb() {
    try {
        // 1. Bảng users: kiểm tra xem cột 'id' có tồn tại không
        try {
            await pool.query(`SELECT id FROM users LIMIT 1`);
        } catch (colErr) {
            console.log("⚠️ Bảng users bị thiếu cột 'id' hoặc lỗi cấu trúc, tiến hành xóa để tạo lại bảng chuẩn...");
            await pool.query(`DROP TABLE IF EXISTS users`);
        }

        await pool.query(`
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(100) NOT NULL UNIQUE,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 2. Bảng scan_requests
        await pool.query(`
            CREATE TABLE IF NOT EXISTS scan_requests (
                id INT AUTO_INCREMENT PRIMARY KEY,
                input_type VARCHAR(50) NOT NULL,
                input_value TEXT NOT NULL,
                status VARCHAR(50) DEFAULT 'PROCESSING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 3. Bảng blacklist
        await pool.query(`
            CREATE TABLE IF NOT EXISTS blacklist (
                id INT AUTO_INCREMENT PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                value VARCHAR(255) NOT NULL,
                status VARCHAR(50) DEFAULT 'VERIFIED',
                reason TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_blacklist_type_val (type, value)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        console.log("✅ Các bảng CSDL (users, scan_requests, blacklist) đã sẵn sàng.");
    } catch (error) {
        console.error("⚠️ Không thể tự động tạo bảng CSDL:", error.message);
    }
}

module.exports = {
    pool,
    testConnection,
    initDb
};
