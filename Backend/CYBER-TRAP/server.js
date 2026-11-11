const path = require("path");
const fs = require("fs");
const dotenv = require("dotenv");

// Nạp file cấu hình môi trường (.env hoặc root.env)
const envPath = fs.existsSync(path.join(__dirname, ".env"))
    ? path.join(__dirname, ".env")
    : path.join(__dirname, "root.env");
dotenv.config({ path: envPath });

const express = require("express");
const cors = require("cors");

const { testConnection, initDb } = require("./db");
const scanRoutes = require("./scan.routes");
const authRoutes = require("./auth.routes");

const app = express();

app.use(cors());
app.use(express.json());

// Routes hỗ trợ cả tiền tố /api/v1 và /api (tránh lệch route giữa Frontend và Backend)
app.use("/api/v1/auth", authRoutes);
app.use("/api/auth", authRoutes);

app.use("/api/v1/scan", scanRoutes);
app.use("/api/scan", scanRoutes);

app.get(["/api/health", "/api/v1/health"], (req, res) => {
    res.json({
        success: true,
        message: "Cyber Trap Backend is running",
        timestamp: new Date().toISOString()
    });
});

const PORT = process.env.PORT || 3000;

async function startServer() {
    try {
        await testConnection();
        await initDb();
    } catch (error) {
        console.error("⚠️ Không thể kết nối hoặc khởi tạo MySQL:", error.message);
        console.log("👉 Vui lòng kiểm tra lại cấu hình DB trong file .env hoặc root.env.");
    }

    app.listen(PORT, "0.0.0.0", () => {
        console.log(`===============================================`);
        console.log(`🚀 Cyber Trap Backend đang chạy tại port ${PORT}`);
        console.log(`📡 Local URL: http://localhost:${PORT}`);
        console.log(`📱 Android Emulator URL: http://10.0.2.2:${PORT}`);
        console.log(`🔑 Auth Endpoints:`);
        console.log(`   - POST http://localhost:${PORT}/api/v1/auth/register`);
        console.log(`   - POST http://localhost:${PORT}/api/v1/auth/login`);
        console.log(`🔍 Scan Endpoint:`);
        console.log(`   - POST http://localhost:${PORT}/api/v1/scan`);
        console.log(`===============================================`);
    });
}

startServer();
