const express = require('express');
const sequelize = require('./config/db');
const DangerKeyword = require('./models/DangerKeyword'); // Bảng chứa từ khóa nguy hiểm

const app = express();
app.use(express.json());

// =========================================================================
// ENDPOINT CHUYÊN BIỆT: KIỂM TRA NỘI DUNG VĂN BẢN (SMS/ZALO)
// =========================================================================
app.post('/api/v1/check/text', async (req, res) => {
    try {
        const { text } = req.body; // App Android gửi chuỗi văn bản tin nhắn lên qua biến 'text'
        
        if (!text || text.trim() === '') {
            return res.status(400).json({ error: "Nội dung tin nhắn không được để trống" });
        }

        // 1. Chuẩn hóa chữ: Đưa tất cả văn bản về chữ thường để quét chính xác
        const cleanText = text.trim().toLowerCase();

        // 2. Truy vấn lấy danh mục từ khóa lừa đảo từ bảng danger_keywords trong MySQL
        const dangerKeywords = await DangerKeyword.findAll();
        
        let totalRiskScore = 0;      // Tổng điểm nguy hiểm của tin nhắn
        let detectedKeywordsList = []; // Danh sách các từ khóa lừa đảo bị phát hiện

        // 3. Duyệt qua từng từ khóa nguy hiểm trong cơ sở dữ liệu để chấm điểm cộng dồn
        dangerKeywords.forEach(item => {
            // Nếu tin nhắn có chứa từ khóa lừa đảo (Ví dụ: "cung cap otp")
            if (cleanText.includes(item.text.toLowerCase())) {
                totalRiskScore += item.weight;      // Cộng dồn điểm nguy hiểm (Ví dụ: +5 điểm)
                detectedKeywordsList.push(item.text); // Lưu lại từ khóa đó để báo cáo
            }
        });

        // 4. Định nghĩa các mức độ rủi ro dựa trên tổng số điểm nguy hiểm tích lũy
        let isScam = false;
        let riskLevel = "NONE";
        let responseMessage = "Tin nhắn tạm thời an toàn. Chưa phát hiện dấu hiệu lừa đảo từ nội dung văn bản này.";
        let scanReason = "Văn bản bình thường";

        if (totalRiskScore >= 5) {
            isScam = true;
            riskLevel = "HIGH";
            responseMessage = "Cảnh báo khẩn cấp: Hệ thống phát hiện tin nhắn có hành vi lừa đảo nghiêm trọng (Yêu cầu OTP/Mật khẩu hoặc mạo danh cơ quan pháp luật). Tuyệt đối không làm theo yêu cầu trong tin nhắn!";
            scanReason = "Phát hiện kịch bản lừa đảo tài khoản mức độ nguy hiểm cao";
        } else if (totalRiskScore >= 3) {
            isScam = true;
            riskLevel = "MEDIUM";
            responseMessage = "Chú ý: Nội dung tin nhắn có dấu hiệu nghi ngờ lừa đảo trúng thưởng, nhận quà miễn phí hoặc thao túng tâm lý. Vui lòng xác thực kỹ lại thông tin.";
            scanReason = "Nội dung mang dấu hiệu dụ dỗ, lừa đảo trúng thưởng";
        }

        // 5. Trả kết quả đồng nhất về cho ứng dụng (Qua Retrofit đón dữ liệu)
        return res.status(200).json({
            isScam: isScam,
            riskLevel: riskLevel,
            message: responseMessage,
            details: {
                reason: scanReason,
                riskScore: totalRiskScore,              // Trả về tổng điểm để hiển thị thanh đo rủi ro trên App
                detectedKeywords: detectedKeywordsList // Trả về danh sách từ bị phát hiện để App bôi đỏ/cảnh báo
            }
        });

    } catch (error) {
        console.error("Lỗi hệ thống khi quét tin nhắn SMS/Zalo:", error);
        return res.status(500).json({ error: "Lỗi hệ thống Backend không thể phân tích văn bản lúc này" });
    }
});

app.listen(3000, () => console.log('🚀 API chuyên quét tin nhắn SMS/Zalo đang chạy tại port 3000'));
