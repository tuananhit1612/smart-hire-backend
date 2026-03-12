# SmartHire Backend – Development Rules (Spring Boot)

> Tài liệu này là **quy tắc bắt buộc** cho toàn bộ team khi phát triển SmartHire Backend.
> Mọi thành viên phải đọc, hiểu và tuân thủ trước khi bắt đầu code.

---

## Mục lục

1. [Cấu trúc Project](#1-cấu-trúc-project)
2. [Package Structure](#2-package-structure)
3. [Kiến trúc Feature-Based](#3-kiến-trúc-feature-based)
4. [Trách nhiệm từng Layer](#4-trách-nhiệm-từng-layer)
5. [Quy tắc đặt tên File & Class](#5-quy-tắc-đặt-tên-file--class)
6. [Luồng xử lý Backend](#6-luồng-xử-lý-backend)
7. [Quy tắc Controller](#7-quy-tắc-controller)
8. [Quy tắc Service](#8-quy-tắc-service)
9. [Quy tắc Repository](#9-quy-tắc-repository)
10. [Exception Handling](#10-exception-handling)
11. [Git Workflow](#11-git-workflow)
12. [Code Style](#12-code-style)
13. [Clean Code Rules](#13-clean-code-rules)
14. [Quy tắc làm việc nhóm](#14-quy-tắc-làm-việc-nhóm)
15. [Quy tắc API](#15-quy-tắc-api)
16. [Nguyên tắc thiết kế](#16-nguyên-tắc-thiết-kế)

---

## 1. Cấu trúc Project

```
smarthire-backend/
├── src/
│   ├── main/
│   │   ├── java/com/smarthire/
│   │   │   ├── core/
│   │   │   │   ├── config/          # Security, CORS, Config chung
│   │   │   │   ├── exception/       # Global exception handler
│   │   │   │   ├── security/        # JWT, authentication
│   │   │   │   └── utils/           # Utility functions
│   │   │   │
│   │   │   ├── features/
│   │   │   │   ├── auth/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── mapper/
│   │   │   │   │
│   │   │   │   ├── jobs/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── mapper/
│   │   │   │   │
│   │   │   │   ├── cv/
│   │   │   │   │   ├── controller/
│   │   │   │   │   ├── service/
│   │   │   │   │   ├── repository/
│   │   │   │   │   ├── entity/
│   │   │   │   │   ├── dto/
│   │   │   │   │   └── mapper/
│   │   │   │   │
│   │   │   │   └── ai/
│   │   │   │       ├── service/
│   │   │   │       ├── model/
│   │   │   │       └── client/
│   │   │   │
│   │   │   ├── shared/
│   │   │   │   ├── dto/
│   │   │   │   ├── enums/
│   │   │   │   └── constants/
│   │   │   │
│   │   │   └── infrastructure/
│   │   │       ├── database/
│   │   │       ├── storage/
│   │   │       └── external/        # External integrations
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/        # Flyway migrations
│   │
│   └── test/
│       └── java/com/smarthire/      # Mirror structure của main
│
└── pom.xml
```

---

## 2. Package Structure

```
com.smarthire
│
├── core
│   ├── config          → SecurityConfig, CorsConfig, AppConfig
│   ├── exception       → GlobalExceptionHandler, CustomException
│   ├── security        → JwtFilter, JwtUtil, UserDetailsServiceImpl
│   └── utils           → DateUtils, StringUtils, ResponseUtils
│
├── features
│   ├── auth            → Đăng nhập, đăng ký, refresh token
│   ├── jobs            → Quản lý tin tuyển dụng
│   ├── cv              → Hồ sơ ứng viên, CV upload/builder
│   └── ai              → AI matching, CV parsing, interview
│
├── shared
│   ├── dto             → ApiResponse, PageResponse, ErrorResponse
│   ├── enums           → Role, ApplicationStatus, JobStatus...
│   └── constants       → AppConstants, ApiPaths, ErrorCodes
│
└── infrastructure
    ├── database        → DataSource config, multi-tenant (nếu có)
    ├── storage         → S3/MinIO file storage
    └── external        → Email, SMS, AI API clients
```

---

## 3. Kiến trúc Feature-Based

Project backend được tổ chức theo **domain / feature**. Mỗi feature là một module độc lập.

### Các feature chính

| Feature | Developer phụ trách | Mô tả                           |
| ------- | ------------------- | ------------------------------- |
| `auth`  | Dev 1               | Đăng nhập, đăng ký, JWT         |
| `jobs`  | Dev 2               | Tin tuyển dụng, pipeline        |
| `cv`    | Dev 3               | Hồ sơ, CV upload/builder        |
| `ai`    | Dev 4               | AI matching, parsing, interview |

### Quy tắc

- **KHÔNG** trộn logic giữa các feature.
- Mỗi feature có đầy đủ: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`.
- Feature giao tiếp với nhau **chỉ thông qua Service** — không inject Repository của feature khác.
- Dữ liệu dùng chung → đặt vào `shared/`.

---

## 4. Trách nhiệm từng Layer

### 4.1 Controller Layer

**Được phép:**

- Nhận HTTP request
- Validate input (dùng `@Valid`, `@Validated`)
- Gọi Service
- Return `ResponseEntity<ApiResponse<T>>`

**Không được phép:**

- Viết business logic
- Truy cập database trực tiếp
- Gọi Repository trực tiếp
- Xử lý exception thủ công (để `GlobalExceptionHandler` xử lý)

```java
// ĐÚNG
@PostMapping
public ResponseEntity<ApiResponse<JobResponse>> createJob(
        @Valid @RequestBody JobRequest request) {
    JobResponse response = jobService.createJob(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}

// SAI – viết logic trong controller
@PostMapping
public ResponseEntity<?> createJob(@RequestBody JobRequest request) {
    if (request.getTitle() == null) { ... }   // ❌ validate thủ công
    jobRepository.save(...);                   // ❌ gọi repository trực tiếp
}
```

---

### 4.2 Service Layer

**Được phép:**

- Xử lý business logic
- Gọi Repository
- Gọi Service khác (cùng hoặc khác feature)
- Xử lý transaction (`@Transactional`)
- Throw custom exception

**Không được phép:**

- Chứa HTTP logic (`HttpServletRequest`, `HttpServletResponse`)
- Return `ResponseEntity`
- Gọi Controller

```java
// ĐÚNG
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Override
    @Transactional
    public JobResponse createJob(JobRequest request) {
        Job job = jobMapper.toEntity(request);
        Job saved = jobRepository.save(job);
        return jobMapper.toResponse(saved);
    }
}
```

---

### 4.3 Repository Layer

**Được phép:**

- Giao tiếp với database
- Sử dụng Spring Data JPA
- Viết JPQL / Native Query khi cần

**Không được phép:**

- Viết business logic
- Gọi Service khác

```java
// ĐÚNG
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatusAndCompanyId(JobStatus status, Long companyId);

    @Query("SELECT j FROM Job j WHERE j.title LIKE %:keyword%")
    Page<Job> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
}
```

---

### 4.4 Entity Layer

- Mapping 1-1 với database table
- Dùng `@Entity`, `@Table`, `@Column`
- Không chứa business logic
- Dùng `BaseEntity` cho các field chung (`createdAt`, `updatedAt`)

```java
@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
```

---

### 4.5 DTO Layer

DTO dùng cho **API Request / Response** — không dùng Entity trực tiếp.

**Quy tắc:**

- Không return Entity trực tiếp ra ngoài
- Request DTO: dùng `@Valid` annotation để validate
- Response DTO: chỉ chứa field cần thiết

```
JobRequest   → dữ liệu từ client gửi lên
JobResponse  → dữ liệu trả về cho client
LoginRequest
LoginResponse
```

---

### 4.6 Mapper Layer

- Dùng **MapStruct** để map Entity ↔ DTO
- Không viết mapping thủ công bằng setter

```java
@Mapper(componentModel = "spring")
public interface JobMapper {
    JobResponse toResponse(Job job);
    Job toEntity(JobRequest request);
    List<JobResponse> toResponseList(List<Job> jobs);
}
```

---

## 5. Quy tắc đặt tên File & Class

| Loại                | Convention          | Ví dụ                             |
| ------------------- | ------------------- | --------------------------------- |
| Controller          | `{Name}Controller`  | `JobController`, `AuthController` |
| Service (interface) | `{Name}Service`     | `JobService`, `AuthService`       |
| Service (impl)      | `{Name}ServiceImpl` | `JobServiceImpl`                  |
| Repository          | `{Name}Repository`  | `JobRepository`                   |
| Entity              | `{Name}` (danh từ)  | `Job`, `User`, `Company`          |
| Request DTO         | `{Name}Request`     | `JobRequest`, `LoginRequest`      |
| Response DTO        | `{Name}Response`    | `JobResponse`, `LoginResponse`    |
| Mapper              | `{Name}Mapper`      | `JobMapper`, `UserMapper`         |
| Enum                | `{Name}` (UPPER)    | `JobStatus`, `UserRole`           |
| Exception           | `{Name}Exception`   | `ResourceNotFoundException`       |

---

## 6. Luồng xử lý Backend

```
Client (HTTP Request)
        ↓
   Controller          ← validate input, map to DTO
        ↓
    Service            ← business logic, transaction
        ↓
  Repository           ← query database
        ↓
   Database (MySQL)
        ↓
  Repository           ← map to Entity
        ↓
    Service            ← map to Response DTO
        ↓
   Controller          ← wrap ApiResponse
        ↓
Client (HTTP Response)
```

---

## 7. Quy tắc Controller

```java
@RestController
@RequestMapping(ApiPaths.JOBS)
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job management APIs")
public class JobController {

    private final JobService jobService;

    // GET /api/jobs
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getAllJobs(page, size)));
    }

    // GET /api/jobs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobById(id)));
    }

    // POST /api/jobs
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(jobService.createJob(request)));
    }

    // PUT /api/jobs/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(ApiResponse.success(jobService.updateJob(id, request)));
    }

    // DELETE /api/jobs/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
```

---

## 8. Quy tắc Service

```java
// Interface
public interface JobService {
    Page<JobResponse> getAllJobs(int page, int size);
    JobResponse getJobById(Long id);
    JobResponse createJob(JobRequest request);
    JobResponse updateJob(Long id, JobRequest request);
    void deleteJob(Long id);
}

// Implementation
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobMapper      jobMapper;

    @Override
    public Page<JobResponse> getAllJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return jobRepository.findAll(pageable).map(jobMapper::toResponse);
    }

    @Override
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
        return jobMapper.toResponse(job);
    }

    @Override
    @Transactional                       // ghi dữ liệu → override readOnly
    public JobResponse createJob(JobRequest request) {
        Job job   = jobMapper.toEntity(request);
        Job saved = jobRepository.save(job);
        return jobMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long id, JobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", id));
        jobMapper.updateEntity(request, job);
        return jobMapper.toResponse(job);
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job", id);
        }
        jobRepository.deleteById(id);
    }
}
```

---

## 9. Quy tắc Repository

```java
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // Derived query
    Page<Job> findByStatus(JobStatus status, Pageable pageable);
    List<Job> findByCompanyIdAndStatus(Long companyId, JobStatus status);
    boolean existsByTitleAndCompanyId(String title, Long companyId);

    // JPQL
    @Query("SELECT j FROM Job j WHERE j.title LIKE %:keyword% AND j.status = :status")
    Page<Job> searchJobs(@Param("keyword") String keyword,
                         @Param("status") JobStatus status,
                         Pageable pageable);

    // Native query (chỉ dùng khi cần tối ưu performance)
    @Query(value = "SELECT * FROM jobs WHERE MATCH(title, description) AGAINST (:keyword IN BOOLEAN MODE)",
           nativeQuery = true)
    List<Job> fullTextSearch(@Param("keyword") String keyword);
}
```

---

## 10. Exception Handling

Tất cả exception **phải** được xử lý tập trung tại `core/exception/GlobalExceptionHandler`.

### Cấu trúc

```
core/exception/
├── GlobalExceptionHandler.java   ← @RestControllerAdvice
├── ResourceNotFoundException.java
├── BadRequestException.java
├── UnauthorizedException.java
└── ForbiddenException.java
```

### Global Exception Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ErrorCodes.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "Invalid value")
                ));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCodes.VALIDATION_FAILED, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCodes.INTERNAL_ERROR, "Something went wrong"));
    }
}
```

### Custom Exception

```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
```

### API Response Wrapper

```java
@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final String  code;
    private final String  message;
    private final T       data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true).code("SUCCESS").data(data).build();
    }

    public static <T> ApiResponse<T> error(String code, Object message) {
        return ApiResponse.<T>builder()
                .success(false).code(code).message(message.toString()).build();
    }
}
```

---

## 11. Git Workflow

### Branch Naming

```
feature/{feature-name}-{task}
bugfix/{issue-description}
hotfix/{critical-fix}
```

**Ví dụ:**

```
feature/auth-login-api
feature/jobs-crud-api
feature/cv-upload
feature/ai-matching
bugfix/jwt-token-expiry
hotfix/null-pointer-jobs
```

### Workflow

```
1. Checkout từ main/develop
   git checkout develop
   git pull origin develop
   git checkout -b feature/jobs-crud-api

2. Code & commit theo convention
   git add .
   git commit -m "feat(jobs): add CRUD endpoints for job management"

3. Push branch
   git push origin feature/jobs-crud-api

4. Tạo Pull Request
   → Base: develop
   → Assign reviewer (1 người tối thiểu)

5. Code Review
   → Reviewer comment / approve

6. Merge vào develop sau khi approved
   → Squash and merge (khuyến khích)

7. Định kỳ merge develop → main cho release
```

### Commit Message Convention

```
feat(scope):     Thêm tính năng mới
fix(scope):      Sửa bug
refactor(scope): Refactor code (không thêm feature, không fix bug)
docs(scope):     Cập nhật tài liệu
test(scope):     Thêm/sửa test
chore(scope):    Cập nhật config, dependency
```

**Ví dụ:**

```
feat(auth): implement JWT refresh token
fix(jobs): handle null pointer when company not found
refactor(cv): extract CV parsing logic to separate service
docs: update API documentation
```

---

## 12. Code Style

### Naming Conventions

| Loại            | Convention  | Ví dụ                          |
| --------------- | ----------- | ------------------------------ |
| Class           | PascalCase  | `JobService`, `AuthController` |
| Interface       | PascalCase  | `JobService`, `UserMapper`     |
| Method          | camelCase   | `createJob()`, `findByEmail()` |
| Variable        | camelCase   | `jobTitle`, `userEmail`        |
| Constant        | UPPER_SNAKE | `API_PREFIX`, `MAX_RETRY`      |
| Package         | lowercase   | `com.smarthire.features.jobs`  |
| Database table  | snake_case  | `job_skills`, `cv_files`       |
| Database column | snake_case  | `created_at`, `company_id`     |

### Annotations Order (trên class)

```java
@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job { ... }
```

### Import Order

```java
// 1. Java standard
import java.time.LocalDateTime;
import java.util.List;

// 2. Spring
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 3. Third-party
import lombok.RequiredArgsConstructor;

// 4. Internal
import com.smarthire.features.jobs.dto.JobRequest;
import com.smarthire.shared.dto.ApiResponse;
```

---

## 13. Clean Code Rules

### Nguyên tắc chính

| Rule                  | Mô tả                                                |
| --------------------- | ---------------------------------------------------- |
| Single Responsibility | Một class = một responsibility                       |
| Method length         | Không quá **40 dòng**                                |
| Meaningful names      | Tên biến/method phải rõ nghĩa                        |
| No duplication        | Không duplicate code — trích xuất vào `shared/utils` |
| No magic numbers      | Dùng constant thay vì số/string literal              |

### Ví dụ

```java
// SAI ❌
public void process(User u, int t) {
    if (t == 1) { ... }   // 1 nghĩa là gì?
    if (u.getR() == 2) { ... }
}

// ĐÚNG ✅
public void processApplication(User applicant, ApplicationStatus status) {
    if (status == ApplicationStatus.PENDING) { ... }
    if (applicant.getRole() == UserRole.CANDIDATE) { ... }
}
```

```java
// SAI ❌ – magic string
String prefix = "/api/v1";

// ĐÚNG ✅ – dùng constant
public class ApiPaths {
    public static final String BASE    = "/api";
    public static final String JOBS    = BASE + "/jobs";
    public static final String AUTH    = BASE + "/auth";
    public static final String CV      = BASE + "/cv";
}
```

```java
// SAI ❌ – method quá dài, làm nhiều việc
public JobResponse createJobAndNotifyAndLog(JobRequest request) {
    // 80+ dòng ...
}

// ĐÚNG ✅ – tách nhỏ, mỗi method một việc
public JobResponse createJob(JobRequest request) {
    Job saved = saveJob(request);
    notifyEmployer(saved);
    return jobMapper.toResponse(saved);
}

private Job saveJob(JobRequest request) { ... }
private void notifyEmployer(Job job) { ... }
```

---

## 14. Quy tắc làm việc nhóm

### Phân công feature

| Developer | Feature phụ trách | Package                          |
| --------- | ----------------- | -------------------------------- |
| Dev 1     | `auth`            | `features/auth`, `core/security` |
| Dev 2     | `jobs`            | `features/jobs`                  |
| Dev 3     | `cv`              | `features/cv`                    |
| Dev 4     | `ai`              | `features/ai`                    |

### Quy tắc

- **KHÔNG** sửa code feature của người khác khi chưa báo.
- Shared code (`shared/`, `core/`) phải **tạo PR và có review** trước khi thay đổi.
- Khi cần dùng logic của feature khác → **báo owner feature đó** để expose qua Service.
- Conflict resolution: owner feature được quyền quyết định cuối cùng.

### Communication

```
Trước khi thay đổi shared code:
  1. Tạo issue/ticket mô tả thay đổi
  2. Tag toàn bộ team để review
  3. Merge sau khi ít nhất 2 người approved

Khi phát hiện bug ở feature khác:
  1. Tạo issue mô tả bug + cách tái hiện
  2. Assign cho owner feature đó
  3. Không tự sửa code của feature khác
```

---

## 15. Quy tắc API

### API Prefix

```
/api
```

### RESTful Conventions

| Method   | Endpoint                      | Mô tả                 |
| -------- | ----------------------------- | --------------------- |
| `GET`    | `/api/jobs`                   | Lấy danh sách jobs    |
| `GET`    | `/api/jobs/{id}`              | Lấy job theo ID       |
| `POST`   | `/api/jobs`                   | Tạo job mới           |
| `PUT`    | `/api/jobs/{id}`              | Cập nhật toàn bộ job  |
| `PATCH`  | `/api/jobs/{id}`              | Cập nhật một phần job |
| `DELETE` | `/api/jobs/{id}`              | Xóa job               |
| `GET`    | `/api/jobs/{id}/applications` | Sub-resource          |

### HTTP Status Codes

| Trường hợp         | Status Code                    |
| ------------------ | ------------------------------ |
| Thành công (đọc)   | `200 OK`                       |
| Tạo mới thành công | `201 Created`                  |
| Xóa thành công     | `200 OK` hoặc `204 No Content` |
| Lỗi input          | `400 Bad Request`              |
| Chưa xác thực      | `401 Unauthorized`             |
| Không có quyền     | `403 Forbidden`                |
| Không tìm thấy     | `404 Not Found`                |
| Lỗi server         | `500 Internal Server Error`    |

### Response Format

```json
// Thành công
{
  "success": true,
  "code": "SUCCESS",
  "data": { ... }
}

// Thất bại
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "Job not found with id: 123"
}

// Validation lỗi
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": {
    "title": "Title is required",
    "salary": "Salary must be positive"
  }
}
```

---

## 16. Nguyên tắc thiết kế

### Ưu tiên theo thứ tự

```
1. Clean Architecture       → tách biệt concern, dependency inversion
2. Separation of Concerns   → mỗi layer một trách nhiệm rõ ràng
3. Feature-based design     → dễ scale, dễ assign team
4. Reusable code            → shared/, utils/ cho logic tái sử dụng
5. DRY (Don't Repeat Yourself)
6. KISS (Keep It Simple, Stupid)
7. YAGNI (You Aren't Gonna Need It) → không over-engineer
```

### SOLID trong project

| Principle                     | Áp dụng                                               |
| ----------------------------- | ----------------------------------------------------- |
| **S** – Single Responsibility | Mỗi class/layer một nhiệm vụ                          |
| **O** – Open/Closed           | Dùng interface cho Service (dễ mở rộng, không sửa cũ) |
| **L** – Liskov Substitution   | `JobServiceImpl implements JobService`                |
| **I** – Interface Segregation | Tách nhỏ interface khi cần                            |
| **D** – Dependency Inversion  | Inject interface, không inject implementation         |

### Dependency Rules

```
Controller  →  Service (interface)
Service     →  Repository (interface)
Repository  →  Database

KHÔNG ĐƯỢC:
Controller  →  Repository   ❌
Service     →  Controller   ❌
Entity      →  DTO           ❌
```

---

> **Last updated:** March 2026
> **Maintained by:** SmartHire Backend Team
