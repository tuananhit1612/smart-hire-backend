package com.smarthire.backend.features.notification.service;

/**
 * Service gửi email thông báo cho các sự kiện quan trọng.
 */
public interface EmailService {

    /**
     * Gửi email xác nhận ứng tuyển thành công.
     */
    void sendApplicationConfirmation(String toEmail, String candidateName, String jobTitle, String companyName);

    /**
     * Gửi email mời phỏng vấn.
     */
    void sendInterviewInvitation(String toEmail, String candidateName, String jobTitle, String companyName);

    /**
     * Gửi email thông báo nhận offer.
     */
    void sendOfferNotification(String toEmail, String candidateName, String jobTitle, String companyName);

    /**
     * Gửi email chúc mừng trúng tuyển.
     */
    void sendHiredNotification(String toEmail, String candidateName, String jobTitle, String companyName);

    /**
     * Gửi email thông báo không trúng tuyển.
     */
    void sendRejectedNotification(String toEmail, String candidateName, String jobTitle, String companyName);

    /**
     * Gửi email reset mật khẩu.
     */
    void sendPasswordResetEmail(String toEmail, String fullName, String resetToken);
}
