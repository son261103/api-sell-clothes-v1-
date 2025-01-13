package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.Email;
import com.example.api_sell_clothes_v1.Repository.EmailRepository;
import com.example.api_sell_clothes_v1.Utils.OTPUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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


    public void sendOtpWithOTP(String recipientEmail, String username, String otp) {
        // Tạo Context để truyền dữ liệu cho mẫu Thymeleaf
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("otp", otp);

        // Chuyển mẫu Thymeleaf thành HTML
        String emailContent = templateEngine.process("Mail/emailTemplate", context);

        // Tạo email và lưu OTP vào cơ sở dữ liệu (không lưu body của email nữa)
        Email email = new Email();
        email.setRecipientEmail(recipientEmail);
        email.setSubject("Your OTP Code");
        email.setOtp(otp);  // Lưu mã OTP thay vì toàn bộ body
        email.setSentAt(LocalDateTime.now());
        email.setSent(false);
        emailRepository.save(email);

        try {
            // Gửi email với nội dung HTML
            MimeMessage mimeMessage = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(recipientEmail);
            helper.setSubject("Your OTP Code");
            helper.setText(emailContent, true);  // Gửi nội dung HTML (chứa OTP)

            // Gửi email
            emailSender.send(mimeMessage);

            // Cập nhật trạng thái email đã gửi
            email.setSent(true);
            emailRepository.save(email);
        } catch (MessagingException e) {
            // Nếu có lỗi khi gửi, lưu lại thông báo lỗi
            email.setErrorMessage(e.getMessage());
            emailRepository.save(email);
        }
    }

}
