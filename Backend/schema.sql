-- =========================================================
-- Cyber Trap - Full Database Schema (cybertrap_db)
-- Includes: Authentication, Scan, Scam Reports, Blacklist, Forum & Newsfeed Optimization (NFR01)
-- =========================================================

CREATE DATABASE IF NOT EXISTS `cybertrap_db`
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `cybertrap_db`;

-- ---------------------------------------------------------
-- 1. Bảng người dùng (Users / Authentication - Sprint 1)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 2. Bảng điểm uy tín đóng góp tài khoản (UserTrustScore)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_trust_score` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL UNIQUE,
    `trust_score` INT DEFAULT 100,
    `total_reports` INT DEFAULT 0,
    `verified_reports` INT DEFAULT 0,
    `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_trust_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 3. Bảng chứa bài báo cáo lừa đảo từ UI (scam_reports)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `scam_reports` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NULL,
    `target_value` VARCHAR(255) NOT NULL,
    `target_type` VARCHAR(50) DEFAULT 'PHONE',
    `scam_category` VARCHAR(100) NOT NULL,
    `description` TEXT NULL,
    `lost_amount` DECIMAL(15,2) DEFAULT 0.00,
    `status` VARCHAR(50) DEFAULT 'PENDING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_report_user_new` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 4. Bảng chứa tệp ảnh bằng chứng đính kèm (report_media)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `report_media` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `report_id` INT NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `file_size` INT NULL,
    `file_type` VARCHAR(50) DEFAULT 'image/png',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_media_report` FOREIGN KEY (`report_id`) REFERENCES `scam_reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 5. Bảng tập trung dữ liệu lừa đảo tra cứu nhanh (scam_reports_centralized)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `scam_reports_centralized` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `type` VARCHAR(50) NOT NULL,
    `value` VARCHAR(255) NOT NULL,
    `category` VARCHAR(100) NOT NULL,
    `total_reports_count` INT DEFAULT 1,
    `risk_level` VARCHAR(50) DEFAULT 'HIGH',
    `status` VARCHAR(50) DEFAULT 'VERIFIED',
    `last_reported_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `idx_central_type_val` (`type`, `value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 6. Bảng danh sách đen đã xác minh (blacklist)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `blacklist` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `type` VARCHAR(50) NOT NULL,
    `value` VARCHAR(255) NOT NULL,
    `status` VARCHAR(50) DEFAULT 'VERIFIED',
    `reason` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_blacklist_type_val` (`type`, `value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 7. Bảng lưu trữ yêu cầu tra cứu / quét (scan_requests)
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS `scan_requests` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `input_type` VARCHAR(50) NOT NULL,
    `input_value` TEXT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'PROCESSING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================
-- FORUM MODULE (5 BẢNG DIỄN ĐÀN CỘNG ĐỒNG)
-- =========================================================

