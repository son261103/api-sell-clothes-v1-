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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

    @Value("${spring.mail.username}")
    private String fromEmail;

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
//-------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gửi email văn bản đơn giản
     */
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            emailSender.send(message);
            log.info("Đã gửi email đến {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đến {}: {}", to, e.getMessage());
        }
    }

    /**
     * Gửi email HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("Đã gửi HTML email đến {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi HTML email đến {}: {}", to, e.getMessage());
        }
    }

    /**
     * Gửi email thông báo xác nhận đơn hàng
     */
    public void sendOrderConfirmationEmail(String to, String customerName, String orderNumber, String orderDetails) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("orderDetails", orderDetails);

            String emailContent = templateEngine.process("Mail/orderConfirmation", context);

            sendHtmlEmail(to, "Xác nhận đơn hàng #" + orderNumber, emailContent);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận đơn hàng: {}", e.getMessage());
        }
    }

    /**
     * Gửi email thông báo thanh toán thành công
     */
    public void sendPaymentConfirmationEmail(String to, String customerName, String orderNumber,
                                             String paymentMethod, String amount, String transactionId) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("paymentMethod", paymentMethod);
            context.setVariable("amount", amount);
            context.setVariable("transactionId", transactionId);
            context.setVariable("paymentDate", LocalDateTime.now().toString());

            String emailContent = templateEngine.process("Mail/paymentConfirmation", context);

            sendHtmlEmail(to, "Xác nhận thanh toán đơn hàng #" + orderNumber, emailContent);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận thanh toán: {}", e.getMessage());
        }
    }

    /**
     * Gửi email thông báo cập nhật trạng thái đơn hàng
     */
    public void sendOrderStatusUpdateEmail(String to, String customerName, String orderNumber, String newStatus) {
        try {
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("newStatus", newStatus);
            context.setVariable("updateDate", LocalDateTime.now().toString());

            String emailContent = templateEngine.process("Mail/orderStatusUpdate", context);

            sendHtmlEmail(to, "Cập nhật trạng thái đơn hàng #" + orderNumber, emailContent);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email cập nhật trạng thái đơn hàng: {}", e.getMessage());
        }
    }
}
