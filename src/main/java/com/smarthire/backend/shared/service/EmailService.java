package com.smarthire.backend.shared.service;

/**
 * Service for sending email notifications.
 */
public interface EmailService {

    /**
     * Send a simple text email.
     */
    void sendSimpleEmail(String to, String subject, String body);

    /**
     * Send an HTML-formatted email.
     */
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
