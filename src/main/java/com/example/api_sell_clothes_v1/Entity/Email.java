package com.example.api_sell_clothes_v1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail;  // Địa chỉ email người nhận

    @Column(nullable = false)
    private String subject;  // Tiêu đề email

    @Column(nullable = false)
    private String otp;  // Nội dung email

    @Column(nullable = false)
    private LocalDateTime sentAt;  // Thời gian gửi email

    @Column(nullable = false)
    private boolean isSent;  // Trạng thái đã gửi hay chưa

    @Column(nullable = true)
    private String errorMessage;  // Nếu gửi không thành công, lưu thông báo lỗi
}
