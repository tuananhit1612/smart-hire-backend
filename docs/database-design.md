# SmartHire - Database Design (Student Project)

## 1. Tổng quan

Thiết kế database gồm **22 bảng** chia theo 5 module:

| Module            | Bảng                                                                                                                                           | Mô tả                 |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- |
| M0 - Core         | `users`, `refresh_tokens`                                                                                                                      | Auth & RBAC           |
| M1 - Candidate    | `candidate_profiles`, `candidate_educations`, `candidate_experiences`, `candidate_skills`, `candidate_projects`, `cv_files`, `cv_builder_data` | Hồ sơ & CV Builder    |
| M2 - Employer     | `companies`, `jobs`, `job_skills`, `applications`, `application_stage_history`, `interview_rooms`                                              | Tuyển dụng & pipeline |
| M3 - AI           | `ai_cv_parsed`, `ai_match_results`, `ai_cv_reviews`, `ai_interview_sessions`, `ai_interview_questions`, `ai_interview_answers`                 | AI xử lý & đánh giá   |
| M4 - Notification | `notifications`                                                                                                                                | Thông báo realtime    |

> **Lưu ý:** `cv_builder_data` phân biệt CV tạo từ Builder vs Upload. `ai_cv_parsed` lưu kết quả AI trích xuất. `interview_rooms` phục vụ tính năng streaming phỏng vấn trực tuyến.

---

## 2. ERD - Entity Relationship Diagram

