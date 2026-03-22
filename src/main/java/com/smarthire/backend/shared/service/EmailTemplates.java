package com.smarthire.backend.shared.service;

/**
 * Static HTML email templates for the SmartHire application.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static String buildResetPasswordEmail(String userName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:'Segoe UI',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                    <tr><td align="center">
                      <table width="520" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,0.08);overflow:hidden;">
                        <!-- Header -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#2563eb,#7c3aed);padding:32px 40px;text-align:center;">
                            <h1 style="margin:0;color:#ffffff;font-size:24px;">SmartHire</h1>
                          </td>
                        </tr>
                        <!-- Body -->
                        <tr>
                          <td style="padding:32px 40px;">
                            <p style="margin:0 0 16px;font-size:16px;color:#1e293b;">
                              Xin chào <strong>%s</strong>,
                            </p>
                            <p style="margin:0 0 24px;font-size:14px;color:#475569;line-height:1.6;">
                              Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                              Nhấn vào nút bên dưới để tạo mật khẩu mới:
                            </p>
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr><td align="center" style="padding:8px 0 24px;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 36px;background:#2563eb;color:#ffffff;
                                          font-size:15px;font-weight:600;text-decoration:none;border-radius:8px;">
                                  Đặt lại mật khẩu
                                </a>
                              </td></tr>
                            </table>
                            <p style="margin:0 0 8px;font-size:13px;color:#94a3b8;">
                              Hoặc copy đường link sau vào trình duyệt:
                            </p>
                            <p style="margin:0 0 24px;font-size:12px;color:#2563eb;word-break:break-all;">
                              %s
                            </p>
                            <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;">
                            <p style="margin:0;font-size:12px;color:#94a3b8;line-height:1.5;">
                              ⏰ Link này sẽ hết hạn sau <strong>15 phút</strong>.<br>
                              Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.
                            </p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8fafc;padding:20px 40px;text-align:center;">
                            <p style="margin:0;font-size:12px;color:#94a3b8;">
                              © 2026 SmartHire. All rights reserved.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(userName, resetLink, resetLink);
    }
}
