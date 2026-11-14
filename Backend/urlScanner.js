
const { URL } = require("url");

function scanUrl(input) {
    const reasons = [];
    const matchedKeywords = [];

    if (typeof input !== "string" || input.trim() === "") {
        return {
            valid: false,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["URL không được để trống"],
            matchedKeywords
        };
    }

    let url = input.trim();

    // Thêm giao thức https nếu người dùng chưa nhập
    if (!/^https?:\/\//i.test(url)) {
        url = "https://" + url;
    }

    let parsedUrl;

    try {
        parsedUrl = new URL(url);
    } catch (error) {
        return {
            valid: false,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["URL không hợp lệ"],
            matchedKeywords
        };
    }

    const hostname = parsedUrl.hostname.toLowerCase();
    const protocol = parsedUrl.protocol;

    // Chỉ chấp nhận HTTP hoặc HTTPS
    if (protocol !== "http:" && protocol !== "https:") {
        return {
            valid: false,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Giao thức URL không được hỗ trợ"],
            matchedKeywords
        };
    }

    const fullUrl = parsedUrl.href.toLowerCase();

    // 1. Kiểm tra địa chỉ IP được dùng làm tên miền
    const isIPv4 = /^\d{1,3}(?:\.\d{1,3}){3}$/.test(hostname);

    if (isIPv4) {
        reasons.push("URL sử dụng địa chỉ IP thay vì tên miền");
    }

    // 2. Kiểm tra tên miền có nhiều dấu gạch nối
    const hyphenCount = (hostname.match(/-/g) || []).length;

    if (hyphenCount >= 3) {
        reasons.push("Tên miền có nhiều dấu gạch nối bất thường");
    }

    // 3. Kiểm tra tên miền có nhiều cấp phụ
    const subdomainCount = hostname.split(".").length - 2;

    if (subdomainCount >= 4) {
        reasons.push("Tên miền có nhiều cấp tên miền phụ");
    }

    // 4. Kiểm tra ký tự @ trong URL
    if (parsedUrl.username || parsedUrl.password || fullUrl.includes("@")) {
        reasons.push("URL chứa ký tự hoặc thông tin đăng nhập đáng ngờ");
    }

    // 5. Kiểm tra URL sử dụng HTTP không mã hóa
    if (protocol === "http:") {
        reasons.push("URL sử dụng HTTP thay vì HTTPS");
    }

    // 6. Kiểm tra từ khóa thường xuất hiện trong URL lừa đảo
    const suspiciousKeywords = [
        "login",
        "verify",
        "verification",
        "account",
        "password",
        "bank",
        "wallet",
        "secure",
        "update",
        "confirm",
        "gift",
        "prize",
        "bonus",
        "claim",
        "free",
        "urgent",
        "suspended",
        "payment"
    ];

    for (const keyword of suspiciousKeywords) {
        if (fullUrl.includes(keyword)) {
            matchedKeywords.push(keyword);
        }
    }

    if (matchedKeywords.length > 0) {
        reasons.push(
            "URL chứa từ khóa cần chú ý: " + matchedKeywords.join(", ")
        );
    }

    // Xác định kết quả dựa trên dấu hiệu, không tính điểm
    const isSuspicious = reasons.length > 0;

    if (!isSuspicious) {
        reasons.push("Chưa phát hiện dấu hiệu đáng ngờ từ các quy tắc cơ bản");
    }

    const riskLevel = isSuspicious ? "MEDIUM" : "LOW";

    return {
        valid: true,
        normalizedUrl: parsedUrl.href,
        hostname,
        protocol,
        riskLevel,
        isSuspicious,
        reasons,
        matchedKeywords
    };
}

module.exports = { scanUrl };