### 2.1 Module 0: Core Platform

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash
        ENUM role "CANDIDATE | HR | ADMIN"
        VARCHAR full_name
        VARCHAR phone
        VARCHAR avatar_url
        BOOLEAN is_active
        DATETIME created_at
        DATETIME updated_at
    }

    refresh_tokens {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token UK
        DATETIME expires_at
        BOOLEAN is_revoked
        DATETIME created_at
    }

    users ||--o{ refresh_tokens : "has"
```

### 2.2 Module 1: Candidate Portal

```mermaid
erDiagram
    users ||--|| candidate_profiles : "has"
    candidate_profiles ||--o{ candidate_educations : "has"
    candidate_profiles ||--o{ candidate_experiences : "has"
    candidate_profiles ||--o{ candidate_skills : "has"
    candidate_profiles ||--o{ candidate_projects : "has"
    candidate_profiles ||--o{ cv_files : "owns"

    candidate_profiles {
        BIGINT id PK
        BIGINT user_id FK_UK
        VARCHAR headline
        TEXT summary
        DATE date_of_birth
        ENUM gender "MALE | FEMALE | OTHER"
        VARCHAR address
        VARCHAR city
        INT years_of_experience
        ENUM job_level "INTERN | JUNIOR | MID | SENIOR | LEAD | MANAGER"
        DATETIME created_at
        DATETIME updated_at
    }

    candidate_educations {
        BIGINT id PK
        BIGINT candidate_profile_id FK
        VARCHAR institution
        VARCHAR degree
        VARCHAR field_of_study
        DATE start_date
        DATE end_date
        DECIMAL gpa
        TEXT description
    }

    candidate_experiences {
        BIGINT id PK
        BIGINT candidate_profile_id FK
        VARCHAR company_name
        VARCHAR title
        DATE start_date
        DATE end_date
        BOOLEAN is_current
        TEXT description
    }

    candidate_skills {
        BIGINT id PK
        BIGINT candidate_profile_id FK
        VARCHAR skill_name
        ENUM proficiency_level "BEGINNER | INTERMEDIATE | ADVANCED | EXPERT"
    }

    candidate_projects {
        BIGINT id PK
        BIGINT candidate_profile_id FK
        VARCHAR project_name
        TEXT description
        TEXT technologies
    }

    cv_files {
        BIGINT id PK
        BIGINT candidate_profile_id FK
        VARCHAR file_name
        VARCHAR file_path
        ENUM file_type "PDF | DOCX"
        ENUM source "UPLOAD | BUILDER"
        INT file_size
        BOOLEAN is_primary
        DATETIME created_at
    }

    cv_builder_data {
        BIGINT id PK
        BIGINT cv_file_id FK_UK
        BIGINT candidate_profile_id FK
        VARCHAR template_id
        JSON sections_data
        DATETIME created_at
        DATETIME updated_at
    }

    cv_files ||--o| cv_builder_data : "has"
```

### 2.3 Module 2: Employer/HR Portal

```mermaid
erDiagram
    users ||--o{ companies : "creates"
    companies ||--o{ jobs : "posts"
    jobs ||--o{ job_skills : "requires"
    jobs ||--o{ applications : "receives"
    candidate_profiles ||--o{ applications : "applies"
    cv_files ||--o{ applications : "used_in"
    applications ||--o{ application_stage_history : "tracks"
    applications ||--o{ interview_rooms : "has"

    companies {
        BIGINT id PK
        VARCHAR name
        VARCHAR logo_url
        VARCHAR website
        VARCHAR industry
        ENUM company_size "STARTUP | SMALL | MEDIUM | LARGE | ENTERPRISE"
        TEXT description
        VARCHAR address
        VARCHAR city
        BIGINT created_by FK
        BOOLEAN is_verified
        DATETIME created_at
        DATETIME updated_at
    }

    jobs {
        BIGINT id PK
        BIGINT company_id FK
        BIGINT created_by FK
        VARCHAR title
        TEXT description
        TEXT requirements
        TEXT benefits
        ENUM job_type "FULL_TIME | PART_TIME | CONTRACT | INTERNSHIP"
        ENUM job_level "INTERN | JUNIOR | MID | SENIOR | LEAD | MANAGER"
        VARCHAR location
        BOOLEAN is_remote
        DECIMAL salary_min
        DECIMAL salary_max
        VARCHAR salary_currency
        DATE deadline
        ENUM status "DRAFT | OPEN | CLOSED"
        DATETIME created_at
        DATETIME updated_at
    }

    job_skills {
        BIGINT id PK
        BIGINT job_id FK
        VARCHAR skill_name
        ENUM skill_type "MUST_HAVE | NICE_TO_HAVE"
    }

    applications {
        BIGINT id PK
        BIGINT job_id FK
        BIGINT candidate_profile_id FK
        BIGINT cv_file_id FK
        ENUM stage "APPLIED | SCREENING | INTERVIEW | OFFER | HIRED | REJECTED"
        DATETIME applied_at
        DATETIME updated_at
    }

    application_stage_history {
        BIGINT id PK
        BIGINT application_id FK
        VARCHAR from_stage
        VARCHAR to_stage
        BIGINT changed_by FK
        TEXT note
        DATETIME created_at
    }

    interview_rooms {
        BIGINT id PK
        BIGINT application_id FK
        BIGINT created_by FK
        VARCHAR room_name
        VARCHAR room_code UK
        DATETIME scheduled_at
        INT duration_minutes
        VARCHAR meeting_url
        TEXT note
        ENUM status "SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED"
        DATETIME created_at
        DATETIME updated_at
    }
```

### 2.4 Module 3: AI Service

```mermaid
erDiagram
    cv_files ||--o{ ai_cv_parsed : "parsed"
    applications ||--o{ ai_match_results : "scored"
    cv_files ||--o{ ai_cv_reviews : "reviewed"
    candidate_profiles ||--o{ ai_interview_sessions : "practices"
    ai_interview_sessions ||--o{ ai_interview_questions : "contains"
    ai_interview_questions ||--o| ai_interview_answers : "answered"

    ai_cv_parsed {
        BIGINT id PK
        BIGINT cv_file_id FK
        JSON parsed_data
        ENUM status "PENDING | PROCESSING | COMPLETED | FAILED"
        TEXT error_message
        DATETIME created_at
    }

    ai_match_results {
        BIGINT id PK
        BIGINT application_id FK
        DECIMAL score_total "0-100"
        JSON score_breakdown
        JSON strengths
        JSON gaps
        JSON recommendations
        TEXT explanation
        DATETIME created_at
    }

    ai_cv_reviews {
        BIGINT id PK
        BIGINT cv_file_id FK
        JSON issues
        JSON suggestions
        DECIMAL overall_rating
        DATETIME created_at
    }

    ai_interview_sessions {
        BIGINT id PK
        BIGINT candidate_profile_id FK
        BIGINT job_id FK
        BIGINT cv_file_id FK
        ENUM status "IN_PROGRESS | COMPLETED"
        DECIMAL total_score
        JSON summary
        DATETIME created_at
        DATETIME completed_at
    }

    ai_interview_questions {
        BIGINT id PK
        BIGINT session_id FK
        TEXT question_text
        ENUM question_type "TECHNICAL | BEHAVIORAL | SITUATIONAL"
        INT display_order
    }

    ai_interview_answers {
        BIGINT id PK
        BIGINT question_id FK
        TEXT answer_text
        DECIMAL score
        TEXT feedback
        DATETIME created_at
    }
```

### 2.5 Module 4: Notification

```mermaid
erDiagram
    users ||--o{ notifications : "receives"

    notifications {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR type
        VARCHAR title
        TEXT content
        VARCHAR reference_type
        BIGINT reference_id
        BOOLEAN is_read
        DATETIME created_at
    }
```

---

## 3. Tổng quan quan hệ (Full ERD)

```mermaid
erDiagram
    users ||--|| candidate_profiles : "1:1"
    users ||--o{ companies : "creates"
    users ||--o{ refresh_tokens : "has"
    users ||--o{ notifications : "receives"

    candidate_profiles ||--o{ candidate_educations : "1:N"
    candidate_profiles ||--o{ candidate_experiences : "1:N"
    candidate_profiles ||--o{ candidate_skills : "1:N"
    candidate_profiles ||--o{ candidate_projects : "1:N"
    candidate_profiles ||--o{ cv_files : "1:N"
    cv_files ||--o| cv_builder_data : "1:1"
    candidate_profiles ||--o{ applications : "1:N"
    candidate_profiles ||--o{ ai_interview_sessions : "1:N"

    companies ||--o{ jobs : "1:N"
    jobs ||--o{ job_skills : "1:N"
    jobs ||--o{ applications : "1:N"

    cv_files ||--o{ applications : "used_in"
    cv_files ||--o{ ai_cv_parsed : "1:N"
    cv_files ||--o{ ai_cv_reviews : "1:N"

    applications ||--o{ application_stage_history : "1:N"
    applications ||--o{ ai_match_results : "1:N"
    applications ||--o{ interview_rooms : "1:N"

    ai_interview_sessions ||--o{ ai_interview_questions : "1:N"
    ai_interview_questions ||--|| ai_interview_answers : "1:1"
```

---

## 4. Mapping chức năng → Bảng DB

| #          | Chức năng                      | Bảng phục vụ                                                              |
| ---------- | ------------------------------ | ------------------------------------------------------------------------- |
| 1          | Đăng ký, đăng nhập, RBAC       | `users`, `refresh_tokens`                                                 |
| 2          | Hồ sơ ứng viên + công ty       | `candidate_profiles`, `candidate_*`, `companies`                          |
| 3          | Đăng tin + pipeline (realtime) | `jobs`, `job_skills`, `applications`, `application_stage_history`         |
| 4          | AI trích xuất CV               | `ai_cv_parsed`                                                            |
| 5          | AI matching CV-JD + chấm điểm  | `ai_match_results`                                                        |
| 6          | Gợi ý job/ứng viên             | `ai_match_results` (query by score)                                       |
| 7          | Theo dõi trạng thái            | `applications`, `application_stage_history`                               |
| 8          | Dashboard thống kê             | Query aggregate từ `applications`, `jobs`, `ai_match_results`             |
| 9          | Thông báo                      | `notifications`                                                           |
| **Thầy 1** | CV Builder                     | `cv_builder_data`, `cv_files`                                             |
| **Thầy 2** | AI review/chấm CV              | `ai_cv_reviews`                                                           |
| **Thầy 3** | Phỏng vấn ảo AI                | `ai_interview_sessions`, `ai_interview_questions`, `ai_interview_answers` |
| **Thầy 4** | Từ CV tạo câu hỏi              | `ai_interview_sessions.cv_file_id` → `ai_interview_questions`             |
| **Thầy 5** | HR streaming Meet              | `interview_rooms`                                                         |
| **Thầy 6** | Toàn bộ trên hệ thống          | Tất cả bảng trên                                                          |

## 5. Điểm thiết kế chính

| Nghiệp vụ             | Giải pháp                                                              |
| --------------------- | ---------------------------------------------------------------------- | --- | ------------- | ------------------------------------------------------------------ | --- | ----------------- | ------------------------------------------------- |
| **Chống apply trùng** | `UNIQUE(job_id, candidate_profile_id)` trên `applications`             |
| **Pipeline tracking** | `applications.stage` (current) + `application_stage_history` (history) |
| **CV Builder**        | `cv_builder_data.sections_data` (JSON) lưu form data → export PDF      |     | **source CV** | `cv_files.source` = UPLOAD \| BUILDER — phân biệt loại CV trong UI |     | **AI CV parsing** | `ai_cv_parsed` extract structured JSON từ CV file |
| **Video Interview**   | `interview_rooms` với `room_code` unique để join phòng                 |
| **RBAC**              | `users.role` = CANDIDATE / HR / ADMIN                                  |
| **AI scoring**        | `score_breakdown` (JSON) chứa skills_match, exp_match, semantic_match  |
| **Notification**      | Polymorphic reference (`reference_type` + `reference_id`)              |
| **Full-text search**  | FULLTEXT index trên `jobs(title, description, requirements)`           |
