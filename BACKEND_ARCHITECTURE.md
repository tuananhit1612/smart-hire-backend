# KẾT CẤU & KIẾN TRÚC MÃ NGUỒN SMARTHIRE BACKEND

Tài liệu này đóng vai trò như một bản đồ (map) giúp bạn dễ dàng tra cứu xem "một chức năng nào đó nằm ở đâu, code viết như thế nào, logic luân chuyển ra sao" trong dự án.

## 1. TỔNG QUAN KIẾN TRÚC (Package-by-Feature)
Dự án sử dụng Java Spring Boot và được tổ chức theo kiến trúc **Package-by-Feature** (chia thư mục theo tính năng nghiệp vụ) thay vì Layer (chia theo tầng kỹ thuật như controller, service chung chung).

Toàn bộ mã nguồn cốt lõi nằm tại:
`src/main/java/com/smarthire/backend/`

Nó được chia thành 4 phân khu chính:
1. `core`: Cấu hình hệ thống, bảo mật (Security), xử lý lỗi toàn cục.
2. `features` **(Quan trọng nhất)**: Nơi chứa toàn bộ logic, các chức năng nghiệp vụ của ứng dụng.
3. `infrastructure`: Giao tiếp với dịch vụ bên ngoài (AI Gemini, Storage upload file, Database config).
4. `shared`: Các class dùng chung (Enums, Constants, DTO chung).

---

## 2. BẢN ĐỒ CÁC CHỨC NĂNG (`features/`)

Bất kỳ khi nào bạn cần tìm code của một chức năng nghiệp vụ, hãy vào ngay thư mục `features/`. Dưới đây là danh sách các module và ý nghĩa của chúng:

| Thư mục chức năng | Chức năng chính | Ghi chú & Logic cơ bản |
| :--- | :--- | :--- |
| **`auth`** | Xác thực & Phân quyền | Quy trình Đăng ký, Đăng nhập, cấp phát JWT và Làm mới Token (Refresh Token). |
| **`candidate`** | Hồ sơ ứng viên | Quản lý Profile của ứng viên: Cập nhật thông tin cá nhân, Học vấn, Kinh nghiệm, Kỹ năng và upload file CV. |
| **`company`** | Thông tin Công ty | Quản lý thông tin nhà tuyển dụng. Tạo profile công ty, chỉnh sửa thông tin, upload logo. |
| **`job`** | Tin tuyển dụng (Nhà tuyển dụng) | HR tạo Job mới, cập nhật Job, đổi trạng thái (Open/Closed). Nơi chứa logic lọc/tìm kiếm Job public cho Candidate. |
| **`application`** | Đơn ứng tuyển (Quy trình tuyển dụng) | Logic quan trọng: Candidate nộp CV vào Job -> Tạo Application. HR sẽ đổi trạng thái (Applied -> Interview -> Hired / Rejected). |
| **`interview`** | Lịch phỏng vấn | HR xếp lịch phỏng vấn cho ứng viên. Chứa thông tin link Google Meet, thời gian, người phỏng vấn, đánh giá kết quả. |
| **`dashboard`** | Thống kê dữ liệu | Tính toán các con số tổng quan cho Admin/HR (Tổng số Job, Tỉ lệ chuyển đổi ứng viên, Số lượng CV nộp trong tháng). |
| **`cv`** | Thông tin CV chi tiết | Trích xuất, phân tích và quản lý dữ liệu chuyên sâu từ CV. |
| **`notification`** | Hệ thống thông báo | Logic đẩy thông báo cho User (Realtime qua Websocket hoặc Email). |
| **`onboarding`** | Luồng thiết lập ban đầu | Các bước bắt buộc lúc user mới tạo tài khoản phải nhập (Bổ sung profile, chọn role, cập nhật thông tin công ty lần đầu). |
| **`report`** | Báo cáo | Xóa xuất báo cáo thống kê định kỳ ra file (PDF/Excel) nếu có. |

---

## 3. CẤU TRÚC CODE BÊN TRONG MỘT CHỨC NĂNG (Mô hình MVC linh hoạt)

Khi bạn mở bất kỳ một module chức năng nào trong `features/` (Ví dụ: `features/job`), bạn sẽ luôn luôn thấy 5 thư mục con chuẩn mực sau:

