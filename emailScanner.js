function scanEmail(inputEmail) {
    if (!inputEmail || typeof inputEmail !== "string") {
        return {
            valid: false,
            ruleScore: 0,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Email không hợp lệ"]
        };
    }

    const email = inputEmail.trim().toLowerCase();

    // Kiểm tra format Email
    const emailRegex =
        /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    if (!emailRegex.test(email)) {
        return {
            valid: false,
            normalizedEmail: email,
            ruleScore: 0,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Email không đúng định dạng"]
        };
    }

    let score = 0;
    let reasons = [];

    // Tách username và domain
    const parts = email.split("@");

    const username = parts[0];
    const domain = parts[1];

    // Username quá dài
    if (username.length > 30) {
        score += 10;
        reasons.push("Tên người dùng của email dài bất thường");
    }

    // Domain quá dài
    if (domain.length > 40) {
        score += 10;
        reasons.push("Tên miền email dài bất thường");
    }

    // Kiểm tra domain đáng ngờ
    const suspiciousDomains = [
        "tempmail.com",
        "10minutemail.com",
        "guerrillamail.com",
        "mailinator.com"
    ];

    if (suspiciousDomains.includes(domain)) {
        score += 40;
        reasons.push("Email sử dụng dịch vụ email tạm thời");
    }

    // Kiểm tra nhiều số trong username
    const numberCount = (username.match(/[0-9]/g) || []).length;

    if (numberCount >= 5) {
        score += 10;
        reasons.push("Tên người dùng chứa nhiều chữ số");
    }

    // Kiểm tra ký tự lặp
    if (/(.)\1{4,}/.test(username)) {
        score += 15;
        reasons.push("Tên người dùng chứa nhiều ký tự lặp lại");
    }

    if (score > 100) {
        score = 100;
    }

    let riskLevel = "LOW";

    if (score >= 80) {
        riskLevel = "CRITICAL";
    } else if (score >= 60) {
        riskLevel = "HIGH";
    } else if (score >= 30) {
        riskLevel = "MEDIUM";
    }

    const isSuspicious = score >= 30;

    if (reasons.length === 0) {
        reasons.push("Chưa phát hiện dấu hiệu đáng ngờ từ các rule cơ bản");
    }

    return {
        valid: true,
        normalizedEmail: email,
        ruleScore: score,
        riskLevel,
        isSuspicious,
        reasons
    };
}

module.exports = { scanEmail };