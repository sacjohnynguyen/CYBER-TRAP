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
                role VARCHAR(20) NOT NULL DEFAULT 'USER',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        const [userColumns] = await pool.query(
            `SELECT COLUMN_NAME
             FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'users'
               AND COLUMN_NAME = 'role'`
        );
        const existingUserColumns = new Set(userColumns.map((column) => column.COLUMN_NAME));

        if (!existingUserColumns.has("role")) {
            await pool.query(
                "ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'"
            );
        }

        await pool.query(`
            CREATE TABLE IF NOT EXISTS scam_report_review_queue (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NULL,
                target_value VARCHAR(255) NOT NULL,
                target_type VARCHAR(50) NOT NULL DEFAULT 'PHONE',
                scam_category VARCHAR(100) NOT NULL,
                description TEXT NULL,
                lost_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                scam_report_id INT NULL UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_scam_review_user_created (user_id, created_at, id),
                INDEX idx_scam_review_status (status, created_at),
                CONSTRAINT fk_scam_review_user
                    FOREIGN KEY (user_id) REFERENCES users(id)
                    ON DELETE SET NULL
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

        console.log("✅ Các bảng CSDL (users, scam_report_review_queue, scan_requests, blacklist) đã sẵn sàng.");
    } catch (error) {
        console.error("⚠️ Không thể tự động tạo bảng CSDL:", error.message);
    }
}

module.exports = {
    pool,
    testConnection,
    initDb
};
