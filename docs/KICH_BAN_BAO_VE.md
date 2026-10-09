# Kịch bản trình diễn và hỗ trợ báo cáo

## Trình diễn khoảng 8–12 phút

1. Trang chủ/tìm kiếm: lọc giá, diện tích, địa điểm và tiện ích; xem chi phí, ảnh, địa chỉ và đánh giá phòng. Giải thích chỉ bài đã duyệt và phòng trống xuất hiện.
2. Người thuê demo 5: lưu một phòng yêu thích, gửi yêu cầu cho phòng trống của chủ trọ 2, xem PENDING.
3. Chủ trọ 2: xem yêu cầu đến, chấp nhận. Phòng chuyển RESERVED; người khác không gửi tiếp hoặc không được chấp nhận thêm.
4. Chủ trọ xác nhận nhận phòng với ngày thực tế. Hồ sơ ACTIVE, phòng OCCUPIED.
5. Người thuê viết đánh giá tại Hồ sơ thuê. Giải thích trước nhận phòng chưa đánh giá được.
6. Chủ kết thúc thuê có lý do; xem lịch sử, snapshot giá và trạng thái bài được ẩn trước khi tái mở.
7. Chủ tạo phòng mới có ảnh, lưu nháp, gửi duyệt; ADMIN duyệt; chủ bật hiện bài. Sửa bài đã duyệt thì quay về chờ duyệt.
8. ADMIN xem dashboard/xuất CSV, quản lý danh mục, ẩn một đánh giá có lý do. Khóa tài khoản demo phụ để chứng minh quyền bị thu hồi; không khóa tài khoản đang trình diễn.

Nếu demo cần lặp, dùng phòng/tài khoản khác hoặc gửi yêu cầu mới sau khi hồ sơ cũ kết thúc. Không reset DB thật để trình diễn. Seed không chạy lại nếu đã có tài khoản.

## Nội dung đưa vào báo cáo

| Chương | Nội dung và bằng chứng |
|---|---|
| 1. Tổng quan | Bài toán, tác nhân, phạm vi một phòng/một hồ sơ đang giữ; loại hợp đồng/thu tiền khỏi bản đầu |
| 2. Yêu cầu | UC01–UC16, ma trận quyền, tiền/hậu điều kiện, lỗi và ngoại lệ |
| 3. Thiết kế | MVC/phân lớp, 13 bảng, enum trạng thái, PK/FK/unique/partial unique, transaction |
| 4. Triển khai | Công nghệ, cấu hình biến môi trường, màn hình chụp từ ứng dụng chạy thực |
| 5. Kiểm thử | 16 test mặc định, browser E2E, lỗi Thymeleaf đã sửa, tách rõ native PostgreSQL chưa chạy |
| Kết luận | Kết quả, giới hạn, hướng phát triển: email thật, avatar, phân trang quản trị quy mô lớn; chưa tự thêm thanh toán |

## Câu hỏi thường gặp khi bảo vệ

**Vì sao tách Room và Listing?** Room mô tả tài sản và đang trống/đang thuê; Listing mô tả nội dung, giá niêm yết và kiểm duyệt. Một bài có thể được duyệt nhưng phòng không còn trống.

**Vì sao có RentalRequest và Tenancy?** Yêu cầu là lời đề nghị có thể từ chối/hủy; hồ sơ chỉ sinh khi được chấp nhận và lưu quá trình giữ/nhận/kết thúc.

**Ngăn hai chủ thao tác đồng thời thế nào?** Cùng room được khóa trong transaction; trạng thái được đọc lại sau khóa. Partial unique trên native PostgreSQL là bảo vệ bổ sung. Test đồng thời đã chạy H2 nhưng còn phải chạy native để xác nhận DB thực tế.

**Vì sao lưu giá snapshot?** Giá quảng cáo về sau có thể thay đổi; lịch sử thuê vẫn phải phản ánh giá tại lúc người thuê gửi yêu cầu được chấp nhận.

**Ẩn nút có đủ bảo mật không?** Không. Route kiểm tra role, Service kiểm tra chủ sở hữu, endpoint ảnh kiểm tra công khai/ownership, CSRF bảo vệ các POST.

**Mật khẩu được lưu thế nào?** BCrypt cho mật khẩu, SHA-256 cho reset token ngẫu nhiên; không lưu password/token gốc và không đưa DB password vào Git.

**Đã kiểm thử trên PostgreSQL chưa?** Chưa trong môi trường bàn giao; syntax migration đã được parser PostgreSQL chấp nhận. Chỉ sau chạy bộ native trên database test riêng mới ghi kết quả tương thích PostgreSQL 18.6 vào báo cáo cuối.
