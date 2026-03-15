# SmartHire - Task Breakdown (Jira-Ready)

> Project: SmartHire Backend
> Stack: Spring Boot 4, Java 25, MySQL, JWT, WebSocket, Ollama
---

## MODULE: AUTHENTICATION & USER

| Task ID | Task                                 | Short description                                                                          |
| ------- | ------------------------------------ | ------------------------------------------------------------------------------------------ |
| BE004   | Implement authentication API         | Hoan thanh register, login, refresh token, logout bang JWT va refresh token rotation.      |
| BE005   | Implement authorization and security | Cau hinh Spring Security, JWT filter, RBAC cho Candidate/HR/Admin va bao ve endpoint.      |
| BE006   | Implement user profile API           | Cung cap API lay va cap nhat thong tin user hien tai.                                      |
| BE007   | Implement avatar and file storage    | Tich hop storage service de upload avatar va tai su dung cho cac file khac trong he thong. |
| BE008   | Implement password recovery flow     | Hoan thanh forgot password, reset password, change password.                               |
| BE009   | Implement global error handling      | Chuan hoa response loi, validate input va xu ly exception toan he thong.                   |

---

## MODULE: EMPLOYER / HR PORTAL

| Task ID | Task                                    | Short description                                                                          |
| ------- | --------------------------------------- | ------------------------------------------------------------------------------------------ |
| BE010   | Implement company management API        | Tao, cap nhat, xem thong tin cong ty va upload logo cong ty.                               |
| BE011   | Implement HR job management API         | CRUD job, quan ly job skills va thay doi trang thai `DRAFT/OPEN/CLOSED`.                   |
| BE012   | Implement public job listing API        | Cung cap API xem chi tiet job va tim kiem job theo keyword, location, level, type, salary. |
| BE013   | Implement HR application management API | HR xem danh sach ung vien theo job, xem chi tiet application va thong tin CV/AI result.    |
| BE014   | Implement recruitment pipeline API      | Chuyen stage ung vien, luu history va ghi chu noi bo cho qua trinh tuyen dung.             |
| BE015   | Implement interview scheduling API      | Tao va quan ly interview room, lich phong van va trang thai buoi phong van.                |

---

## MODULE: CANDIDATE PORTAL

| Task ID | Task                                         | Short description                                                               |
| ------- | -------------------------------------------- | ------------------------------------------------------------------------------- |
| BE016   | Implement candidate profile API              | Quan ly profile ung vien gom thong tin ca nhan, headline, summary va job level. |
| BE017   | Implement candidate resume details API       | CRUD hoc van, kinh nghiem, ky nang va du an cua ung vien.                       |
| BE018   | Implement CV upload and versioning API       | Upload CV PDF/DOCX, quan ly danh sach CV, set primary va xoa CV hop le.         |
| BE019   | Implement CV Builder API                     | Tao CV bang form, luu du lieu theo section va export PDF thanh file CV.         |
| BE020   | Implement candidate apply job API            | Ung vien nop ho so vao job, chong apply trung va validate dieu kien ung tuyen.  |
| BE021   | Implement candidate application tracking API | Xem danh sach application cua minh, loc theo stage/job va rut don khi hop le.   |

---

## MODULE: AI SERVICE

| Task ID | Task                                | Short description                                                                 |
| ------- | ----------------------------------- | --------------------------------------------------------------------------------- |
| AI001   | Setup AI integration infrastructure | Tich hop Ollama client, prompt template va async processing cho cac tac vu AI.    |
| AI002   | Implement CV parsing API            | Parse CV thanh JSON co cau truc va cung cap endpoint trigger + xem ket qua.       |
| AI003   | Implement CV-JD matching API        | Cham diem muc do phu hop CV va job, tra ve score, strengths, gaps va explanation. |
| AI004   | Implement CV review API             | AI phan tich loi CV, goi y cai thien theo section va luu ket qua review.          |
| AI005   | Implement virtual interview API     | Tao interview session, sinh cau hoi, cham cau tra loi va tra report tong ket.     |

---

## MODULE: NOTIFICATION & REALTIME

| Task ID | Task                                 | Short description                                                                      |
| ------- | ------------------------------------ | -------------------------------------------------------------------------------------- |
| BE022   | Implement realtime event system      | Cau hinh WebSocket/STOMP va phat realtime event khi apply, doi stage, AI xong.         |
| BE023   | Implement in-app notification API    | Tao notification, danh sach notification, mark as read, mark all read va unread count. |
| BE024   | Implement email notification service | Gui email cho cac su kien chinh: apply thanh cong, moi phong van, ket qua ung tuyen.   |

---

## MODULE: DASHBOARD & REPORTING

| Task ID | Task                           | Short description                                                                 |
| ------- | ------------------------------ | --------------------------------------------------------------------------------- |
| BE025   | Implement HR dashboard API     | Thong ke tong ung vien, funnel theo stage, pass rate va chi so tuyen dung cho HR. |
| BE026   | Implement admin dashboard API  | Tong hop user theo role, tong so job, company va hoat dong he thong cho Admin.    |
| BE027   | Implement reporting export API | Export bao cao tuyen dung theo dinh dang CSV cho dashboard/reporting.             |

---

## MODULE: QA & DOCUMENTATION

| Task ID | Task                                  | Short description                                                          |
| ------- | ------------------------------------- | -------------------------------------------------------------------------- |
| QA001   | Implement auth and security tests     | Unit/integration test cho auth flow, JWT, RBAC va endpoint protection.     |
| QA002   | Implement recruitment flow tests      | Test job management, apply flow, pipeline transition va notification flow. |
| QA003   | Implement API documentation artifacts | Hoan thien Swagger annotation va Postman collection cho toan bo API.       |

---
_Tai lieu cap nhat: 2026-03-12 | SmartHire Backend Team_
