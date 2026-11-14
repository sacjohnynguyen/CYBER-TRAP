
// ======================================================
// 1. HÀM KIỂM TRA URL
// ======================================================

function scanUrl(inputUrl) {

    // ----------------------------------------------
    // Kiểm tra dữ liệu đầu vào
    // ----------------------------------------------

    if (!inputUrl || typeof inputUrl !== "string") {

        return {
            valid: false,
            ruleScore: 0,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: [
                "URL không hợp lệ"
            ]
        };

    }


    // Xóa khoảng trắng ở đầu và cuối

    let url = inputUrl.trim();


    // ----------------------------------------------
    // Nếu người dùng nhập:
    //
    // google.com
    //
    // thì tự thêm https://
    // ----------------------------------------------

    if (!/^https?:\/\//i.test(url)) {

        url = "https://" + url;

    }


    // ==================================================
    // 2. PHÂN TÍCH URL
    // ==================================================

    let parsedUrl;

    try {

        parsedUrl = new URL(url);

    } catch (error) {

        return {
            valid: false,
            ruleScore: 0,
            riskLevel: "UNKNOWN",
            isSuspicious: false,
            reasons: [
                "URL không đúng định dạng"
            ]
        };

    }


    // ==================================================
    // 3. LẤY THÔNG TIN URL
    // ==================================================

    const protocol = parsedUrl.protocol;

    const hostname = parsedUrl.hostname.toLowerCase();

    const pathname = parsedUrl.pathname.toLowerCase();

    const fullUrl = url.toLowerCase();


    // Điểm rủi ro ban đầu

    let score = 0;


    // Danh sách lý do tại sao URL bị cộng điểm

    let reasons = [];



    // ==================================================
    // RULE 1: HTTP
    // ==================================================
    //
    // HTTPS mã hóa kết nối.
    //
    // HTTP không tự động có nghĩa là lừa đảo,
    // nhưng đây có thể là một dấu hiệu rủi ro.
    //
    // ==================================================

    if (protocol === "http:") {

        score += 10;

        reasons.push(
            "URL sử dụng HTTP thay vì HTTPS"
        );

    }



    // ==================================================
    // RULE 2: URL SỬ DỤNG IP THAY CHO DOMAIN
    // ==================================================
    //
    // Ví dụ:
    //
    // http://192.168.1.100/login
    //
    // Một số link lừa đảo có thể sử dụng IP trực tiếp.
    //
    // ==================================================

    const ipRegex =
        /^(?:\d{1,3}\.){3}\d{1,3}$/;


    if (ipRegex.test(hostname)) {

        score += 25;

        reasons.push(
            "URL sử dụng địa chỉ IP thay cho tên miền"
        );

    }



    // ==================================================
    // RULE 3: DOMAIN CÓ KÝ TỰ @
    // ==================================================
    //
    // Ví dụ:
    //
    // https://google.com@evil.com
    //
    // Trình duyệt thực sự sẽ truy cập evil.com.
    //
    // Đây là dấu hiệu đáng chú ý.
    //
    // ==================================================

    if (parsedUrl.username || parsedUrl.password) {

        score += 25;

        reasons.push(
            "URL chứa thông tin đăng nhập hoặc ký tự @ đáng ngờ"
        );

    }



    // ==================================================
    // RULE 4: DOMAIN DÀI BẤT THƯỜNG
    // ==================================================

    if (hostname.length > 40) {

        score += 10;

        reasons.push(
            "Tên miền dài bất thường"
        );

    }



    // ==================================================
    // RULE 5: URL QUÁ DÀI
    // ==================================================

    if (url.length > 150) {

        score += 10;

        reasons.push(
            "URL có độ dài bất thường"
        );

    }



    // ==================================================
    // RULE 6: KIỂM TRA TỪ KHÓA ĐÁNG NGỜ
    // ==================================================
    //
    // Đây chỉ là RULE hỗ trợ.
    // Không nên kết luận một URL là lừa đảo
    // chỉ vì nó chứa một từ khóa.
    //
    // ==================================================

    const suspiciousKeywords = [

        "login",
        "signin",
        "verify",
        "verification",
        "secure",
        "security",
        "account",
        "update",
        "confirm",
        "password",
        "bank",
        "payment",
        "wallet",
        "gift",
        "bonus",
        "free",
        "prize",
        "reward"

    ];


    let keywordFound = [];


    for (const keyword of suspiciousKeywords) {

        if (fullUrl.includes(keyword)) {

            keywordFound.push(keyword);

        }

    }


    // Nếu có từ khóa đáng ngờ

    if (keywordFound.length > 0) {

        // Tối đa cộng 20 điểm
        // để tránh URL chứa quá nhiều keyword
        // bị cộng điểm quá lớn.

        const keywordScore =
            Math.min(keywordFound.length * 5, 20);

        score += keywordScore;


        reasons.push(
            "URL chứa từ khóa thường xuất hiện trong các trang yêu cầu đăng nhập/xác minh"
        );

    }



    // ==================================================
    // RULE 7: DOMAIN DÙNG PUNYCODE
    // ==================================================
    //
    // Punycode thường bắt đầu bằng:
    //
    // xn--
    //
    // Nó không có nghĩa chắc chắn là lừa đảo,
    // nhưng có thể được sử dụng trong một số
    // kiểu giả mạo tên miền.
    //
    // ==================================================

    if (hostname.includes("xn--")) {

        score += 15;

        reasons.push(
            "Tên miền sử dụng Punycode"
        );

    }



    // ==================================================
    // RULE 8: SHORTENER
    // ==================================================
    //
    // Một số dịch vụ rút gọn URL làm cho
    // người dùng khó nhìn thấy domain đích.
    //
    // ==================================================

    const shortenerDomains = [

        "bit.ly",
        "tinyurl.com",
        "t.co",
        "is.gd",
        "cutt.ly",
        "rebrand.ly"

    ];


    if (shortenerDomains.includes(hostname)) {

        score += 10;

        reasons.push(
            "URL sử dụng dịch vụ rút gọn link"
        );

    }



    // ==================================================
    // 4. GIỚI HẠN ĐIỂM
    // ==================================================

    if (score > 100) {

        score = 100;

    }



    // ==================================================
    // 5. XÁC ĐỊNH MỨC ĐỘ RỦI RO
    // ==================================================

    let riskLevel = "LOW";


    if (score >= 80) {

        riskLevel = "CRITICAL";

    }

    else if (score >= 60) {

        riskLevel = "HIGH";

    }

    else if (score >= 30) {

        riskLevel = "MEDIUM";

    }

    else {

        riskLevel = "LOW";

    }



    // ==================================================
    // 6. XÁC ĐỊNH CÓ DẤU HIỆU ĐÁNG NGỜ HAY KHÔNG
    // ==================================================

    const isSuspicious = score >= 30;



    // ==================================================
    // 7. NẾU KHÔNG CÓ DẤU HIỆU
    // ==================================================

    if (reasons.length === 0) {

        reasons.push(
            "Chưa phát hiện dấu hiệu đáng ngờ từ các rule cơ bản"
        );

    }



    // ==================================================
    // 8. TRẢ KẾT QUẢ
    // ==================================================

    return {

        valid: true,

        normalizedUrl: url,

        hostname: hostname,

        protocol: protocol,

        ruleScore: score,

        riskLevel: riskLevel,

        isSuspicious: isSuspicious,

        reasons: reasons

    };

}



// ======================================================
// EXPORT FUNCTION
// ======================================================
//
// scan.routes.js sẽ import hàm này bằng:
//
// const { scanUrl } = require("./urlScanner");
// ======================================================

module.exports = {
    scanUrl
};