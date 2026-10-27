# QuanLyPhongTro — Trọ Việt

Ứng dụng quản lý và cho thuê phòng trọ trực tuyến, tiếp tục từ dự án Spring Boot hiện có. Java 21 · Spring Boot 4.1.1 · Maven Wrapper · PostgreSQL · Spring MVC/JPA/Security · Thymeleaf.

## 1. Tình trạng bản bàn giao

Đã xây dựng các luồng UC01–UC16: tài khoản, hồ sơ, tìm kiếm/chi tiết, yêu thích, yêu cầu thuê, bài đăng/kiểm duyệt, hồ sơ thuê, đánh giá, tài khoản quản trị, danh mục và thống kê CSV. Source được biên dịch và `verify` trên JDK 21; kiểm thử nghiệp vụ và browser dùng H2 độc lập. **Chưa chạy migration hoặc integration test trên PostgreSQL 18.6 thật**. Xem `docs/KET_QUA_KIEM_THU.md` để phân biệt phần đã chạy và phần còn cần kiểm tra.

H2 chỉ có scope test, không phải database vận hành và không được đóng gói trong JAR. Không tuyên bố bộ test H2 thay thế các partial unique index hoặc hành vi khóa của PostgreSQL.

## 2. Mở dự án trên MacBook

1. Giải nén ZIP vào một thư mục mới, ví dụ `~/Downloads/QuanLyPhongTro_Work`. Không giải nén đè lên `~/IdeaProjects/QuanLyPhongTro` đang dùng.
2. Trong IntelliJ: File → Open → chọn thư mục `QuanLyPhongTro` vừa giải nén hoặc `pom.xml` bên trong. Chờ Maven tải dependencies.
3. Project SDK, Maven runner JRE và Run Configuration cùng chọn **JDK 21**. Package vẫn là `com.quanlyphongtro`.
4. Mở Terminal tại thư mục chứa `pom.xml` rồi chạy:

```bash
chmod +x mvnw
./mvnw verify
```

Bộ test mặc định chạy H2 in-memory, không kết nối database PostgreSQL trên máy. Cần Internet lần đầu để tải Maven/dependencies. Giữ bản dự án gốc để đối chiếu. Không cần tự đổi parent Boot hoặc nâng toàn bộ dependency.

## 3. Chạy demo PostgreSQL riêng

Database hiện tại `quanlyphongtro` chưa được đọc schema ở môi trường phát triển này. Vì vậy **chạy demo bằng database mới `quanlyphongtro_demo`**, không sửa database cũ.

Chỉ khi bạn đã chọn tạo database demo, thực hiện trong Terminal có PostgreSQL CLI:

```bash
createdb -h localhost -U postgres quanlyphongtro_demo
```

Lệnh này tạo DB mới, không xóa/đổi mật khẩu DB cũ. Nếu tên đã tồn tại, kiểm tra nội dung trước; không drop hoặc recreate. Có thể tạo DB riêng bằng pgAdmin thay vì CLI.

Trong IntelliJ → Run → Edit Configurations → chọn `QuanLyPhongTroApplication` → Environment variables, điền:

| Biến | Giá trị cần cấu hình |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `demo` |
| `DEMO_DB_URL` | `jdbc:postgresql://localhost:5432/quanlyphongtro_demo` |
| `DB_USERNAME` | `postgres` hoặc tài khoản app đã được cấp |
| `DB_PASSWORD` | Mật khẩu PostgreSQL thật, chỉ đặt ở cấu hình local |
| `DEMO_SEED` | `true` cho lần tạo dữ liệu demo |
| `DEMO_ADMIN_PASSWORD` | Mật khẩu demo quản trị do bạn chọn |
| `DEMO_USER_PASSWORD` | Mật khẩu demo các tài khoản còn lại do bạn chọn |
| `UPLOAD_DIR` | Thư mục local có quyền ghi, ví dụ `/Users/hiep/QuanLyPhongTroUploads` |
| `APP_BASE_URL` | `http://localhost:8080` |

Hai mật khẩu demo cần 10–64 ký tự, không quá 72 byte UTF-8. Không dùng lại mật khẩu PostgreSQL. Không đặt giá trị thật vào source, `.env.example`, Git hoặc ZIP gửi người khác.

Nhấn Run, mở http://localhost:8080. Profile demo bật Flyway và chỉ cho chạy migration trên tên database kết thúc `_demo`. Flyway không clean hoặc tự baseline DB đã có dữ liệu. Seeder chỉ chạy khi bật cờ và bảng accounts đang trống; lần chạy lại không đổi mật khẩu đã có. Sau khởi tạo có thể đặt `DEMO_SEED=false`.

