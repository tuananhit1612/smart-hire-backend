# SmartHire Backend Development Rules (Spring Boot)

## 2. Cấu trúc Project
```text
smarthire-backend
src
    main
        java/com/smarthire
            core
            features
            shared
            infrastructure
        resources
            application.yml
            db/migration
test
```

## 3. Package Structure
```text
com.smarthire
├── core
│   ├── config (Security, CORS, Config chung)
│   ├── exception (Global exception handler)
│   ├── security (JWT, authentication)
│   └── utils (Utility functions)
├── features
│   ├── auth (controller, service, repository, entity, dto, mapper)
│   ├── jobs (controller, service, repository, entity, dto, mapper)
│   ├── cv (controller, service, repository, entity, dto, mapper)
│   └── ai (service, model, client)
├── shared
│   ├── dto
│   ├── enums
│   └── constants
└── infrastructure
    ├── database
    ├── storage
    └── external integrations
```

## 4. Kiến trúc Feature-Based
Project backend được tổ chức theo domain / feature.
Ví dụ: `auth`, `jobs`, `cv`, `ai`

**Quy tắc:**
- Không trộn logic giữa các feature.
- Mỗi feature có `controller`, `service`, `repository`, `entity`, `dto`.
- Feature giao tiếp với nhau thông qua `service`.

## 5. Trách nhiệm của từng Layer

### Controller Layer
**Nhiệm vụ:**
- Nhận HTTP request.
- Validate dữ liệu.
- Gọi Service.
- Trả response.
**Không được:**
- Viết business logic.
- Truy cập database trực tiếp.

### Service Layer
**Nhiệm vụ:**
- Xử lý business logic.
- Gọi Repository.
- Xử lý transaction.
**Không được:**
- Chứa HTTP logic.

### Repository Layer
**Nhiệm vụ:**
- Giao tiếp database.
- Sử dụng Spring Data JPA.

### Entity Layer
**Nhiệm vụ:**
- Mapping database table.

### DTO Layer
DTO dùng cho API Request / Response.
**Quy tắc:**
- Không return entity trực tiếp.
- Luôn dùng DTO.
Ví dụ: `JobRequest`, `JobResponse`, `LoginRequest`, `LoginResponse`.

## 6. Quy tắc đặt tên File
- **Controller:** `JobController`, `AuthController`
- **Service:** `JobService`, `JobServiceImpl`
- **Repository:** `JobRepository`
- **Entity:** `Job`, `User`
- **DTO:** `JobRequest`, `JobResponse`

## 7. Luồng xử lý Backend
Luồng chuẩn: `Controller` → `Service` → `Repository` → `Database`

## 8. Quy tắc Controller
Controller phải:
- Chỉ xử lý HTTP request.
- Gọi Service.
- Return `ResponseEntity`.

## 9. Quy tắc Service
Service:
- Chứa business logic.
- Controller gọi service.
- Service gọi repository.

## 10. Quy tắc Repository
Repository:
- Dùng Spring Data JPA.
- Không viết business logic.

## 11. Exception Handling
Tất cả lỗi phải xử lý tại: `core/exception/GlobalExceptionHandler`
**Mục đích:**
- API trả lỗi thống nhất.
- Tránh lộ stacktrace.

## 12. Git Workflow
Không commit trực tiếp vào `main`.
**Branch naming:**
- `feature/auth-api`
- `feature/jobs-api`
- `feature/cv-api`
- `feature/ai-api`

**Workflow:** Tạo branch → Code → Push → Pull Request → Code Review → Merge

## 13. Quy tắc Code Style
- **Class → PascalCase:** Ví dụ: `JobService`, `AuthController`
- **Variable → camelCase:** Ví dụ: `jobTitle`, `userEmail`
- **Constant → UPPER_CASE:** Ví dụ: `API_PREFIX`, `MAX_RETRY`

## 14. Clean Code Rules
- Một class = một responsibility.
- Method không quá 40 dòng.
- Tên biến phải rõ nghĩa.
- Không duplicate code.
- Logic dùng chung đặt trong `shared/utils`.

## 15. Quy tắc làm việc nhóm
Để tránh conflict, mỗi developer phụ trách một feature.
Ví dụ:
- Dev 1 → `auth`
- Dev 2 → `jobs`
- Dev 3 → `cv`
- Dev 4 → `ai`

**Quy tắc:**
- Không sửa code feature khác.
- Shared code phải review trước khi thay đổi.

## 16. Quy tắc API
API prefix: `/api/v1` (Khuyến khích có version) hoặc `/api`
Ví dụ:
- `POST /api/jobs`
- `GET /api/jobs`
- `GET /api/jobs/{id}`

## 17. Nguyên tắc thiết kế
Ưu tiên:
1. Clean Architecture
2. Separation of Concerns
3. Feature-based design
4. Reusable code
