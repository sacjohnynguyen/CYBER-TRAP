/**
 * VIETNAM PHONE SCAM DETECTION SCANNER - CYBER TRAP
 * Special AI Agent module for analyzing & scoring phone scam risks in Vietnam.
 */

// Danh sách mã quốc gia nhạy cảm thường gặp trong kịch bản nháy máy dụ gọi lại
const SUSPICIOUS_COUNTRY_CODES = [
    { code: "+216", country: "Tunisia", name: "Tunisia (+216)" },
    { code: "+373", country: "Moldova", name: "Moldova (+373)" },
    { code: "+240", country: "Equatorial Guinea", name: "Xích Đạo Guinea (+240)" },
    { code: "+252", country: "Somalia", name: "Somalia (+252)" },
    { code: "+231", country: "Liberia", name: "Liberia (+231)" }
];

/**
 * Chuẩn hóa và phân tích số điện thoại
 * @param {string} inputPhone - Chuỗi số điện thoại đầu vào
 * @param {object} options - Cấu hình bổ sung (nội dung tin nhắn/cuộc gọi kèm theo nếu có)
 */
function scanPhone(inputPhone, options = {}) {
    if (!inputPhone || typeof inputPhone !== "string" || !inputPhone.trim()) {
        return {
            valid: false,
            phone_number: "",
            country: "Unknown",
            phone_type: "Unknown",
            risk_score: 0,
            risk_level: "LOW",
            is_scam: false,
            isSuspicious: false,
            detection_reasons: ["Số điện thoại không hợp lệ hoặc để trống."],
            reasons: ["Số điện thoại không hợp lệ hoặc để trống."],
            recommendation: "Không thực hiện cuộc gọi hoặc cung cấp thông tin.",
            data_status: "UNVERIFIED"
        };
    }

    // 1. Chuẩn hóa số điện thoại
    let rawStr = inputPhone.trim();
    let phone = rawStr.replace(/[\s().-]/g, "");

    // Xử lý mã quốc gia Việt Nam (+84 hoặc 84) về đầu 0
    if (phone.startsWith("+84")) {
        phone = "0" + phone.substring(3);
    } else if (phone.startsWith("84") && (phone.length === 11 || phone.length === 12)) {
        phone = "0" + phone.substring(2);
    }

    let country = "Vietnam";
    let phoneType = "Mobile";
    let isSuspiciousCountry = false;
    let suspiciousCountryInfo = null;

    // 2. Kiểm tra số điện thoại quốc tế
    if (rawStr.startsWith("+") && !rawStr.startsWith("+84")) {
        country = "International";
        phoneType = "International";

        for (const c of SUSPICIOUS_COUNTRY_CODES) {
            if (rawStr.startsWith(c.code)) {
                isSuspiciousCountry = true;
                suspiciousCountryInfo = c;
                country = c.country;
                break;
            }
        }
    }

    let riskScore = 10;
    let detectionReasons = [];
    let isScam = false;
    let isSuspicious = false;

    // 3. Phân loại & Kiểm tra định dạng đầu số Việt Nam
    if (country === "Vietnam") {
        // Cảnh báo số điện thoại di động 11 chữ số (Đầu số cũ 01x đã ngừng chuyển đổi từ 2018)
        if (/^01[0-9]{9}$/.test(phone) || (phone.startsWith("0") && phone.length === 11)) {
            phoneType = "Obsolete 11-digit Mobile";
            riskScore = 85;
            isScam = true;
            isSuspicious = true;
            detectionReasons.push(
                "Số điện thoại 11 chữ số thuộc các đầu số cũ (01x...) đã ngưng sử dụng từ 2018. Hiện tại các đầu 11 số chủ yếu là số giả mạo, spam lừa đảo."
            );
        } else if (/^0(24|28)[0-9]{8}$/.test(phone)) {
            phoneType = "Fixed Line (HN/HCM)";
        } else if (/^02[0-9]{8,9}$/.test(phone)) {
            phoneType = "Fixed Line (Province)";
        } else if (/^0(3|5|7|8|9)[0-9]{8}$/.test(phone)) {
            phoneType = "Mobile (10 digits)";
        } else if (!rawStr.startsWith("+")) {
            return {
                valid: false,
                phone_number: phone,
                country: "Vietnam",
                phone_type: "Unknown",
                risk_score: 0,
                risk_level: "LOW",
                is_scam: false,
                isSuspicious: false,
                detection_reasons: ["Số điện thoại không đúng định dạng di động hoặc cố định Việt Nam."],
                reasons: ["Số điện thoại không đúng định dạng Việt Nam."],
                recommendation: "Vui lòng kiểm tra lại định dạng số điện thoại.",
                data_status: "UNVERIFIED"
            };
        }
    }

    // A. Xử lý cuộc gọi quốc tế nhạy cảm
    if (isSuspiciousCountry) {
        riskScore = Math.max(riskScore, 45);
        isSuspicious = true;
        detectionReasons.push(
            `Mã quốc gia quốc tế nhạy cảm (${suspiciousCountryInfo.name}) thường xuất hiện trong các kịch bản nháy máy dụ gọi lại thu cước viễn thông đắt đỏ.`
        );
    } else if (country === "International") {
        riskScore = Math.max(riskScore, 30);
        isSuspicious = true;
        detectionReasons.push("Cuộc gọi từ quốc tế. Cần thận trọng nếu không có người thân ở nước ngoài.");
    }

    // B. Phân tích nội dung / Hành vi lừa đảo đi kèm (nếu có context)
    const contextText = String(options.content || options.context || "").toLowerCase();
    if (contextText) {
        if (contextText.includes("otp") || contextText.includes("mã xác thực") || contextText.includes("mật khẩu")) {
            riskScore += 35;
            detectionReasons.push("Yêu cầu cung cấp mã xác thực OTP hoặc mật khẩu tài khoản.");
        }
        if (contextText.includes("công an") || contextText.includes("tòa án") || contextText.includes("viện kiểm sát") || contextText.includes("lệnh bắt")) {
            riskScore += 40;
            detectionReasons.push("Dấu hiệu mạo danh cơ quan công an, tòa án, viện kiểm sát đe dọa người nghe.");
        }
        if (contextText.includes("ngân hàng") || contextText.includes("chuyển tiền") || contextText.includes("xác minh tài khoản")) {
            riskScore += 30;
            detectionReasons.push("Yêu cầu chuyển tiền xác minh hoặc giả danh nhân viên ngân hàng.");
        }
        if (contextText.includes("trúng thưởng") || contextText.includes("nhận quà") || contextText.includes("việc nhẹ lương cao")) {
            riskScore += 25;
            detectionReasons.push("Dụ dỗ nhận quà trúng thưởng hoặc tuyển dụng làm nhiệm vụ online.");
        }
        if (contextText.includes("http") || contextText.includes("link") || contextText.includes("truy cập")) {
            riskScore += 20;
            detectionReasons.push("Yêu cầu truy cập đường dẫn liên kết không rõ nguồn gốc.");
        }
    }

    // Phân loại đầu số cố định (024, 028)
    if (phoneType.startsWith("Fixed Line") && detectionReasons.length === 0) {
        detectionReasons.push("Số điện thoại cố định. Cần xác minh danh tính tổ chức người gọi trước khi thực hiện giao dịch.");
    }

    // 5. Chuẩn hóa điểm rủi ro (0 - 100) & Xếp loại Risk Level
    if (riskScore > 100) riskScore = 100;

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
        riskLevel = "LOW";
        if (detectionReasons.length === 0) {
            detectionReasons.push("Số điện thoại di động 10 số hợp pháp. Chưa phát hiện dấu hiệu bất thường trong cơ sở dữ liệu.");
        }
    }

    // 6. Đưa ra khuyến nghị phù hợp cho người dùng (Recommendation)
    let recommendation = "Chưa phát hiện dấu hiệu lừa đảo. Cần cảnh giác khi giao dịch với số lạ.";
    if (riskLevel === "CRITICAL") {
        recommendation = "Nguy cơ lừa đảo cực kỳ cao! Không cung cấp OTP, mật khẩu, không chuyển tiền. Tắt máy, chặn số và báo cáo ngay.";
    } else if (riskLevel === "HIGH") {
        recommendation = "Số điện thoại có nhiều dấu hiệu đáng nghi. Không gọi lại cho các số 11 số cũ, số quốc tế nhạy cảm hoặc làm theo yêu cầu chuyển tiền.";
    } else if (riskLevel === "MEDIUM") {
        recommendation = "Cần chú ý xác minh danh tính người gọi. Không làm theo bất kỳ hướng dẫn tài chính nào qua điện thoại.";
    }

    return {
        valid: true,
        phone_number: phone,
        country: country,
        phone_type: phoneType,
        risk_score: riskScore,
        risk_level: riskLevel,
        is_scam: isScam,
        isSuspicious: isSuspicious,
        detection_reasons: detectionReasons,
        reasons: detectionReasons,
        recommendation: recommendation,
        data_status: "UNVERIFIED"
    };
}

module.exports = { scanPhone };