Nếu chạy Terminal, đặt các biến môi trường bằng cách local phù hợp rồi dùng:

```bash
./mvnw spring-boot:run
```

Không tự đổi cấu hình toàn hệ thống Java/PostgreSQL. Trong IntelliJ hãy cấu hình JDK cho riêng dự án này.

### Tài khoản demo

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Quản trị viên | `admin@tro.demo` | Giá trị `DEMO_ADMIN_PASSWORD` bạn đã chọn lúc seed |
| Chủ trọ | `chutro1@tro.demo`, `chutro2@tro.demo` | Giá trị `DEMO_USER_PASSWORD` lúc seed |
| Người thuê | `nguoithue1@tro.demo` đến `nguoithue5@tro.demo` | Giá trị `DEMO_USER_PASSWORD` lúc seed |

Seed có 14 phòng, 3 loại phòng, 6 tiện ích, một yêu cầu đang chờ, hồ sơ giữ phòng/đang thuê/đã kết thúc và một đánh giá hợp lệ. Địa chỉ, danh tính và ảnh đều minh họa; tọa độ để trống vì không giả định vị trí thật. Ảnh minh họa được tạo bằng Java và lưu vào `UPLOAD_DIR`, không lấy ảnh cá nhân.

## 4. Các quy trình sử dụng

- **Khách/người thuê:** tìm phòng → xem chi phí, ảnh, tiện ích → đăng ký TENANT → lưu yêu thích/gửi yêu cầu → xem phản hồi. Khi chủ xác nhận nhận phòng, có thể đánh giá tại Hồ sơ thuê.
- **Chủ trọ:** thêm phòng → thêm ảnh JPEG/PNG → gửi duyệt → quản trị duyệt → chủ chọn hiện bài → xử lý yêu cầu → xác nhận nhận phòng → kết thúc thuê. Sau kết thúc, bài được ẩn để cập nhật trước khi mở lại.
- **Quản trị:** quản lý tài khoản, duyệt/từ chối/chặn bài có lý do, quản lý danh mục, ẩn/khôi phục đánh giá, thống kê theo khoảng ngày và xuất CSV.
- **Hủy thuê:** TENANT hủy khi yêu cầu còn PENDING. Sau ACCEPTED, chủ hủy giữ phòng trong Hồ sơ thuê nếu người thuê chưa nhận phòng. ACTIVE dùng quy trình kết thúc thuê, không hủy giữ.
- **Sửa/xóa:** sửa nội dung bài đã duyệt cần duyệt lại và hủy yêu cầu đang chờ với lý do. Xóa phòng trên phạm vi bản này là lưu trữ, không xóa lịch sử thuê.

## 5. Khôi phục mật khẩu

Profile demo dùng hộp thư local: gửi từ trang Quên mật khẩu rồi người trình diễn mở file mới trong `UPLOAD_DIR/mailbox` để đọc link. **Thư mục này không có endpoint web**, không nằm trong static và không được commit. Token hết hạn sau 15 phút, một lần; DB chỉ giữ hash. Đây là cách demo khi không có dịch vụ mail, không phải tính năng cho khách xem token.

Để dùng email thật, cấu hình `MAIL_MODE=smtp`, `MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_TLS` và `APP_BASE_URL` của ứng dụng. Không thay đổi SMTP/account email nếu chưa chủ động thiết lập. Nếu mail gửi thất bại, trang vẫn phản hồi chung, log không ghi token/password. SMTP thật chưa được kiểm thử trong bản bàn giao.

## 6. Chatbot AI

