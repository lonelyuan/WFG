package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import com.example.demo.config.EmailConfig;
import com.example.demo.entity.EmailTemplate;
import com.example.demo.repository.EmailTemplateRepository;

import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;

@Service
public class EmailService {

    @Autowired
    private EmailConfig emailConfig;

    @Autowired
    private EmailTemplateRepository templateRepository;

    @Autowired
    private TemplateService templateService;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.enabled}")
    private boolean emailEnabled;

    @Async
    public CompletableFuture<Boolean> sendWelcomeEmail(String toEmail, String username) {
        if (!emailEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        EmailTemplate template = templateRepository.findByName("welcome");
        String subject = "Welcome to Our Platform";
        String content = templateService.processTemplate(template.getContent(), 
            "username", username);
        
        return sendEmail(toEmail, subject, content);
    }

    @Async
    public CompletableFuture<Boolean> sendActivationEmail(String toEmail) {
        if (!emailEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        EmailTemplate template = templateRepository.findByName("activation");
        String subject = "Account Activated";
        String content = templateService.processTemplate(template.getContent());
        
        return sendEmail(toEmail, subject, content);
    }

    @Async
    public CompletableFuture<Boolean> sendPasswordResetEmail(String toEmail, String resetToken) {
        if (!emailEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        EmailTemplate template = templateRepository.findByName("password_reset");
        String subject = "Password Reset Request";
        String resetLink = generateResetLink(resetToken);
        String content = templateService.processTemplate(template.getContent(), 
            "resetLink", resetLink);
        
        return sendEmail(toEmail, subject, content);
    }

    @Async
    public CompletableFuture<Boolean> sendOrderConfirmationEmail(String toEmail, String orderNumber) {
        if (!emailEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        EmailTemplate template = templateRepository.findByName("order_confirmation");
        String subject = "Order Confirmation - " + orderNumber;
        String content = templateService.processTemplate(template.getContent(), 
            "orderNumber", orderNumber);
        
        return sendEmail(toEmail, subject, content);
    }

    @Async
    public CompletableFuture<Boolean> sendPromotionEmail(String toEmail, String promotionCode) {
        if (!emailEnabled) {
            return CompletableFuture.completedFuture(false);
        }

        EmailTemplate template = templateRepository.findByName("promotion");
        String subject = "Special Offer Just for You!";
        String content = templateService.processTemplate(template.getContent(), 
            "promotionCode", promotionCode);
        
        return sendEmail(toEmail, subject, content);
    }

    private CompletableFuture<Boolean> sendEmail(String toEmail, String subject, String content) {
        try {
            // 模拟邮件发送逻辑
            EmailMessage message = new EmailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setContent(content);
            message.setSentAt(LocalDateTime.now());
            
            // 实际的邮件发送逻辑会在这里
            boolean sent = emailConfig.getMailSender().send(message);
            
            if (sent) {
                logEmailSent(toEmail, subject);
            } else {
                logEmailFailed(toEmail, subject);
            }
            
            return CompletableFuture.completedFuture(sent);
        } catch (Exception e) {
            logEmailError(toEmail, subject, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private String generateResetLink(String resetToken) {
        return emailConfig.getBaseUrl() + "/reset-password?token=" + resetToken;
    }

    private void logEmailSent(String toEmail, String subject) {
        System.out.println("Email sent successfully to: " + toEmail + ", Subject: " + subject);
    }

    private void logEmailFailed(String toEmail, String subject) {
        System.err.println("Failed to send email to: " + toEmail + ", Subject: " + subject);
    }

    private void logEmailError(String toEmail, String subject, Exception e) {
        System.err.println("Error sending email to: " + toEmail + ", Subject: " + subject + 
                          ", Error: " + e.getMessage());
    }

    public boolean isEmailServiceAvailable() {
        return emailEnabled && emailConfig.isConfigured();
    }

    private static class EmailMessage {
        private String from;
        private String to;
        private String subject;
        private String content;
        private LocalDateTime sentAt;

        // Getters and setters
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    }
} 