# CYBER-TRAP
Một ứng dụng cho phép người dùng phòng tránh các lừa đảo trực tuyến và bảo vệ tài khoản

## Gửi báo cáo nội dung đáng ngờ

`POST /api/v1/reports` tiếp nhận báo cáo và lưu ở trạng thái `PENDING`.

Request JSON:

```json
{
  "inputType": "URL",
  "inputValue": "https://example.com",
  "reason": "Trang web yêu cầu thông tin đăng nhập ngân hàng"
}
```

`inputType` hỗ trợ `URL`, `PHONE`, `EMAIL`, `CONTENT` và `TEXT`.
`inputValue` bắt buộc, tối đa 5000 ký tự; `reason` bắt buộc, tối đa 2000 ký tự.
Khi thành công, API trả HTTP `201` cùng `data.reportId` và trạng thái báo cáo.

## Lấy báo cáo của người dùng

`GET /api/v1/reports/user/{id}` trả danh sách báo cáo của người dùng, mới nhất trước.
Gửi access token nhận được từ API đăng nhập trong header `Authorization`.
Người dùng chỉ có thể xem báo cáo gắn với ID trong token của chính mình.

Các báo cáo gửi bằng `POST /api/v1/reports` kèm access token sẽ được liên kết với tài khoản đó.
Báo cáo gửi không có token vẫn được tiếp nhận nhưng không gắn với người dùng và không xuất hiện trong danh sách này.
