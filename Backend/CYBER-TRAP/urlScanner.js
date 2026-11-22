/**
 * VIETNAM URL / WEBSITE SCAM DETECTION SCANNER - CYBER TRAP
 */

const { URL } = require("url");

function scanUrl(input) {
    const reasons = [];
    const matchedKeywords = [];

    if (typeof input !== "string" || input.trim() === "") {
        return {
            valid: false,
            risk_score: 0,
            riskLevel: "UNKNOWN",
            is_scam: false,
            isSuspicious: false,
            reasons: ["URL không được để trống"],
            detection_reasons: ["URL không được để trống"],
            matchedKeywords,
            recommendation: "Không truy cập vào các đường dẫn không xác định."
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
            risk_score: 0,
            riskLevel: "UNKNOWN",
            is_scam: false,
            isSuspicious: false,
            reasons: ["URL không hợp lệ"],
            detection_reasons: ["URL không hợp lệ"],
            matchedKeywords,
            recommendation: "Kiểm tra lại đường dẫn website."
        };
    }

    const hostname = parsedUrl.hostname.toLowerCase();
    const protocol = parsedUrl.protocol;

    if (protocol !== "http:" && protocol !== "https:") {
        return {
            valid: false,
            risk_score: 0,
            riskLevel: "UNKNOWN",
            is_scam: false,
            isSuspicious: false,
            reasons: ["Giao thức URL không được hỗ trợ"],
            detection_reasons: ["Giao thức URL không được hỗ trợ"],
            matchedKeywords,
            recommendation: "Chỉ truy cập các website hỗ trợ giao thức HTTP/HTTPS."
        };
    }

    const fullUrl = parsedUrl.href.toLowerCase();
    let riskScore = 10;

    // 1. Kiểm tra Tên miền giả mạo thương hiệu lừa đảo (Phishing Typosquatting)
    const phishingKeywords = [
        "vay-tien", "nhan-qua", "trung-thuong", "dangnhap-", "vietcombank-", "agribank-",
        "bidv-", "techcombank-", "mbbank-", "vpbank-", "fecredit-", "shopee-", "tin-dung", "hoan-tien"
    ];

    for (const kw of phishingKeywords) {
        if (hostname.includes(kw)) {
            riskScore += 45;
            matchedKeywords.push(kw);
            reasons.push("Tên miền có chứa từ khóa giả mạo ngân hàng / dịch vụ tài chính / trúng thưởng (" + kw + ").");
        }
    }

    // 2. Kiểm tra tên miền đuôi giá rẻ / miễn phí dễ bị lạm dụng (.xyz, .top, .site, .tk...)
    const suspiciousTlds = [".xyz", ".top", ".site", ".online", ".club", ".work", ".tk", ".ml", ".ga", ".cf", ".gq", ".cc"];
    for (const tld of suspiciousTlds) {
        if (hostname.endsWith(tld)) {
            riskScore += 25;
            reasons.push("Tên miền sử dụng đuôi mở rộng (" + tld + ") thường được kẻ lừa đảo sử dụng để tạo web rác.");
            break;
        }
    }

    // 3. Kiểm tra địa chỉ IP được dùng làm tên miền
    const isIPv4 = /^\d{1,3}(?:\.\d{1,3}){3}$/.test(hostname);
    if (isIPv4) {
        riskScore += 35;
        reasons.push("Website sử dụng trực tiếp địa chỉ IP thay vì tên miền chính thức.");
    }

    // 4. Kiểm tra tên miền có nhiều dấu gạch nối
    const hyphenCount = (hostname.match(/-/g) || []).length;
    if (hyphenCount >= 3) {
        riskScore += 20;
        reasons.push("Tên miền chứa nhiều dấu gạch nối bất thường.");
    }

    // 5. Kiểm tra ký tự @ hoặc thông tin đăng nhập trong URL
    if (parsedUrl.username || parsedUrl.password || fullUrl.includes("@")) {
        riskScore += 30;
        reasons.push("URL chứa ký tự @ hoặc thông tin xác thực bất thường.");
    }

    // 6. Kiểm tra HTTP không mã hóa
    if (protocol === "http:") {
        riskScore += 15;
        reasons.push("Website sử dụng giao thức HTTP không mã hóa an toàn (không có HTTPS).");
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
        reasons.push("Chưa phát hiện dấu hiệu giả mạo từ cấu trúc tên miền.");
    }

    let recommendation = "Chưa phát hiện dấu hiệu lừa đảo. Kiểm tra kỹ trước khi nhập thông tin cá nhân.";
    if (riskLevel === "CRITICAL" || riskLevel === "HIGH") {
        recommendation = "Website có nguy cơ giả mạo cao! Tuyệt đối không nhập mật khẩu, mã OTP hoặc thông tin tài khoản ngân hàng.";
    }

    return {
        valid: true,
        normalizedUrl: parsedUrl.href,
        hostname,
        protocol,
        risk_score: riskScore,
        riskLevel,
        risk_level: riskLevel,
        is_scam: isScam,
        isSuspicious,
        reasons,
        detection_reasons: reasons,
        matchedKeywords,
        recommendation
    };
}

module.exports = { scanUrl };
