require("dotenv").config({
    path: require("path").join(__dirname, "root.env")
});

const express = require("express");
const cors = require("cors");

const { testConnection } = require("./db");
const authRoutes = require("./auth.routes");
const scanRoutes = require("./scan.routes");

const app = express();

app.use(cors());
app.use(express.json());
app.use("/api/v1/auth", authRoutes);
app.use("/api/scan", scanRoutes);

app.get("/api/health", (req, res) => {
    res.json({
        success: true,
        message: "Cyber Trap Backend is running"
    });
});


const PORT = process.env.PORT || 3000;

async function startServer() {
    try {
        await testConnection();

        app.listen(PORT, "0.0.0.0", () => {
            console.log(`🚀 Backend đang chạy tại port ${PORT}`);
        });

    } catch (error) {
        console.error("❌ Không kết nối được MySQL:", error.message);
        process.exitCode = 1;
    }
}

startServer();