Chatbot ở góc phải các trang: khách hoặc người dùng đăng nhập có thể hỏi cách dùng và tìm phòng bằng tiếng Việt. Tạo API key tại [OpenAI Platform](https://platform.openai.com/api-keys), rồi đặt `OPENAI_API_KEY` vào biến môi trường của Run Configuration hoặc shell để bật. Không nhập khóa vào `.env.example`, Git hoặc trình duyệt. Không cấu hình khóa thì trang vẫn chạy, chatbot báo chưa sẵn sàng. Chatbot dùng OpenAI Responses API với model `gpt-4o-mini`; mỗi câu hỏi gọi API và có phát sinh chi phí. Gói ChatGPT và tín dụng API tính riêng.

Chatbot chỉ đọc tối đa **12 bài công khai mới nhất** (đã duyệt, đang hiện, phòng còn trống, chủ trọ hoạt động). Vì giới hạn này, gợi ý có thể không bao quát; dùng bộ lọc `/rooms` để tìm thêm. Hệ thống không gửi số điện thoại/tên chủ trọ hay hồ sơ thuê lên OpenAI. Câu hỏi và tối đa 8 lượt trò chuyện gần nhất được gửi từ tab trình duyệt tới server/OpenAI, không lưu vào DB; request đặt `store=false`. Đừng nhập mật khẩu hoặc dữ liệu nhạy cảm. Link phòng được server đối chiếu lại với danh sách công khai trước khi hiển thị. Gợi ý AI có thể sai, cần xem lại trang chi tiết và hỏi chủ trọ trước khi quyết định.

## 7. Kiểm thử PostgreSQL thật

Tạo một database test **riêng và mới**, ví dụ `quanlyphongtro_test`, bằng tài khoản đã có quyền. Cấu hình các biến `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` tại môi trường Terminal/Run Configuration riêng. URL phải có tên DB kết thúc `_test`; test resolver từ chối tên khác.

```bash
./mvnw -Dintegration.profile=postgres-test -Dtest=PostgresConstraintsIT test
```

Lệnh này chạy migration và các test kế thừa luồng nghiệp vụ cộng kiểm tra chỉ mục/ràng buộc native. Không có drop/truncate trong bộ native, nhưng có tạo tài khoản/phòng và ghi dữ liệu fixture; cần quyền tạo bảng/index. Test H2 mặc định có `create-drop` chỉ trên DB in-memory.

Nếu test PostgreSQL thất bại, giữ log `target/surefire-reports`, không tự bật baseline hoặc sửa checksum để bỏ qua lỗi. Những kiểm tra này chưa được chạy trong môi trường bàn giao; kết quả trên máy bạn mới xác nhận tính tương thích PostgreSQL 18.6.

## 8. Cấu hình an toàn và giới hạn

- Profile mặc định: Flyway tắt, Hibernate `validate`. Muốn dùng DB hiện tại phải xem schema, sao lưu và thiết kế migration phù hợp trước. Không dùng `ddl-auto=update/create/create-drop` trên DB thật.
- Profile demo/test được tách biệt. H2 và các biến preview của môi trường kiểm thử không được đóng gói làm cấu hình vận hành.
- Ảnh tối đa 10/phòng, 5 MB/ảnh, 20 triệu pixel; chấp nhận JPEG/PNG, chuyển thành PNG; ảnh bài ẩn/chặn chỉ chủ sở hữu và ADMIN xem được.
- Password BCrypt, login session, CSRF, kiểm tra ownership, kiểm tra account bị khóa mỗi request; không dùng JWT vì đây là website server-render.
- Bản đầu không thu CCCD/ngân hàng, không ký hợp đồng điện tử, không tạo hóa đơn/thu tiền. Thống kê là hoạt động hệ thống, không phải doanh thu thực thu.
- Quy trình thuê một phòng vật lý có một người đại diện, không quản lý nhiều hợp đồng đang hoạt động hoặc nhiều đợt đặt trước cùng lúc.
- Email đăng nhập và role chưa cho tự đổi. Avatar là trường mở rộng chưa có màn upload. Bản đồ là liên kết mở bằng tọa độ tùy chọn.
- Kiểm thử browser hiện đã kiểm tra các luồng chính, không phải pentest hoặc kiểm thử tải. Quản lý danh sách dùng cách truy vấn đơn giản phù hợp quy mô đồ án; tối ưu khi dữ liệu lớn là bước sau.

## 9. Tài liệu trong gói

- `docs/THIET_KE_DA_DUYET.md`: thiết kế Giai đoạn 1.
- `docs/KIEN_TRUC_VA_NGHIEP_VU.md`: ánh xạ code và các điều chỉnh triển khai.
- `docs/KET_QUA_KIEM_THU.md`: bằng chứng build/test và giới hạn.
- `docs/KICH_BAN_BAO_VE.md`: thao tác trình diễn và câu hỏi kiến trúc.
- `docs/screenshots/`: ảnh giao diện chạy thực tế ở môi trường kiểm thử, gồm desktop/mobile và các vai trò.

Source không chứa mật khẩu PostgreSQL, SMTP hoặc mật khẩu demo cố định. Bản ZIP gốc được giữ nguyên; gói bàn giao này là bản làm việc riêng.
