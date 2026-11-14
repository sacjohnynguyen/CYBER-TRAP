require("dotenv").config();

const express = require("express");
const cors = require("cors");

const { testConnection } = require("./db");
const scanRoutes = require("./routes/scan.routes");
const blacklistRoutes = require("./routes/blacklist.routes");
const rulesRoutes = require("./routes/rules.routes");

const app = express();

app.use(cors());
app.use(express.json());

app.get("/api/health", (req, res) => {
    res.json({
        success: true,
        message: "Cyber Trap Backend is running"
    });
});

app.use("/api/scan", scanRoutes);
app.use("/api/blacklist", blacklistRoutes);
app.use("/api/rules", rulesRoutes);

const PORT = process.env.PORT || 3000;

async function startServer() {
    try {
        await testConnection();

        app.listen(PORT, "0.0.0.0", () => {
            console.log(`🚀 Backend đang chạy tại port ${PORT}`);
        });

    } catch (error) {
        console.error("❌ Không kết nối được MySQL:", error.message);
    }
}

startServer();