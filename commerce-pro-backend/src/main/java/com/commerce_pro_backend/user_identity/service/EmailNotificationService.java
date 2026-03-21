package com.commerce_pro_backend.user_identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles all transactional email notifications for the identity module.
 * Emails are sent asynchronously so they never block the main request thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@commercepro.local}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Send temporary password to the user after an admin-triggered reset.
     */
    @Async
    public void sendPasswordResetNotification(String toEmail, String username, String temporaryPassword) {
        String subject = "Your Commerce Pro Password Has Been Reset";
        String body = buildPasswordResetEmail(username, temporaryPassword);
        sendEmail(toEmail, subject, body);
    }

    /**
     * Notify user their account has been locked after too many failed attempts.
     */
    @Async
    public void sendAccountLockedNotification(String toEmail, String username, int durationMinutes) {
        String subject = "Commerce Pro: Account Temporarily Locked";
        String body = String.format(
            "<h2>Account Locked</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Your account has been temporarily locked due to multiple failed login attempts.</p>" +
            "<p>It will be automatically unlocked in <strong>%d minutes</strong>.</p>" +
            "<p>If you did not attempt to log in, please contact your administrator immediately.</p>",
            username, durationMinutes
        );
        sendEmail(toEmail, subject, body);
    }

    /**
     * Notify user that MFA has been enabled on their account.
     */
    @Async
    public void sendMfaEnabledNotification(String toEmail, String username) {
        String subject = "Commerce Pro: Two-Factor Authentication Enabled";
        String body = String.format(
            "<h2>MFA Enabled</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Two-factor authentication has been successfully enabled on your account.</p>" +
            "<p>You will now be required to provide a TOTP code on each login.</p>" +
            "<p>If you did not make this change, contact your administrator immediately.</p>",
            username
        );
        sendEmail(toEmail, subject, body);
    }

    /**
     * Notify user that MFA has been disabled on their account.
     */
    @Async
    public void sendMfaDisabledNotification(String toEmail, String username) {
        String subject = "Commerce Pro: Two-Factor Authentication Disabled";
        String body = String.format(
            "<h2>MFA Disabled</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Two-factor authentication has been <strong>disabled</strong> on your account.</p>" +
            "<p>If you did not make this change, contact your administrator immediately.</p>",
            username
        );
        sendEmail(toEmail, subject, body);
    }

    /**
     * Notify user their account has been created.
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String username, String temporaryPassword) {
        String subject = "Welcome to Commerce Pro";
        String body = String.format(
            "<h2>Welcome to Commerce Pro</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Your account has been created. Here are your login credentials:</p>" +
            "<p><strong>Username:</strong> %s</p>" +
            "<p><strong>Temporary Password:</strong> <code>%s</code></p>" +
            "<p>Please <a href='%s/auth/login'>log in</a> and change your password immediately.</p>" +
            "<p>This temporary password will expire on first use.</p>",
            username, username, temporaryPassword, frontendUrl
        );
        sendEmail(toEmail, subject, body);
    }

    private String buildPasswordResetEmail(String username, String temporaryPassword) {
        return String.format(
            "<h2>Password Reset</h2>" +
            "<p>Hi %s,</p>" +
            "<p>Your password has been reset by an administrator.</p>" +
            "<p><strong>Temporary Password:</strong> <code>%s</code></p>" +
            "<p>Please <a href='%s/auth/login'>log in</a> and change your password immediately.</p>" +
            "<p>This temporary password expires on first use.</p>" +
            "<p>If you did not request this reset, contact your administrator immediately.</p>",
            username, temporaryPassword, frontendUrl
        );
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (Exception e) {
            // Email failure must never break the main transaction
            log.error("Failed to send email to {} with subject '{}': {}", to, subject, e.getMessage());
        }
    }
}
