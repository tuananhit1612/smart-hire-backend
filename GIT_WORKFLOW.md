# GIT WORKFLOW (GitHub + Jira) & COMMIT CONVENTION

## 1. Branch model
- **`main`**: production/stable. Không push hoặc commit trực tiếp.
- **`develop`**: development/integration. Tích hợp các feature mới.

### Nhánh làm việc theo task Jira:
- `feature/<TASK-ID>-<short-description>`: Tính năng mới.
- `fix/<TASK-ID>-<short-description>`: Sửa lỗi từ QA hoặc Bug bình thường.
- `hotfix/<TASK-ID>-<short-description>`: Sửa lỗi khẩn cấp trên production.
- `chore/<TASK-ID>-<short-description>`: Refactor code, build, config.
- `docs/<TASK-ID>-<short-description>`: Cập nhật tài liệu.

### Quy tắc đặt tên branch
**Format:** `<type>/<TASK-ID>-<short-description>`
- `short-description`: Viết thường, dùng dấu `-`, không dấu, ngắn gọn 3–6 từ.

**Ví dụ:**
- `feature/ATS-12-cv-upload`
- `feature/ATS-20-job-posting`
- `fix/ATS-31-application-status-bug`
- `hotfix/ATS-99-login-critical`
- `chore/ATS-10-update-docker-compose`

## 2. Quy trình làm việc chuẩn theo task Jira
### Step-by-step
1. Dev kéo task trên Jira → đổi trạng thái sang **In Progress**.
2. Tạo nhánh từ `develop`:
   ```bash
   git checkout develop
   git pull
   git checkout -b feature/ATS-12-cv-upload
   ```
3. Dev thực hiện code theo DoD (Definition of Done) và commit đúng convention.
4. Dev tự test local đảm bảo build pass.
5. Push branch lên GitHub.
6. Tạo Pull Request (PR) → target vào `develop`.
7. Nhắn PM để review code + test.
8. PM review:
   - Request changes (Yêu cầu sửa).
   - Hoặc Approve.
9. PM merge branch vào `develop`.
10. Sau khi merge:
    - Xóa branch (Delete branch trên GitHub).
    - Dev chuyển task Jira → **Done**.

**Rule:** Không push trực tiếp lên `main` hoặc `develop` (chỉ được merge qua PR).

## 3. Quy định Pull Request (PR)
### PR title
**Format:** `[TASK-ID] <short summary>`
**Ví dụ:** `[ATS-12] Add CV upload & versioning`

---

# COMMIT CONVENTION (Conventional Commits)

## 1. Format chuẩn
**Format:** `<type>(<scope>): <message> [TASK-ID]`
- `type`: Loại thay đổi.
- `scope`: Module/Feature (auth, jobs, applications, ai, ws, ui…).
- `message`: Ngắn gọn, bắt đầu bằng động từ (add, update, fix…).
- `[TASK-ID]`: Bắt buộc để trace Jira.

**Ví dụ chuẩn:**
- `feat(auth): add jwt login endpoint [ATS-01]`
- `feat(cv): implement cv upload & storage [ATS-12]`
- `fix(applications): prevent duplicate apply [ATS-31]`
- `refactor(jobs): split job service into query/command [ATS-40]`
- `chore(ci): add lint and test pipeline [ATS-05]`
- `docs(readme): update setup guide [ATS-06]`

## 2. Danh sách `type` dùng trong team
- `feat`: Thêm tính năng mới.
- `fix`: Sửa lỗi.
- `hotfix`: Sửa lỗi khẩn (thường message vẫn dùng `fix`, nhưng branch là `hotfix`).
- `refactor`: Chỉnh sửa code nhưng không làm thay đổi behavior.
- `perf`: Tối ưu hiệu năng.
- `test`: Thêm/sửa test.
- `docs`: Cập nhật tài liệu.
- `style`: Format code (dấu cách, tab, chấm phẩy...), không đổi logic.
- `chore`: Chỉnh sửa config, process build, update dependencies, việc lặt vặt.

## 3. Scope gợi ý cho dự án tuyển dụng AI
`auth`, `users`, `companies`, `jobs`, `applications`, `cv`, `ai`, `notifications`, `ws` (websocket), `dashboard`, `admin`, `ui`, `api`.

## 4. Hotfix workflow (Chuẩn doanh nghiệp)
Khi hệ thống xảy ra lỗi nghiêm trọng trên production:
1. Tạo nhánh riêng biệt từ `main`:
   ```bash
   git checkout main
   git pull
   git checkout -b hotfix/ATS-99-login-critical
   ```
2. Mở PR vào `main` → PM review và merge nhanh.
3. Tag bản release (nếu cần).
4. Phải merge ngược (backport) từ `main` vào `develop` để đồng bộ code lỗi đã sửa.

## 5. Quy tắc “xóa branch”
- Sau khi Pull Request được merge xong: **Delete branch**.
- Tuyệt đối **không reuse (dùng lại)** branch cũ cho task khác.
