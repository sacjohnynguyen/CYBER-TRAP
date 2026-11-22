/**
 * VIETNAM EMAIL SCAM DETECTION SCANNER - CYBER TRAP
 */

function scanEmail(inputEmail) {
    if (!inputEmail || typeof inputEmail !== "string" || !inputEmail.trim()) {
        return {
            valid: false,
            normalizedEmail: "",
            risk_score: 0,
            riskLevel: "UNKNOWN",
            is_scam: false,
            isSuspicious: false,
            detection_reasons: ["Địa chỉ Email không được để trống hoặc không hợp lệ."],
            reasons: ["Email không hợp lệ"],
            recommendation: "Không mở tệp đính kèm hoặc nhấp vào liên kết trong email lạ."
        };
    }

    const email = inputEmail.trim().toLowerCase();
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    if (!emailRegex.test(email)) {
        return {
            valid: false,
            normalizedEmail: email,
            risk_score: 0,
            riskLevel: "UNKNOWN",
            is_scam: false,
            isSuspicious: false,
            detection_reasons: ["Địa chỉ Email không đúng định dạng chuẩn (username@domain.com)."],
            reasons: ["Email không đúng định dạng"],
            recommendation: "Vui lòng kiểm tra lại định dạng địa chỉ Email."
        };
    }

    const [username, domain] = email.split("@");
    let riskScore = 10;
    const reasons = [];

    // 1. Kiểm tra từ khóa ngân hàng / hỗ trợ trên tên miền miễn phí (@gmail, @yahoo, @hotmail...)
    const freeDomains = ["gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "yandex.com"];
    const bankKeywords = [
        "cskh", "hotro", "support", "nhanthuong", "vietcombank", "bidv", "agribank",
        "techcombank", "tpbank", "mbbank", "vib", "vpbank", "sacombank", "acb", "chuyentien", "scam"
    ];

    const isFreeDomain = freeDomains.includes(domain);
    let hasBankKeyword = false;

    for (const kw of bankKeywords) {
        if (username.includes(kw)) {
            hasBankKeyword = true;
            break;
        }
    }

    if (isFreeDomain && hasBankKeyword) {
        riskScore += 75;
        reasons.push("Email sử dụng tên miền miễn phí (" + domain + ") mạo danh bộ phận hỗ trợ / chăm sóc khách hàng ngân hàng.");
    }

    // 2. Kiểm tra tên người dùng email quá dài hoặc chứa dãy số ngẫu nhiên
    if (username.length > 25) {
        riskScore += 20;
        reasons.push("Tên người dùng email dài bất thường (> 25 ký tự).");
    }

    if (/[a-z]+[0-9]{6,}/.test(username)) {
        riskScore += 25;
        reasons.push("Tên người dùng chứa chuỗi số tự động ngẫu nhiên thường dùng trong email rác.");
    }

    // 3. Kiểm tra ký tự lặp lại bất thường
    if (/(.)\1{4,}/.test(username)) {
        riskScore += 20;
        reasons.push("Tên người dùng email có chuỗi ký tự lặp lại bất thường.");
    }

    // 4. Kiểm tra các từ khóa dụ dỗ trúng thưởng
    if (username.includes("nhanthuong") || username.includes("trungthuong") || username.includes("quatang")) {
        riskScore += 40;
        reasons.push("Email mang dấu hiệu lừa đảo thông báo trúng thưởng / nhận quà tri ân.");
    }

    if (riskScore > 100) riskScore = 100;

    let isScam = false;
    let isSuspicious = false;
    let riskLevel = "LOW";

    if (riskScore >= 70) {
        riskLevel = "CRITICAL";
        isScam = true;
        isSuspicious = true;
    } else if (riskScore >= 40) {
        riskLevel = "HIGH";
        isSuspicious = true;
    } else if (riskScore >= 20) {
        riskLevel = "MEDIUM";
        isSuspicious = true;
    } else {
        reasons.push("Định dạng Email hợp lệ. Chưa phát hiện dấu hiệu bất thường từ các quy tắc phân tích tên miền.");
    }

    let recommendation = "Chưa phát hiện dấu hiệu lừa đảo. Cần thận trọng với các tệp đính kèm lạ.";
    if (riskLevel === "CRITICAL" || riskLevel === "HIGH") {
        recommendation = "Tuyệt đối không nhấp vào liên kết hoặc tải tệp đính kèm. Không cung cấp mật khẩu/OTP cho Email này.";
    }

    return {
        valid: true,
        normalizedEmail: email,
        risk_score: riskScore,
        riskLevel,
        risk_level: riskLevel,
        is_scam: isScam,
        isSuspicious,
        detection_reasons: reasons,
        reasons,
        recommendation
    };
}

module.exports = { scanEmail };
