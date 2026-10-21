const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const mysql = require('mysql2/promise');
require('dotenv').config();

const app = express();

app.use(express.json());


// ========================================
// 1. CẤU HÌNH
// ========================================

const PORT = process.env.PORT || 3000;

const ACCESS_TOKEN_SECRET = process.env.ACCESS_TOKEN_SECRET;
const REFRESH_TOKEN_SECRET = process.env.REFRESH_TOKEN_SECRET;


// ========================================
// 2. KẾT NỐI MYSQL
// ========================================

const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306,
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});


// Kiểm tra kết nối MySQL
async function testDatabase() {
    try {
        const connection = await db.getConnection();
        console.log('✅ MySQL kết nối thành công!');
        connection.release();
    } catch (error) {
        console.error('❌ Không thể kết nối MySQL:', error.message);
    }
}


// ========================================
// 3. REGISTER
// ========================================

app.post('/api/v1/auth/register', async (req, res) => {
    try {
        const { username, email, password } = req.body;

        // Kiểm tra dữ liệu
        if (!username || !email || !password) {
            return res.status(400).json({
                message: 'Tên người dùng, email và mật khẩu không được để trống'
            });
        }

        // Kiểm tra email đã tồn tại 
        const [existingUsers] = await db.execute(
            'SELECT `ID User` FROM User WHERE Email = ?',
            [email]
        );

        if (existingUsers.length > 0) {
            return res.status(409).json({
                message: 'Email đã được đăng ký'
            });
        }

        // Mã hóa password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Thêm user vào MySQL 
        await db.execute(
            'INSERT INTO User (UserName, Email, Password) VALUES (?, ?, ?)',
            [username, email, hashedPassword]
        );

        res.status(201).json({
            message: 'Đăng ký thành công!'
        });

    } catch (error) {
        console.error('REGISTER ERROR:', error);
        res.status(500).json({
            message: 'Lỗi hệ thống'
        });
    }
});


// ========================================
// 4. LOGIN
// ========================================

app.post('/api/v1/auth/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        // Kiểm tra dữ liệu
        if (!username || !password) {
            return res.status(400).json({
                message: 'Tên đăng nhập và mật khẩu không được để trống'
            });
        }

        // Tìm user trong MySQL 
        const [users] = await db.execute(
            'SELECT `ID User`, UserName, Password FROM User WHERE UserName = ?',
            [username]
        );

        // Không tìm thấy user
        if (users.length === 0) {
            return res.status(401).json({
                message: 'Sai tài khoản hoặc mật khẩu'
            });
        }

        const user = users[0];

        // Kiểm tra password 
        const isMatch = await bcrypt.compare(password, user.Password);

        if (!isMatch) {
            return res.status(401).json({
                message: 'Sai tài khoản hoặc mật khẩu'
            });
        }

        // TẠO ACCESS TOKEN 
        const accessToken = jwt.sign(
            {
                userId: user['ID User'],
                username: user.UserName
            },
            ACCESS_TOKEN_SECRET,
            {
                expiresIn: '15m'
            }
        );

        // TẠO REFRESH TOKEN

        const refreshToken = jwt.sign(
            {
                userId: user['ID User']
            },
            REFRESH_TOKEN_SECRET,
            {
                expiresIn: '30d'
            }
        );


        // LƯU REFRESH TOKEN VÀO MYSQL

        await db.execute(
            'INSERT INTO Token (`ID User`, Token) VALUES (?, ?)',
            [user['ID User'], refreshToken]
        );


        // TRẢ RESPONSE

        res.status(200).json({
            message: 'Đăng nhập thành công!',
            accessToken: accessToken,
            refreshToken: refreshToken
        });

    } catch (error) {
        console.error('LOGIN ERROR:', error);
        res.status(500).json({
            message: 'Lỗi xử lý'
        });
    }
});



// 5. LOGOUT


app.post('/api/v1/auth/logout', async (req, res) => {
    try {
        const { refreshToken } = req.body;

        if (!refreshToken) {
            return res.status(400).json({
                message: 'Không có refresh token'
            });
        }

        // Xóa refresh token khỏi MySQL 
        await db.execute(
            'DELETE FROM Token WHERE Token = ?',
            [refreshToken]
        );

        res.status(200).json({
            message: 'Đăng xuất thành công!'
        });

    } catch (error) {
        console.error('LOGOUT ERROR:', error);
        res.status(500).json({
            message: 'Lỗi hệ thống'
        });
    }
});



// 6. START SERVER


app.listen(PORT, '0.0.0.0', async () => {
    console.log(`🚀 Backend đang chạy tại http://0.0.0.0:${PORT}`);
    await testDatabase();
});
