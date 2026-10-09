# Kết quả kiểm thử bản bàn giao

Ngày kiểm tra: 09/10/2026. Bản này là mã nguồn triển khai từ ZIP dự án hiện có, không phải thay đổi trực tiếp trên MacBook.

## Môi trường đã chạy

- Java: Eclipse Temurin/OpenJDK 21.0.12.1.
- Maven: Wrapper 3.9.16, parent Spring Boot 4.1.1 giữ nguyên.
- Database kiểm thử ứng dụng: H2 2.4.240 độc lập, in-memory.
- Browser: Chromium Headless 151, Playwright; viewport 1440×1000 và 375×812.
- Migration: kiểm tra cú pháp bằng parser PostgreSQL `pglast`, 41 câu lệnh được chấp nhận. Chưa thực thi trên server PostgreSQL.

## Maven

Lệnh kiểm tra cuối: `./mvnw verify` với JDK 21. Runtime này dùng cấu hình proxy/truststore riêng để tải dependencies; các cấu hình đó không đưa vào dự án.

| Lớp | Số test | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| QuanLyPhongTroApplicationTests | 1 | 0 | 0 | 0 |
| BusinessRulesTest | 3 | 0 | 0 | 0 |
| ApplicationFlowTest | 12 | 0 | 0 | 0 |
| **Tổng bộ mặc định** | **16** | **0** | **0** | **0** |

Kết quả: BUILD SUCCESS, JAR đã được tạo thực tế. Gói source không kèm JAR/target để giữ nhỏ và tránh đóng gói tài nguyên môi trường kiểm thử. Maven trên máy người dùng sẽ tạo lại JAR.

Các ca nghiệp vụ gồm: đăng ký chuẩn hóa email và chặn ADMIN; CSRF/role; chủ và người thuê khác không thao tác đối tượng; lifecycle thuê/snapshot; sửa bài hủy request và quay về kiểm duyệt; hai luồng đồng thời chấp nhận chỉ một thắng; favorite idempotent và không lộ bài/ảnh ẩn; token reset một lần/hết hạn; khóa tài khoản hủy pending và loại principal; review ẩn không tự hiện khi sửa; file giả ảnh/bộ lọc sai; render các trang của từng vai trò.

Unit test bổ sung: giới hạn mật khẩu tính byte UTF-8, CSV escape chống công thức, lý do từ chối bắt buộc. Context test gốc được giữ và tách sang profile test.

## Browser chạy thực tế

Ứng dụng được khởi động bằng JDK 21 với database H2 riêng và seed demo. H2 được thêm từ classpath phục vụ kiểm tra; không thay đổi database vận hành sang H2. Profile, secret preview và script môi trường đó không đóng gói trong source.

Đã kiểm tra đăng nhập thật ba vai trò, các trang công khai/chủ/người thuê/quản trị, ảnh được tải qua endpoint, trang form, danh sách, dashboard và responsive. Luồng browser E2E: người thuê gửi yêu cầu → chủ chấp nhận → xác nhận nhận phòng → người thuê ghi đánh giá.

Kiểm tra lỗi JavaScript và tràn ngang ở các trang được duyệt không phát hiện lỗi trong lần chạy cuối. Ảnh chụp giao diện trong `screenshots` là từ ứng dụng chạy, đã kiểm tra trực quan trang chủ desktop/mobile. Script chụp ảnh đợi seed hoàn tất và tải ảnh lazy trước snapshot; không đưa ảnh mockup vào làm bằng chứng ứng dụng.

## Lỗi đã phát hiện và sửa

- Thymeleaf `param.expired or param.blocked` không chuyển null thành boolean được: sửa sang kiểm tra `!= null`, chạy lại bộ test thành công.
- Favorite có bài không còn công khai: tách `th:if` ra ngoài `th:replace` để không render card với đối tượng null.
- Đọc mutable request trước khi khóa có thể dùng snapshot cũ của persistence context: lấy scalar roomId trước, lấy khóa rồi mới đọc request/tenancy.
- Môi trường browser test từng mở chi tiết trước khi seed commit: sửa điều kiện chờ dữ liệu demo trước chạy browser và chụp ảnh.

## Những phần chưa xác minh

| Phần | Trạng thái thực tế | Bước tiếp theo |
|---|---|---|
| Migration PostgreSQL 18.6 | Chỉ qua parser, chưa thực thi | Chạy native test trên DB `_test` riêng |
| Partial unique/FK/CHECK native | Có SQL và PostgresConstraintsIT, chưa chạy | `./mvnw -Dintegration.profile=postgres-test -Dtest=PostgresConstraintsIT test` |
| Khóa/transaction trên PostgreSQL | Test đồng thời đã chạy H2 | Chạy cùng luồng trên native PostgreSQL |
| IntelliJ/PostgreSQL trên MacBook | Không truy cập trực tiếp | Mở bản làm việc riêng theo README |
| SMTP thật | Chưa gửi mail thực tế | Cấu hình account SMTP được phép và kiểm tra giao nhận |
| Tải lớn/pentest/triển khai HTTPS | Chưa chạy | Ngoài bản kiểm tra hiện tại |

`PostgresConstraintsIT` là bộ opt-in riêng, không nằm trong 16 test mặc định và **không được tính là đã pass**. Test native bắt buộc URL database `_test` riêng, chạy migration và tạo fixture; không dùng dữ liệu thật. Kiểm tra native cần hoàn thành trước khi kết luận hệ thống tương thích đầy đủ với PostgreSQL trên máy người dùng.

Giới hạn môi trường: PostgreSQL server cần user chạy phù hợp; môi trường kiểm tra này không hỗ trợ tạo/chuyển sang user đó. Không thay đổi mật khẩu hoặc cấu hình PostgreSQL trên MacBook để khắc phục giới hạn môi trường.
