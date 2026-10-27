-- =========================================================
-- Cyber Trap - Database Schema (cybertrap_db)
-- =========================================================

CREATE DATABASE IF NOT EXISTS `cybertrap_db`
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `cybertrap_db`;

-- 1. Bảng người dùng (Users / Authentication)
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(100) NOT NULL UNIQUE,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Bảng lưu trữ yêu cầu tra cứu / quét (Scan Requests)
CREATE TABLE IF NOT EXISTS `scan_requests` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `input_type` VARCHAR(50) NOT NULL, -- 'URL', 'PHONE', 'EMAIL', 'CONTENT'
    `input_value` TEXT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'PROCESSING',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bảng danh sách đen đã xác minh (Blacklist)
CREATE TABLE IF NOT EXISTS `blacklist` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `type` VARCHAR(50) NOT NULL,      -- 'URL', 'PHONE', 'EMAIL'
    `value` VARCHAR(255) NOT NULL,
    `status` VARCHAR(50) DEFAULT 'VERIFIED',
    `reason` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_blacklist_type_val` (`type`, `value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Thêm dữ liệu mẫu vào Blacklist để thử nghiệm quét
INSERT IGNORE INTO `blacklist` (`type`, `value`, `status`, `reason`) VALUES
('PHONE', '0987654321', 'VERIFIED', 'Số điện thoại giả danh ngân hàng lừa đảo chuyển tiền'),
('PHONE', '0912345678', 'VERIFIED', 'Mạo danh công an đe dọa tống tiền'),
('EMAIL', 'support-fake-bank@gmail.com', 'VERIFIED', 'Email giả mạo hỗ trợ khách hàng chiếm đoạt OTP'),
('URL', 'https://nhan-qua-free.com', 'VERIFIED', 'Website giả mạo trúng thưởng đánh cắp tài khoản'),
('URL', 'http://dangnhap-taikhoan-nganhang.com', 'VERIFIED', 'Trang web phishing giao diện ngân hàng');
