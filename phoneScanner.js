function scanPhone(inputPhone) {
    if (!inputPhone || typeof inputPhone !== "string") {
        return {
            valid: false,
            ruleScore: 0,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Số điện thoại không hợp lệ"]
        };
    }

    // Xóa khoảng trắng, dấu chấm, dấu gạch ngang
    let phone = inputPhone.trim().replace(/[\s.-]/g, "");

    // Chuyển +84 thành 0
    if (phone.startsWith("+84")) {
        phone = "0" + phone.substring(3);
    }

    // Kiểm tra số điện thoại Việt Nam
    const phoneRegex = /^0(3|5|7|8|9)[0-9]{8}$/;

    if (!phoneRegex.test(phone)) {
        return {
            valid: false,
            normalizedPhone: phone,
            ruleScore: 0,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Số điện thoại không đúng định dạng Việt Nam"]
        };
    }

    let score = 0;
    let reasons = [];

    // Kiểm tra số lặp bất thường
    const digits = phone.substring(1);

    if (/(\d)\1{5,}/.test(digits)) {
        score += 20;
        reasons.push("Số điện thoại chứa nhiều chữ số lặp lại");
    }

    // Kiểm tra các mẫu số quá đơn giản
    if (/^0(3|5|7|8|9)12345678$/.test(phone)) {
        score += 20;
        reasons.push("Số điện thoại có chuỗi số đơn giản bất thường");
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
        normalizedPhone: phone,
        ruleScore: score,
        riskLevel,
        isSuspicious,
        reasons
    };
}

module.exports = { scanPhone };