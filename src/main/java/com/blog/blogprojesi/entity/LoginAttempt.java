package com.blog.blogprojesi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Login deneme kayıtları Entity sınıfı
 * Brute-force koruması için kullanılır
 */
@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(name = "success")
    private boolean success;

    @Column(name = "is_admin_login")
    private boolean isAdminLogin;

    @PrePersist
    protected void onCreate() {
        attemptTime = LocalDateTime.now();
    }
}
