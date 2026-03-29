package com.smarthire.backend.features.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service("notificationEmailService")
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-name:SmartHire}")
    private String fromName;

    @Value("${app.mail.from-address:noreply@smarthire.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    // ═══ PUBLIC METHODS (all @Async) ═══

    @Override
    @Async
    public void sendApplicationConfirmation(String toEmail, String candidateName, String jobTitle, String companyName) {
        String subject = "✅ Ứng tuyển thành công — " + jobTitle;
        String body = buildHtml(
                "Ứng tuyển thành công!",
                "#10b981",
                "Xin chào <strong>" + candidateName + "</strong>,",
                "Đơn ứng tuyển của bạn cho vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong> đã được ghi nhận thành công.",
                "Chúng tôi sẽ xem xét hồ sơ và phản hồi trong thời gian sớm nhất.",
                "📋 Bạn có thể theo dõi trạng thái đơn ứng tuyển trên hệ thống SmartHire."
        );
        sendEmail(toEmail, subject, body);
    }

    @Override
    @Async
    public void sendInterviewInvitation(String toEmail, String candidateName, String jobTitle, String companyName) {
        String subject = "📅 Mời phỏng vấn — " + jobTitle;
        String body = buildHtml(
                "Mời phỏng vấn",
                "#6366f1",
                "Xin chào <strong>" + candidateName + "</strong>,",
                "Chúng tôi vui mừng thông báo bạn đã vượt qua vòng sàng lọc cho vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>.",
                "Bạn được mời tham gia vòng phỏng vấn. Vui lòng đăng nhập hệ thống SmartHire để xem chi tiết lịch phỏng vấn.",
                "💡 Hãy chuẩn bị kỹ và chúc bạn thành công!"
        );
        sendEmail(toEmail, subject, body);
    }

    @Override
    @Async
    public void sendOfferNotification(String toEmail, String candidateName, String jobTitle, String companyName) {
        String subject = "🎉 Thư mời nhận việc — " + jobTitle;
        String body = buildHtml(
                "Bạn nhận được Offer!",
                "#f59e0b",
                "Xin chào <strong>" + candidateName + "</strong>,",
                "Tuyệt vời! Sau quá trình đánh giá, chúng tôi rất vui được gửi đến bạn lời mời nhận việc cho vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>.",
                "Vui lòng đăng nhập hệ thống SmartHire để xem chi tiết offer.",
                "🎯 Chúng tôi mong sớm được đón bạn vào đội ngũ!"
        );
        sendEmail(toEmail, subject, body);
    }

    @Override
    @Async
    public void sendHiredNotification(String toEmail, String candidateName, String jobTitle, String companyName) {
        String subject = "🎊 Chúc mừng trúng tuyển — " + jobTitle;
        String body = buildHtml(
                "Chúc mừng! Bạn đã trúng tuyển!",
                "#10b981",
                "Xin chào <strong>" + candidateName + "</strong>,",
                "Chúng tôi rất vui mừng thông báo bạn đã chính thức được tuyển dụng cho vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>! 🎉",
                "Bộ phận Nhân sự sẽ sớm liên hệ để hướng dẫn bạn thủ tục onboarding.",
                "🚀 Chào mừng bạn gia nhập đội ngũ!"
        );
        sendEmail(toEmail, subject, body);
    }

    @Override
    @Async
    public void sendRejectedNotification(String toEmail, String candidateName, String jobTitle, String companyName) {
        String subject = "Kết quả ứng tuyển — " + jobTitle;
        String body = buildHtml(
                "Kết quả ứng tuyển",
                "#64748b",
                "Xin chào <strong>" + candidateName + "</strong>,",
                "Cảm ơn bạn đã quan tâm và ứng tuyển vị trí <strong>" + jobTitle + "</strong> tại <strong>" + companyName + "</strong>.",
                "Sau quá trình đánh giá kỹ lưỡng, chúng tôi rất tiếc phải thông báo rằng hồ sơ của bạn chưa phù hợp với yêu cầu tại thời điểm này.",
                "💪 Chúng tôi đánh giá cao nỗ lực của bạn và khuyến khích bạn tiếp tục ứng tuyển các vị trí khác trong tương lai."
        );
        sendEmail(toEmail, subject, body);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        String subject = "🔐 Đặt lại mật khẩu — SmartHire";
        String body = buildHtml(
                "Đặt lại mật khẩu",
                "#6366f1",
                "Xin chào <strong>" + fullName + "</strong>,",
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.",
                "Mã reset: <div style='background:#f1f5f9;padding:12px;border-radius:8px;text-align:center;font-size:20px;font-weight:bold;letter-spacing:2px;margin:10px 0;color:#1e293b'>" + resetToken + "</div>",
                "⏰ Mã này có hiệu lực trong 15 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này."
        );
        sendEmail(toEmail, subject, body);
    }

    // ═══ PRIVATE HELPERS ═══

    private void sendEmail(String to, String subject, String htmlBody) {
        if (!emailEnabled) {
            log.info("[EMAIL DISABLED] Would send to={}, subject={}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to={}, subject={}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to={}: {}", to, e.getMessage());
        }
    }

    private String buildHtml(String title, String accentColor, String greeting, String line1, String line2, String line3) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background:#f8fafc;font-family:'Segoe UI',Arial,sans-serif">
              <div style="max-width:560px;margin:30px auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)">
                <!-- Header -->
                <div style="background:%s;padding:28px 32px;text-align:center">
                  <h1 style="margin:0;color:#ffffff;font-size:20px;font-weight:700">%s</h1>
                </div>
                <!-- Body -->
                <div style="padding:28px 32px;color:#334155;font-size:14px;line-height:1.7">
                  <p>%s</p>
                  <p>%s</p>
                  <p>%s</p>
                  <p style="margin-top:16px">%s</p>
                </div>
                <!-- Footer -->
                <div style="background:#f1f5f9;padding:16px 32px;text-align:center;font-size:11px;color:#94a3b8">
                  <p style="margin:0">© 2026 SmartHire — Nền tảng tuyển dụng thông minh</p>
                  <p style="margin:4px 0 0">Email này được gửi tự động, vui lòng không trả lời.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(accentColor, title, greeting, line1, line2, line3);
    }
}
