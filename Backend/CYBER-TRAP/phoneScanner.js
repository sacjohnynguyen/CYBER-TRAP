
function scanPhone(inputPhone) {
    if (!inputPhone || typeof inputPhone !== "string") {
        return {
            valid: false,
            normalizedPhone: "",
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Số điện thoại không hợp lệ"]
        };
    }

    // Chuẩn hóa số điện thoại
    let phone = inputPhone.trim().replace(/[\s().-]/g, "");

    // Chuyển mã quốc gia Việt Nam về đầu số 0
    if (phone.startsWith("+84")) {
        phone = "0" + phone.substring(3);
    } else if (phone.startsWith("84")) {
        phone = "0" + phone.substring(2);
    }

    // Kiểm tra số điện thoại cố định: mã vùng 024 hoặc 028
    if (/^0(24|28)[0-9]{7}$/.test(phone)) {
        return {
            valid: true,
            normalizedPhone: phone,
            riskLevel: "MEDIUM",
            isSuspicious: true,
            reasons: [
                "Số điện thoại cố định thuộc mã vùng 024 hoặc 028. Cần xác minh người gọi; mã vùng không tự chứng minh lừa đảo."
            ]
        };
    }

    // Kiểm tra số điện thoại di động Việt Nam gồm 10 chữ số
    const mobileRegex = /^0(3|5|7|8|9)[0-9]{8}$/;

    if (!mobileRegex.test(phone)) {
        return {
            valid: false,
            normalizedPhone: phone,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: ["Số điện thoại không đúng định dạng Việt Nam"]
        };
    }

    // Số hợp lệ không đồng nghĩa với số an toàn.
    // Chưa có bằng chứng thì không kết luận lừa đảo.
    return {
        valid: true,
        normalizedPhone: phone,
        riskLevel: "LOW",
        isSuspicious: false,
        reasons: [
            "Số điện thoại đúng định dạng. Chưa có đủ thông tin để kết luận đây là số lừa đảo."
        ]
    };
}

module.exports = { scanPhone };
