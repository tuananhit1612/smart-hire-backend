package com.smarthire.backend.features.interview.service;

import com.smarthire.backend.core.exception.BadRequestException;
import com.smarthire.backend.core.exception.ForbiddenException;
import com.smarthire.backend.core.exception.ResourceNotFoundException;
import com.smarthire.backend.core.security.SecurityUtils;
import com.smarthire.backend.features.application.entity.Application;
import com.smarthire.backend.features.application.repository.ApplicationRepository;
import com.smarthire.backend.features.auth.entity.User;
import com.smarthire.backend.features.candidate.entity.CandidateProfile;
import com.smarthire.backend.features.candidate.repository.CandidateProfileRepository;
import com.smarthire.backend.features.interview.dto.CreateInterviewRequest;
import com.smarthire.backend.features.interview.dto.InterviewResponse;
import com.smarthire.backend.features.interview.dto.UpdateInterviewRequest;
import com.smarthire.backend.features.interview.entity.InterviewRoom;
import com.smarthire.backend.features.interview.repository.InterviewRoomRepository;
import com.smarthire.backend.shared.enums.InterviewStatus;
import com.smarthire.backend.shared.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRoomRepository interviewRoomRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + request.getApplicationId()));

        String roomCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        InterviewRoom room = InterviewRoom.builder()
                .application(application)
                .createdBy(currentUser)
                .roomName(request.getRoomName())
                .roomCode(roomCode)
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .meetingUrl(request.getMeetingUrl())
                .note(request.getNote())
                .status(InterviewStatus.SCHEDULED)
                .build();

        InterviewRoom saved = interviewRoomRepository.save(room);
        log.info("Interview room created: {} (code={}) by {}", saved.getRoomName(), roomCode, currentUser.getEmail());

        // Send interview invitation email to candidate
        sendInterviewNotification(saved, application);

        return toResponse(saved);
    }

    private void sendInterviewNotification(InterviewRoom room, Application application) {
        try {
            CandidateProfile profile = candidateProfileRepository.findById(application.getCandidateProfile().getId())
                    .orElse(null);
            if (profile == null || profile.getUser() == null) {
                log.warn("Cannot send interview email: candidate profile {} not found", application.getCandidateProfile().getId());
                return;
            }

            String email        = profile.getUser().getEmail();
            String candidateName = profile.getUser().getFullName() != null ? profile.getUser().getFullName() : "ứng viên";
            String jobTitle     = application.getJob().getTitle();
            String companyName  = application.getJob().getCompany() != null ? application.getJob().getCompany().getName() : "Công ty";

            String scheduledTime = room.getScheduledAt() != null
                    ? room.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm, EEEE dd/MM/yyyy", new java.util.Locale("vi", "VN")))
                    : "Sẽ được thông báo sau";

            String meetingUrl = room.getMeetingUrl();

            // Detect interview type from meetingUrl
            String interviewTypeBadge;
            String locationRow;
            if (meetingUrl != null && meetingUrl.startsWith("https://meet.google.com")) {
                interviewTypeBadge = "🖥️ Online (Google Meet)";
                locationRow = "<tr><td style=\"padding:8px;color:#666;\">Link họp:</td>"
                        + "<td style=\"padding:8px;\"><a href=\"" + meetingUrl + "\" style=\"color:#22c55e;\">" + meetingUrl + "</a></td></tr>";
            } else if (meetingUrl != null && !meetingUrl.isBlank()) {
                interviewTypeBadge = "📍 Phỏng vấn trực tiếp";
                locationRow = "<tr><td style=\"padding:8px;color:#666;\">Địa điểm:</td>"
                        + "<td style=\"padding:8px;font-weight:bold;\">" + meetingUrl + "</td></tr>";
            } else {
                interviewTypeBadge = "📞 Phỏng vấn qua điện thoại";
                locationRow = "";
            }

            String subject = "[SmartHire] 📅 Thư mời phỏng vấn — " + jobTitle;
            String body = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head><meta charset="UTF-8"></head>
                    <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                      <div style="max-width:600px;margin:32px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    
                        <!-- Header -->
                        <div style="background:linear-gradient(135deg,#22c55e,#16a34a);padding:32px 40px;">
                          <h1 style="color:#fff;margin:0;font-size:26px;font-weight:700;">SmartHire</h1>
                          <p style="color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:14px;">Hệ thống tuyển dụng thông minh</p>
                        </div>
                    
                        <!-- Body -->
                        <div style="padding:36px 40px;">
                          <h2 style="margin:0 0 8px;color:#1a1a1a;font-size:22px;">Thư mời phỏng vấn 🎉</h2>
                          <p style="color:#555;font-size:15px;margin:0 0 24px;">Xin chào <strong>%s</strong>,</p>
                          <p style="color:#555;font-size:15px;margin:0 0 24px;">
                            Chúng tôi trân trọng thông báo bạn đã được chọn để tham gia buổi phỏng vấn
                            cho vị trí <strong style="color:#16a34a;">%s</strong> tại <strong>%s</strong>.
                          </p>
                    
                          <!-- Interview Details Card -->
                          <div style="background:#f0fdf4;border-left:4px solid #22c55e;border-radius:8px;padding:20px 24px;margin-bottom:24px;">
                            <p style="font-weight:700;color:#15803d;margin:0 0 14px;font-size:15px;">📋 Thông tin buổi phỏng vấn</p>
                            <table style="border-collapse:collapse;width:100%%;">
                              <tr>
                                <td style="padding:7px 0;color:#666;width:130px;font-size:14px;">Tên phòng:</td>
                                <td style="padding:7px 0;font-weight:600;color:#1a1a1a;font-size:14px;">%s</td>
                              </tr>
                              <tr>
                                <td style="padding:7px 0;color:#666;font-size:14px;">Hình thức:</td>
                                <td style="padding:7px 0;font-weight:600;color:#1a1a1a;font-size:14px;">%s</td>
                              </tr>
                              <tr>
                                <td style="padding:7px 0;color:#666;font-size:14px;">Thời gian:</td>
                                <td style="padding:7px 0;font-weight:600;color:#1a1a1a;font-size:14px;">%s</td>
                              </tr>
                              <tr>
                                <td style="padding:7px 0;color:#666;font-size:14px;">Thời lượng:</td>
                                <td style="padding:7px 0;font-weight:600;color:#1a1a1a;font-size:14px;">%d phút</td>
                              </tr>
                              %s
                            </table>
                          </div>
                    
                          <p style="color:#555;font-size:14px;">
                            Vui lòng chuẩn bị đầy đủ và đúng giờ. Nếu bạn có bất kỳ câu hỏi nào,
                            vui lòng liên hệ bộ phận nhân sự.
                          </p>
                        </div>
                    
                        <!-- Footer -->
                        <div style="background:#f9fafb;padding:20px 40px;border-top:1px solid #e5e7eb;text-align:center;">
                          <p style="color:#9ca3af;font-size:12px;margin:0;">
                            © 2026 SmartHire. Email này được gửi tự động, vui lòng không trả lời.
                          </p>
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(
                    candidateName,
                    jobTitle,
                    companyName,
                    room.getRoomName(),
                    interviewTypeBadge,
                    scheduledTime,
                    room.getDurationMinutes(),
                    locationRow
            );

            emailService.sendHtmlEmail(email, subject, body);
            log.info("✅ Interview invitation email sent to {} for job '{}'", email, jobTitle);
        } catch (Exception e) {
            log.error("❌ Failed to send interview notification for room {}: {}", room.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsByApplication(Long applicationId) {
        return interviewRoomRepository.findByApplicationIdOrderByScheduledAtDesc(applicationId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getMyInterviews() {
        User currentUser = SecurityUtils.getCurrentUser();
        return interviewRoomRepository.findByCreatedByIdOrderByScheduledAtDesc(currentUser.getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public InterviewResponse updateInterview(Long id, UpdateInterviewRequest request) {
        InterviewRoom room = findOrThrow(id);
        checkOwnership(room);

        if (request.getRoomName() != null) room.setRoomName(request.getRoomName());
        if (request.getScheduledAt() != null) room.setScheduledAt(request.getScheduledAt());
        if (request.getDurationMinutes() != null) room.setDurationMinutes(request.getDurationMinutes());
        if (request.getMeetingUrl() != null) room.setMeetingUrl(request.getMeetingUrl());
        if (request.getNote() != null) room.setNote(request.getNote());

        InterviewRoom saved = interviewRoomRepository.save(room);
        log.info("Interview room updated: {}", saved.getRoomName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public InterviewResponse changeStatus(Long id, String status) {
        InterviewRoom room = findOrThrow(id);
        checkOwnership(room);

        InterviewStatus newStatus;
        try {
            newStatus = InterviewStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status. Must be: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED");
        }

        room.setStatus(newStatus);
        InterviewRoom saved = interviewRoomRepository.save(room);
        log.info("Interview room {} status changed to {}", saved.getRoomName(), newStatus);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteInterview(Long id) {
        InterviewRoom room = findOrThrow(id);
        checkOwnership(room);
        interviewRoomRepository.delete(room);
        log.info("Interview room deleted: {}", room.getRoomName());
    }

    // ── Helpers ──

    private InterviewRoom findOrThrow(Long id) {
        return interviewRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview room not found with id: " + id));
    }

    private void checkOwnership(InterviewRoom room) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (!room.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to modify this interview room");
        }
    }

    private InterviewResponse toResponse(InterviewRoom room) {
        return InterviewResponse.builder()
                .id(room.getId())
                .applicationId(room.getApplication().getId())
                .createdBy(room.getCreatedBy().getId())
                .roomName(room.getRoomName())
                .roomCode(room.getRoomCode())
                .scheduledAt(room.getScheduledAt())
                .durationMinutes(room.getDurationMinutes())
                .meetingUrl(room.getMeetingUrl())
                .note(room.getNote())
                .status(room.getStatus())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}
