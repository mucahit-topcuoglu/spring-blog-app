package com.blog.blogprojesi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Admin işlemlerini loglamak için Entity sınıfı
 * Admin panelindeki tüm önemli işlemleri kaydeder
 */
@Entity
@Table(name = "admin_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_username", nullable = false)
    private String adminUsername;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AdminActionType actionType;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    /**
     * Admin işlem türleri
     */
    public enum AdminActionType {
        LOGIN,
        LOGOUT,
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        USER_ENABLE,
        USER_DISABLE,
        USER_ROLE_CHANGE,
        POST_CREATE,
        POST_UPDATE,
        POST_DELETE,
        POST_PUBLISH,
        POST_UNPUBLISH,
        POST_FEATURE,
        COMMENT_DELETE,
        SETTINGS_UPDATE,
        MAINTENANCE_MODE_ON,
        MAINTENANCE_MODE_OFF,
        SYSTEM_ACTION
    }
}
