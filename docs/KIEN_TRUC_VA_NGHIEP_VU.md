# Kiến trúc và nghiệp vụ đã triển khai

Giữ điểm khởi chạy `com.quanlyphongtro.QuanLyPhongTroApplication`, parent Boot và Java 21 của dự án gốc. Tiếp tục HomeController và trang chủ hiện có; thêm các lớp theo thiết kế đã duyệt.

## Ánh xạ UC → code

| UC | Controller | Service chính | Dữ liệu |
|---|---|---|---|
| 01–03 | AuthController | AuthService, ResetMailService | Account, PasswordResetToken |
| 04–05 | PublicController, HomeController | RoomQueryService, ImageService | Room, Listing, Category, Amenity, RoomImage |
| 06/08 | TenantController | TenantService | Favorite, Review, Tenancy |
| 07/10/11 | TenantController, LandlordController | RentalService | RentalRequest, Tenancy, Room |
| 09/13 | LandlordController, AdminController | ListingService, StorageService | Room, Listing, RoomAmenity, RoomImage, ModerationEvent |
| 12/14/15 | AdminController | AdminService | Account, các bảng thống kê/danh mục |
| 16 | AdminController | TenantService | Review, ModerationEvent |

Controller kiểm tra binding/Bean Validation, nhận actor từ principal; Service kiểm tra role/ownership và transaction; Repository truy vấn DB. Read service ánh xạ DTO trong transaction để template không tự truy vấn LAZY; `open-in-view=false`. ErrorHandler xử lý lỗi nghiệp vụ, xung đột unique/version và upload quá lớn.

SecurityConfig: public route/auth, tenant/landlord/admin riêng, login form/email, logout POST, CSRF, session registry. ActiveAccountFilter loại phiên của tài khoản bị khóa; RateLimitFilter giới hạn login/forgot 10 thao tác/IP/phút. Đây là bộ giới hạn trong một JVM, chưa phải bộ rate limit phân tán.

## Những khác biệt nhỏ so với thiết kế logic

1. Favorite và RoomAmenity dùng PK `id` cùng BaseEntity, cộng UNIQUE cho cặp FK thay vì PK kép. Dễ dùng Spring Data/Lombok và version chung; vẫn bảo đảm không trùng quan hệ trên PostgreSQL.
2. Các entity đều có created_at, updated_at, version chung. Reviewed/processed actor FK đặt `reviewed_by_id`/`processed_by_id` theo naming JPA.
3. Rating lưu integer thay vì smallint, có CHECK 1..5. Khác biệt kiểu lưu không thay đổi nghiệp vụ.
4. Avatar vẫn là trường tùy chọn, chưa có UI upload. Ảnh phòng chỉ JPEG/PNG vì decoder Java đã dùng chưa hỗ trợ WebP được kiểm thử.
5. Dashboard đếm bản ghi được tạo trong khoảng ngày và phân nhóm theo trạng thái hiện tại. Không phải thống kê “số lần đổi trạng thái” hoặc doanh thu.
6. Hủy giữ không làm request ACCEPTED biến thành CANCELLED: request giữ lịch sử quyết định, tenancy ghi CANCELLED.
7. Ngày xác nhận nhận phòng phải từ ngày dự kiến trở đi và không ở tương lai. Ngày kết thúc không trước ngày nhận.

## Quy tắc quan trọng

- Public = chủ ACTIVE + room AVAILABLE + listing APPROVED + visible.
- Một request PENDING/người/bài và một tenancy PENDING_MOVE_IN hoặc ACTIVE/phòng. DB native bảo vệ bằng partial unique index; Service khóa room để tuần tự hóa.
- Truy vấn ID phòng từ request/tenancy là scalar trước khi lấy khóa; không preload trạng thái request trước khóa rồi dùng dữ liệu cũ.
- Chấp nhận tạo tenancy, đổi room và xử lý request trong một transaction. Request PENDING khác bị REJECTED vì phòng đã được giữ.
- Đổi nội dung/ẩn/chặn/bảo trì/lưu trữ hủy request đang chờ và ghi lý do. Tenancy giữ snapshot giá/địa chỉ.
- Chủ không sửa nội dung phòng RESERVED/OCCUPIED, không tự APPROVED hoặc bỏ BLOCKED. Phòng đang có hồ sơ giữ không được lưu trữ.
- Nội dung dùng `th:text`, không HTML người dùng. Ảnh chỉ đi qua endpoint kiểm tra quyền và cache no-store.
- Review gắn tenancy, chỉ sau ACTIVE/COMPLETED. Sửa nhận xét HIDDEN không tự hiện; DELETED không được tạo lại cho cùng hồ sơ.
- Reset token băm SHA-256, 15 phút, một lần. Password băm BCrypt.
- Lock tài khoản không xóa tenancy; mở khóa không tự bật lại bài. ADMIN không tự khóa mình và thao tác ADMIN được tuần tự hóa.

```mermaid
sequenceDiagram
    participant T as Người thuê
    participant S as RentalService
    participant D as PostgreSQL
    participant L as Chủ trọ
    T->>S: Gửi yêu cầu
    S->>D: Khóa room, kiểm tra công khai
    S->>D: Lưu PENDING và snapshot
    L->>S: Chấp nhận
    S->>D: Khóa room và kiểm tra lại
    S->>D: Tạo PENDING_MOVE_IN, room RESERVED
    S->>D: ACCEPTED; từ chối các yêu cầu khác
    L->>S: Xác nhận nhận phòng
    S->>D: Tenancy ACTIVE, room OCCUPIED
```

## Tham khảo kỹ thuật

- https://docs.spring.io/spring-boot/system-requirements.html
- https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html
- https://docs.spring.io/spring-boot/reference/using/build-systems.html
- https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html

Cấu trúc migration ở `src/main/resources/db/migration/V1__initial_schema.sql` khớp entity hiện tại. Migration đầu chỉ cho schema trống; không tự nhập cấu trúc từ ảnh cũ có Invoice/Contract vì đã loại phần đó khỏi phạm vi.
