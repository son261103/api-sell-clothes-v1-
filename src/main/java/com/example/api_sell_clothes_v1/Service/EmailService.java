package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.Email;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Repository.EmailRepository;
import com.example.api_sell_clothes_v1.Utils.OTPUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private TemplateEngine templateEngine;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendOtpWithOTP(String recipientEmail, String username, String otp) {
        try {
            // Log trước khi xử lý template
            log.info("Processing email template for user: {}", username);

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("otp", otp);

            String emailContent = templateEngine.process("Mail/emailTemplate", context);

            // Log sau khi xử lý template
            log.info("Email content generated successfully");

            Users users = new Users();


            Email email = new Email();
            email.setRecipientEmail(recipientEmail);
            email.setSubject("AURAS");
            email.setOtp(otp);
            email.setSentAt(LocalDateTime.now());
            email.setSent(false);
            emailRepository.save(email);

            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("sonphaman5@gmail.com"); // Thêm địa chỉ người gửi
            helper.setTo(recipientEmail);
            helper.setSubject("Your OTP Code");
            helper.setText(emailContent, true);

            emailSender.send(mimeMessage);

            log.info("Email sent successfully to: {}", recipientEmail);

            email.setSent(true);
            emailRepository.save(email);
        } catch (MessagingException e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }

}
