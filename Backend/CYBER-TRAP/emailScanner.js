
function scanEmail(inputEmail) {
    if (!inputEmail || typeof inputEmail !== "string") {
        return {
            valid: false,
            normalizedEmail: "",
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Email không hợp lệ"]
        };
    }

    const email = inputEmail.trim().toLowerCase();

    const emailRegex =
        /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    if (!emailRegex.test(email)) {
        return {
            valid: false,
            normalizedEmail: email,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Email không đúng định dạng"]
        };
    }

    const [username] = email.split("@");
    const reasons = [];

    // Kiểm tra tên người dùng email quá dài
    if (username.length > 30) {
        reasons.push("Tên người dùng email dài bất thường");
    }

    // Kiểm tra một ký tự lặp lại từ 5 lần liên tiếp
    if (/(.)\1{4,}/.test(username)) {
        reasons.push("Tên người dùng email có ký tự lặp lại bất thường");
    }

    const isSuspicious = reasons.length > 0;

    if (!isSuspicious) {
        reasons.push(
            "Chưa phát hiện dấu hiệu đáng ngờ từ các quy tắc cơ bản"
        );
    }

    const riskLevel = isSuspicious ? "MEDIUM" : "LOW";

    return {
        valid: true,
        normalizedEmail: email,
        riskLevel,
        isSuspicious,
        reasons
    };
}

module.exports = { scanEmail };