-- 8. Bảng 1 Forum: PostCategories (Danh mục hình thức lừa đảo)
CREATE TABLE IF NOT EXISTS `PostCategories` (
    `CategoryId` INT AUTO_INCREMENT PRIMARY KEY,
    `CategoryName` VARCHAR(100) NOT NULL UNIQUE,
    `Description` TEXT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Bảng 2 Forum: ForumPosts (Lưu bài viết diễn đàn)
CREATE TABLE IF NOT EXISTS `ForumPosts` (
    `PostId` INT AUTO_INCREMENT PRIMARY KEY,
    `UserId` INT NOT NULL,
    `CategoryId` INT NOT NULL,
    `Title` VARCHAR(255) NOT NULL,
    `Content` LONGTEXT NOT NULL,
    `LikeCount` INT DEFAULT 0,
    `CommentCount` INT DEFAULT 0,
    `CreatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `UpdatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_forumposts_user` FOREIGN KEY (`UserId`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_forumposts_category` FOREIGN KEY (`CategoryId`) REFERENCES `PostCategories` (`CategoryId`) ON DELETE RESTRICT,
    INDEX `idx_posts_created` (`CreatedAt` DESC),
    INDEX `idx_posts_category` (`CategoryId`, `CreatedAt` DESC),
    INDEX `idx_posts_user` (`UserId`, `CreatedAt` DESC),
    INDEX `idx_feed_pagination` (`CategoryId`, `CreatedAt` DESC, `PostId` DESC),
    INDEX `idx_created_postid` (`CreatedAt` DESC, `PostId` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Bảng 3 Forum: PostMedia (Hình ảnh đính kèm bài viết)
CREATE TABLE IF NOT EXISTS `PostMedia` (
    `MediaId` INT AUTO_INCREMENT PRIMARY KEY,
    `PostId` INT NOT NULL,
    `MediaUrl` VARCHAR(500) NOT NULL,
    `SortOrder` INT DEFAULT 0,
    CONSTRAINT `fk_postmedia_post` FOREIGN KEY (`PostId`) REFERENCES `ForumPosts` (`PostId`) ON DELETE CASCADE,
    INDEX `idx_media_post_sort` (`PostId`, `SortOrder` ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Bảng 4 Forum: PostComments (Bình luận & Trả lời bình luận 2 cấp)
CREATE TABLE IF NOT EXISTS `PostComments` (
    `CommentId` INT AUTO_INCREMENT PRIMARY KEY,
    `PostId` INT NOT NULL,
    `UserId` INT NOT NULL,
    `ParentCommentId` INT NULL,
    `Content` TEXT NOT NULL,
    `CreatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_postcomments_post` FOREIGN KEY (`PostId`) REFERENCES `ForumPosts` (`PostId`) ON DELETE CASCADE,
    CONSTRAINT `fk_postcomments_user` FOREIGN KEY (`UserId`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_postcomments_parent` FOREIGN KEY (`ParentCommentId`) REFERENCES `PostComments` (`CommentId`) ON DELETE CASCADE,
    INDEX `idx_comments_post` (`PostId`, `ParentCommentId`, `CreatedAt` ASC),
    INDEX `idx_comments_user` (`UserId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bảng 5 Forum: PostLikes (Trạng thái Like bài viết)
CREATE TABLE IF NOT EXISTS `PostLikes` (
    `LikeId` INT AUTO_INCREMENT PRIMARY KEY,
    `PostId` INT NOT NULL,
    `UserId` INT NOT NULL,
    `CreatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_postlikes_post` FOREIGN KEY (`PostId`) REFERENCES `ForumPosts` (`PostId`) ON DELETE CASCADE,
    CONSTRAINT `fk_postlikes_user` FOREIGN KEY (`UserId`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uq_post_user_like` (`PostId`, `UserId`),
    INDEX `idx_likes_user_post` (`UserId`, `PostId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================
-- DỮ LIỆU MẪU (Sample Data)
-- =========================================================
INSERT IGNORE INTO `blacklist` (`type`, `value`, `status`, `reason`) VALUES
('PHONE', '0987654321', 'VERIFIED', 'Số điện thoại giả danh ngân hàng lừa đảo chuyển tiền'),
('PHONE', '0912345678', 'VERIFIED', 'Mạo danh công an đe dọa tống tiền'),
('EMAIL', 'support-fake-bank@gmail.com', 'VERIFIED', 'Email giả mạo hỗ trợ khách hàng chiếm đoạt OTP'),
('URL', 'https://nhan-qua-free.com', 'VERIFIED', 'Website giả mạo trúng thưởng đánh cắp tài khoản'),
('URL', 'http://dangnhap-taikhoan-nganhang.com', 'VERIFIED', 'Trang web phishing giao diện ngân hàng');

INSERT IGNORE INTO `PostCategories` (`CategoryName`, `Description`) VALUES
('Giả mạo ngân hàng', 'Cảnh báo và chia sẻ kinh nghiệm về các chiêu thức lừa đảo giả danh ngân hàng, gửi SMS Brandname giả, chiếm đoạt OTP.'),
('Việc nhẹ lương cao', 'Cảnh báo các bẫy tuyển dụng làm nhiệm vụ online, nạp tiền nhận hoa hoa hồng, làm cộng tác viên Shopee/Tiki giả.'),
('Chiếm đoạt Facebook/Zalo', 'Cảnh báo các thủ đoạn hack tài khoản, mạo danh người thân nhắn tin vay tiền, bình chọn trúng thưởng.'),
('Lừa đảo đầu tư', 'Cảnh báo các sàn giao dịch chứng khoán, tiền điện tử (crypto), ứng dụng đa cấp biến tướng cam kết lãi suất khủng.'),
('Lừa đảo mua bán trực tuyến', 'Cảnh báo tình trạng nhận cọc mua hàng rồi bùng, bán hàng nhái, gửi hàng tráo đổi gian lận.');

-- =========================================================
-- TRIGGERS & PROCEDURES (Tự động đồng bộ đếm & Validation)
-- =========================================================

DELIMITER $$

-- Trigger 1: Validation bình luận con thuộc cùng bài viết với bình luận cha
DROP TRIGGER IF EXISTS `trg_before_postcomment_insert`$$
CREATE TRIGGER `trg_before_postcomment_insert`
BEFORE INSERT ON `PostComments`
FOR EACH ROW
BEGIN
    DECLARE parent_post_id INT DEFAULT NULL;
    IF NEW.ParentCommentId IS NOT NULL THEN
        SELECT `PostId` INTO parent_post_id FROM `PostComments` WHERE `CommentId` = NEW.ParentCommentId LIMIT 1;
        IF parent_post_id IS NULL OR parent_post_id != NEW.PostId THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Lỗi: Bình luận con phải thuộc cùng bài viết với bình luận cha!';
        END IF;
    END IF;
END$$

-- Trigger 2 & 3: Tự động cập nhật LikeCount
DROP TRIGGER IF EXISTS `trg_after_postlike_insert`$$
CREATE TRIGGER `trg_after_postlike_insert`
AFTER INSERT ON `PostLikes`
FOR EACH ROW
BEGIN
    UPDATE `ForumPosts` SET `LikeCount` = `LikeCount` + 1 WHERE `PostId` = NEW.PostId;
END$$

DROP TRIGGER IF EXISTS `trg_after_postlike_delete`$$
CREATE TRIGGER `trg_after_postlike_delete`
AFTER DELETE ON `PostLikes`
FOR EACH ROW
BEGIN
    UPDATE `ForumPosts` SET `LikeCount` = GREATEST(0, `LikeCount` - 1) WHERE `PostId` = OLD.PostId;
END$$

-- Trigger 4 & 5: Tự động cập nhật CommentCount
DROP TRIGGER IF EXISTS `trg_after_postcomment_insert`$$
CREATE TRIGGER `trg_after_postcomment_insert`
AFTER INSERT ON `PostComments`
FOR EACH ROW
BEGIN
    UPDATE `ForumPosts` SET `CommentCount` = `CommentCount` + 1 WHERE `PostId` = NEW.PostId;
END$$

DROP TRIGGER IF EXISTS `trg_after_postcomment_delete`$$
CREATE TRIGGER `trg_after_postcomment_delete`
AFTER DELETE ON `PostComments`
FOR EACH ROW
BEGIN
    UPDATE `ForumPosts` SET `CommentCount` = GREATEST(0, `CommentCount` - 1) WHERE `PostId` = OLD.PostId;
END$$

-- Stored Procedure: Keyset Cursor-Based Pagination tối ưu News Feed NFR01 (< 2 giây)
DROP PROCEDURE IF EXISTS `sp_get_newsfeed_posts`$$
CREATE PROCEDURE `sp_get_newsfeed_posts`(
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
END$$

DELIMITER ;
