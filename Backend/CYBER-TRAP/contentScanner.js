/**
 * Quét nội dung văn bản (Tin nhắn SMS / Zalo / Telegram / Tin đồn lừa đảo)
 */
function scanContent(inputText) {
    if (!inputText || typeof inputText !== "string" || !inputText.trim()) {
        return {
            valid: false,
            normalizedContent: "",
            risk_score: 0,
            riskLevel: "UNKNOWN",
            is_scam: false,
            isSuspicious: false,
            reasons: ["Nội dung văn bản không được để trống"],
            detection_reasons: ["Nội dung văn bản không được để trống"],
            matchedKeywords: [],
            recommendation: "Không làm theo hướng dẫn hoặc cung cấp OTP/mật khẩu trong tin nhắn lạ."
        };
    }

    const cleanText = inputText.trim().toLowerCase();
    const matchedKeywords = [];
    const reasons = [];

    // Danh sách từ khóa lừa đảo phổ biến (cả tiếng Việt có dấu và không dấu)
    const scamRules = [
        { keywords: ["otp", "ma otp", "mã otp", "ma xac thuc", "mã xác thực"], weight: 30, reason: "Nội dung yêu cầu hoặc nhắc tới mã xác thực OTP bảo mật." },
        { keywords: ["mat khau", "mật khẩu", "cap mat khau", "cung cap mat khau"], weight: 30, reason: "Nội dung yêu cầu hoặc nhắc tới mật khẩu tài khoản." },
        { keywords: ["bi khoa", "bị khóa", "khoa tai khoan", "khóa tài khoản", "sap bi khoa"], weight: 25, reason: "Đe dọa tài khoản ngân hàng / mạng xã hội sắp bị khóa khẩn cấp." },
        { keywords: ["cong an", "công an", "toa an", "tòa án", "vien kiem sat", "viện kiểm sát", "lenh bat"], weight: 35, reason: "Mạo danh cơ quan công an, tòa án, đe dọa lệnh bắt giam." },
        { keywords: ["trung thuong", "trúng thưởng", "nhan thuong", "nhận thưởng", "qua tri an", "quà tri ân"], weight: 25, reason: "Dụ dỗ nhận quà trúng thưởng giá trị cao." },
        { keywords: ["viec nhe luong cao", "việc nhẹ lương cao", "nap tien lam nhiem vu", "nạp tiền"], weight: 25, reason: "Dấu hiệu bẫy tuyển dụng việc nhẹ lương cao / nạp tiền làm nhiệm vụ." },
        { keywords: ["click vao link", "nhap vao link", "truy cap link", "link", "http", "nhan-qua"], weight: 20, reason: "Dẫn dụ nhấp vào đường dẫn liên kết lạ không rõ nguồn gốc." },
        { keywords: ["chuyen tien", "chuyển tiền", "hoan tien", "hoàn tiền", "mhoan tien"], weight: 20, reason: "Yêu cầu thực hiện giao dịch chuyển tiền khẩn cấp." }
    ];

    let riskScore = 0;

    for (const rule of scamRules) {
        for (const kw of rule.keywords) {
            if (cleanText.includes(kw)) {
                riskScore += rule.weight;
                matchedKeywords.push(kw);
                reasons.push(rule.reason);
                break;
            }
        }
    }

    if (riskScore > 100) riskScore = 100;

    let isScam = false;
    let isSuspicious = false;
    let riskLevel = "LOW";

    if (riskScore >= 70) {
        isScam = true;
        isSuspicious = true;
        riskLevel = "CRITICAL";
        reasons.unshift("Cảnh báo khẩn cấp: Tin nhắn có nhiều dấu hiệu kịch bản lừa đảo chiếm đoạt tài khoản hoặc mạo danh cơ quan chức năng!");
    } else if (riskScore >= 40) {
        isScam = true;
        isSuspicious = true;
        riskLevel = "HIGH";
        reasons.unshift("Cảnh báo: Nội dung chứa từ khóa đáng nghi ngờ. Cần cảnh giác cao độ.");
    } else if (riskScore >= 20) {
        isSuspicious = true;
        riskLevel = "MEDIUM";
        reasons.unshift("Chú ý: Nội dung chứa từ khóa nhạy cảm, cần xác minh kỹ trước khi thực hiện.");
    } else {
        reasons.push("Chưa phát hiện từ khóa nguy hiểm từ kịch bản phân tích văn bản.");
    }

    let recommendation = "Chưa phát hiện dấu hiệu lừa đảo. Cần cảnh giác khi nhận tin nhắn lạ.";
    if (riskLevel === "CRITICAL" || riskLevel === "HIGH") {
        recommendation = "Tuyệt đối không cung cấp mã OTP, mật khẩu, không nhấp link lạ và không chuyển tiền theo hướng dẫn trong tin nhắn!";
    }

    return {
        valid: true,
        normalizedContent: inputText.trim(),
        risk_score: riskScore,
        riskLevel,
        risk_level: riskLevel,
        isSuspicious,
        isScam,
        riskScore,
        reasons,
        detection_reasons: reasons,
        matchedKeywords,
        recommendation
    };
}

module.exports = { scanContent };