### 1. `controller/` (Cửa ngõ giao tiếp)
- **Nhiệm vụ**: Chứa các file `*Controller.java` (VD: `JobController.java`). Là nơi tiếp nhận HTTP Request (`GET`, `POST`, `PUT`, `DELETE`) từ Frontend (React/NextJS).
- **Logic**: Kiểm tra quyền truy cập sơ bộ, xác thực DTO đầu vào (Validation framework), gọi `Service` để xử lý, và trả về JSON kết quả. Nó **KHÔNG NÊN** chứa logic bussiness phức tạp tính toán ở đây.

### 2. `service/` (Khối óc tính toán)
- **Nhiệm vụ**: Chứa class interface định nghĩa chức năng và `*ServiceImpl.java` thực thi logic.
- **Logic**: Đây là **nơi chứa mọi logic phức tạp nhất**. Ví dụ: Khi Nộp CV, `ApplicationService` sẽ (1) Kiểm tra Job còn mở không -> (2) Kiểm tra User đã nộp trước đó chưa -> (3) Gọi AI phân tích CV (nếu cấu hình) -> (4) Lưu vào Database thông qua `Repository` -> (5) Gửi email thông báo cho HR. Mọi xử lý đều nằm ở đây.

### 3. `repository/` (Giao tiếp Database)
- **Nhiệm vụ**: Các interface kế thừa `JpaRepository`.
- **Logic**: Nơi định nghĩa các câu query (truy vấn) xuống cơ sở dữ liệu MySQL mà không cần tự viết SQL thủ công.
  *(VD: `List<Job> findByStatusAndCompanyId(JobStatus status, Long companyId);`)*

### 4. `entity/` (Bản đồ CSDL)
- **Nhiệm vụ**: Chứa các class Java ánh xạ trực tiếp với bảng trong Database.
- **Logic**: Các cột trong SQL đều được khai báo thành thuộc tính ở đây. Định nghĩa cấu trúc, khóa chính, Foreign Key, liên kết bảng (OneToMany, ManyToOne).

### 5. `dto/` (Dữ liệu truyền tải)
- **Nhiệm vụ**: Chứa các class Payload (Request) nhận từ Client và Response (trả về Client).
- **Logic**: Tách biệt rõ ràng Model Entity trong Database và dữ liệu lộ ra cho Frontend, giúp giấu các thông tin nhạy cảm và tối ưu hóa payload truyền tải.

---

## 4. CÁC THÀNH PHẦN KHÁC (Core, Infrastructure, Shared)

### 📌 `core/`
- **`config/`**: Nạp dữ liệu giả (`DatabaseSeeder`), cấu hình Socket, WebSocket, Swagger (Tài liệu API).
- **`security/`**: Xử lý Token (JWT). `JwtAuthenticationFilter` đánh chặn mọi Request để đảm bảo User có Token mới được vào, trích xuất thông tin định danh (Role, Email) cho hệ thống sử dụng. 
- **`exception/`**: `GlobalExceptionHandler` - bắt tất cả lỗi quăng ra từ Service/Controller và gói thành Error JSON chuẩn trả cho Frontend.

### 📌 `infrastructure/` 
- **`ai/`**: Nơi tích hợp trí tuệ nhân tạo. Gọi Google Gemini SDK để phân tích đánh giá tự động độ khớp của CV so với Job Description.
- **`storage/`**: Chứa logic Upload file (CV PDF/Word, Logo công ty) chuẩn hóa, sinh file name, trả về URL.

### 📌 `shared/`
- Chứa các class cực kỳ quan trọng dùng hàng ngày: **Enums** (`Role`, `ApplicationStage`, `JobStatus`), **Constants** (Các hằng số thông báo lỗi), và cấu trúc thư đóng gói API (`ApiResponse<T>`).

---

## TỔNG KẾT QUY TRÌNH TÌM KIẾM NHANH CODE:
1. Bạn thấy một luồng trên UI (Ví dụ "Nhà tuyển dụng bấm Đăng Tin job mới").
2. Chức năng này thuộc về Job -> Vào thư mục `features/job/`.
3. Bạn muốn biết file nào hứng API đó -> Vào `controller/JobController.java` tìm @PostMapping.
4. Bạn muốn tìm cách mà dữ liệu tính toán và lưu -> Vào `service/JobServiceImpl.java`.
5. Nếu luồng đó có gọi hệ thống ngoài (Lưu ảnh logo/gọi AI) -> Nhìn vào `infrastructure/`.
