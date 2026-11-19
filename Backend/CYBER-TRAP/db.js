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
    queueLimit: 0,
    multipleStatements: true
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

// Tự động khởi tạo cấu trúc CSDL đầy đủ
async function initDb() {
    try {
        await pool.query(`SET FOREIGN_KEY_CHECKS = 0;`);

        // 1. Bảng users
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

        // 2. Bảng user_trust_score (Task 2.1 - UserTrustScore)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS user_trust_score (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL UNIQUE,
                trust_score INT DEFAULT 100,
                total_reports INT DEFAULT 0,
                verified_reports INT DEFAULT 0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_trust_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 3. Bảng scam_reports (Task 2.1 - Reports & UI)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS scam_reports (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NULL,
                target_value VARCHAR(255) NOT NULL,
                target_type VARCHAR(50) DEFAULT 'PHONE',
                scam_category VARCHAR(100) NOT NULL,
                description TEXT NULL,
                lost_amount DECIMAL(15,2) DEFAULT 0.00,
                status VARCHAR(50) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_report_user_new FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 4. Bảng report_media (Task 2.1 - ReportMedia & UI)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS report_media (
                id INT AUTO_INCREMENT PRIMARY KEY,
                report_id INT NOT NULL,
                file_path VARCHAR(500) NOT NULL,
                file_size INT NULL,
                file_type VARCHAR(50) DEFAULT 'image/png',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_media_report FOREIGN KEY (report_id) REFERENCES scam_reports (id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 5. Bảng scam_reports_centralized (Task 2.2 - ScamReportsCentralized)
        await pool.query(`
            CREATE TABLE IF NOT EXISTS scam_reports_centralized (
                id INT AUTO_INCREMENT PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                value VARCHAR(255) NOT NULL,
                category VARCHAR(100) NOT NULL,
                total_reports_count INT DEFAULT 1,
                risk_level VARCHAR(50) DEFAULT 'HIGH',
                status VARCHAR(50) DEFAULT 'VERIFIED',
                last_reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY idx_central_type_val (type, value)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 6. Bảng blacklist
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

        // 7. Bảng scan_requests
        await pool.query(`
            CREATE TABLE IF NOT EXISTS scan_requests (
                id INT AUTO_INCREMENT PRIMARY KEY,
                input_type VARCHAR(50) NOT NULL,
                input_value TEXT NOT NULL,
                status VARCHAR(50) DEFAULT 'PROCESSING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        // 8. 5 Bảng Forum Module
        await pool.query(`
            CREATE TABLE IF NOT EXISTS PostCategories (
                CategoryId INT AUTO_INCREMENT PRIMARY KEY,
                CategoryName VARCHAR(100) NOT NULL UNIQUE,
                Description TEXT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        await pool.query(`
            CREATE TABLE IF NOT EXISTS ForumPosts (
                PostId INT AUTO_INCREMENT PRIMARY KEY,
                UserId INT NOT NULL,
                CategoryId INT NOT NULL,
                Title VARCHAR(255) NOT NULL,
                Content LONGTEXT NOT NULL,
                LikeCount INT DEFAULT 0,
                CommentCount INT DEFAULT 0,
                CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                CONSTRAINT fk_forumposts_user FOREIGN KEY (UserId) REFERENCES users (id) ON DELETE CASCADE,
                CONSTRAINT fk_forumposts_category FOREIGN KEY (CategoryId) REFERENCES PostCategories (CategoryId) ON DELETE RESTRICT,
                INDEX idx_posts_created (CreatedAt DESC),
                INDEX idx_posts_category (CategoryId, CreatedAt DESC),
                INDEX idx_posts_user (UserId, CreatedAt DESC),
                INDEX idx_feed_pagination (CategoryId, CreatedAt DESC, PostId DESC),
                INDEX idx_created_postid (CreatedAt DESC, PostId DESC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        await pool.query(`
            CREATE TABLE IF NOT EXISTS PostMedia (
                MediaId INT AUTO_INCREMENT PRIMARY KEY,
                PostId INT NOT NULL,
                MediaUrl VARCHAR(500) NOT NULL,
                SortOrder INT DEFAULT 0,
                CONSTRAINT fk_postmedia_post FOREIGN KEY (PostId) REFERENCES ForumPosts (PostId) ON DELETE CASCADE,
                INDEX idx_media_post_sort (PostId, SortOrder ASC)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        await pool.query(`
            CREATE TABLE IF NOT EXISTS PostComments (
                CommentId INT AUTO_INCREMENT PRIMARY KEY,
                PostId INT NOT NULL,
                UserId INT NOT NULL,
                ParentCommentId INT NULL,
                Content TEXT NOT NULL,
                CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_postcomments_post FOREIGN KEY (PostId) REFERENCES ForumPosts (PostId) ON DELETE CASCADE,
                CONSTRAINT fk_postcomments_user FOREIGN KEY (UserId) REFERENCES users (id) ON DELETE CASCADE,
                CONSTRAINT fk_postcomments_parent FOREIGN KEY (ParentCommentId) REFERENCES PostComments (CommentId) ON DELETE CASCADE,
                INDEX idx_comments_post (PostId, ParentCommentId, CreatedAt ASC),
                INDEX idx_comments_user (UserId)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        await pool.query(`
            CREATE TABLE IF NOT EXISTS PostLikes (
                LikeId INT AUTO_INCREMENT PRIMARY KEY,
                PostId INT NOT NULL,
                UserId INT NOT NULL,
                CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_postlikes_post FOREIGN KEY (PostId) REFERENCES ForumPosts (PostId) ON DELETE CASCADE,
                CONSTRAINT fk_postlikes_user FOREIGN KEY (UserId) REFERENCES users (id) ON DELETE CASCADE,
                UNIQUE KEY uq_post_user_like (PostId, UserId),
                INDEX idx_likes_user_post (UserId, PostId)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        `);

        await pool.query(`SET FOREIGN_KEY_CHECKS = 1;`);

        // Triggers
        await pool.query(`DROP TRIGGER IF EXISTS trg_after_scam_report_insert;`);
        await pool.query(`
            CREATE TRIGGER trg_after_scam_report_insert
            AFTER INSERT ON scam_reports
            FOR EACH ROW
            BEGIN
                IF NEW.user_id IS NOT NULL THEN
                    INSERT INTO user_trust_score (user_id, trust_score, total_reports)
                    VALUES (NEW.user_id, 105, 1)
                    ON DUPLICATE KEY UPDATE
                        trust_score = trust_score + 5,
                        total_reports = total_reports + 1;
                END IF;

                INSERT INTO scam_reports_centralized (type, value, category, total_reports_count, risk_level, status)
                VALUES (NEW.target_type, NEW.target_value, NEW.scam_category, 1, 'HIGH', 'VERIFIED')
                ON DUPLICATE KEY UPDATE
                    total_reports_count = total_reports_count + 1,
                    category = NEW.scam_category,
                    last_reported_at = CURRENT_TIMESTAMP;

                INSERT IGNORE INTO blacklist (type, value, status, reason)
                VALUES (NEW.target_type, NEW.target_value, 'VERIFIED', CONCAT('Cảnh báo từ người dùng: ', NEW.scam_category));
            END;
        `);

        // Stored Procedure News Feed NFR01
        await pool.query(`DROP PROCEDURE IF EXISTS sp_get_newsfeed_posts;`);
        await pool.query(`
            CREATE PROCEDURE sp_get_newsfeed_posts(
                IN p_current_user_id INT,
                IN p_category_id INT,
                IN p_last_created_at TIMESTAMP,
                IN p_last_post_id INT,
                IN p_limit INT
            )
            BEGIN
                DECLARE v_limit INT;
                SET v_limit = IFNULL(p_limit, 10);
                IF v_limit > 50 THEN
                    SET v_limit = 50;
                END IF;

                SELECT
                    p.PostId,
                    p.UserId,
                    u.username AS AuthorName,
                    p.CategoryId,
                    c.CategoryName,
                    p.Title,
                    p.Content,
                    p.LikeCount,
                    p.CommentCount,
                    p.CreatedAt,
                    p.UpdatedAt,
                    IF(p_current_user_id IS NOT NULL AND pl.LikeId IS NOT NULL, 1, 0) AS IsLiked,
                    (
                        SELECT GROUP_CONCAT(pm.MediaUrl ORDER BY pm.SortOrder ASC SEPARATOR ',')
                        FROM PostMedia pm
                        WHERE pm.PostId = p.PostId
                    ) AS MediaUrls
                FROM ForumPosts p
                INNER JOIN users u ON p.UserId = u.id
                INNER JOIN PostCategories c ON p.CategoryId = c.CategoryId
                LEFT JOIN PostLikes pl ON p.PostId = pl.PostId AND pl.UserId = p_current_user_id
                WHERE
                    (p_category_id IS NULL OR p.CategoryId = p_category_id)
                    AND (
                        p_last_created_at IS NULL
                        OR (p.CreatedAt < p_last_created_at)
                        OR (p.CreatedAt = p_last_created_at AND p.PostId < p_last_post_id)
                    )
                ORDER BY p.CreatedAt DESC, p.PostId DESC
                LIMIT v_limit;
            END;
        `);

        console.log("✅ Tất cả các bảng CSDL, Trigger & Stored Procedure (NFR01 Performance < 2s) đã sẵn sàng.");
    } catch (error) {
        console.error("⚠️ Không thể tự động khởi tạo CSDL:", error.message);
    }
}

module.exports = {
    pool,
    testConnection,
    initDb
};
