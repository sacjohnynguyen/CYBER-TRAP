/**
 * Quét nội dung văn bản (Tin nhắn SMS / Zalo / Telegram / Tin đồn lừa đảo)
 */
function scanContent(inputText) {
    if (!inputText || typeof inputText !== "string" || !inputText.trim()) {
        return {
            valid: false,
            normalizedContent: "",
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            isScam: false,
            reasons: ["Nội dung văn bản không được để trống"],
            matchedKeywords: []
        };
    }

    const cleanText = inputText.trim().toLowerCase();
    const matchedKeywords = [];
    const reasons = [];

    // Danh sách từ khóa lừa đảo phổ biến và trọng số nguy hiểm
    const scamRules = [
        { keyword: "cung cap otp", weight: 5, reason: "Yêu cầu cung cấp mã OTP" },
        { keyword: "ma otp", weight: 4, reason: "Đề cập đến mã xác thực OTP" },
        { keyword: "mat khau", weight: 3, reason: "Yêu cầu hoặc nhắc đến mật khẩu" },
        { keyword: "khoa tai khoan", weight: 4, reason: "Đe dọa khóa tài khoản ngân hàng / mạng xã hội" },
        { keyword: "bi khoa", weight: 3, reason: "Thông báo tài khoản bị tạm khóa" },
        { keyword: "cong an", weight: 4, reason: "Mạo danh cơ quan Công an" },
        { keyword: "vien kiem sat", weight: 5, reason: "Mạo danh Viện kiểm sát" },
        { keyword: "toa an", weight: 4, reason: "Đe dọa liên quan đến Tòa án" },
        { keyword: "lenh bat", weight: 5, reason: "Đe dọa lệnh bắt giam / điều tra" },
        { keyword: "trung thuong", weight: 3, reason: "Dấu hiệu dụ dỗ trúng thưởng giá trị cao" },
        { keyword: "nhan thuong", weight: 3, reason: "Dấu hiệu nhận thưởng miễn phí" },
        { keyword: "qua tri an", weight: 3, reason: "Tặng quà tri ân lừa tiền cước" },
        { keyword: "chuyen tien gap", weight: 4, reason: "Thúc ép chuyển tiền gấp" },
        { keyword: "nap tien", weight: 2, reason: "Yêu cầu nạp tiền" },
        { keyword: "click vao link", weight: 3, reason: "Dẫn dụ bấm vào đường link lạ" },
        { keyword: "nhap vao lien ket", weight: 3, reason: "Yêu cầu nhấp vào liên kết ngoài" },
        { keyword: "xac thuc ngay", weight: 3, reason: "Tạo cảm giác cấp bách để nạn nhân mất cảnh giác" },
        { keyword: "viec nhe luong cao", weight: 4, reason: "Lừa đảo tuyển dụng việc nhẹ lương cao" }
    ];

    let riskScore = 0;

    for (const rule of scamRules) {
        if (cleanText.includes(rule.keyword)) {
            riskScore += rule.weight;
            matchedKeywords.push(rule.keyword);
            reasons.push(rule.reason);
        }
    }

    let isScam = false;
    let isSuspicious = false;
    let riskLevel = "LOW";

    if (riskScore >= 5) {
        isScam = true;
        isSuspicious = true;
        riskLevel = "CRITICAL";
        reasons.unshift("Cảnh báo khẩn cấp: Nội dung có dấu hiệu lừa đảo chiếm đoạt tài khoản hoặc mạo danh cơ quan pháp luật!");
    } else if (riskScore >= 3) {
        isSuspicious = true;
        riskLevel = "MEDIUM";
        reasons.unshift("Chú ý: Nội dung mang dấu hiệu dụ dỗ trúng thưởng, tạo sự khẩn cấp hoặc yêu cầu thông tin nhạy cảm.");
    } else {
        reasons.push("Chưa phát hiện từ khóa nguy hiểm từ kịch bản phân tích văn bản.");
    }

    return {
        valid: true,
        normalizedContent: inputText.trim(),
        riskLevel,
        isSuspicious,
        isScam,
        riskScore,
        reasons,
        matchedKeywords
    };
}

module.exports = { scanContent };
