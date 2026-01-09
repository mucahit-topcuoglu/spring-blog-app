package com.blog.blogprojesi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sistem ayarları Entity sınıfı
 * Site adı, bakım modu gibi genel ayarları saklar
 */
@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_type")
    private String settingType; // STRING, BOOLEAN, INTEGER, JSON

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Ayar anahtarları için sabitler
     */
    public static final String KEY_SITE_NAME = "site_name";
    public static final String KEY_SITE_DESCRIPTION = "site_description";
    public static final String KEY_MAINTENANCE_MODE = "maintenance_mode";
    public static final String KEY_MAINTENANCE_MESSAGE = "maintenance_message";
    public static final String KEY_DEFAULT_USER_ROLE = "default_user_role";
    public static final String KEY_REGISTRATION_ENABLED = "registration_enabled";
    public static final String KEY_COMMENTS_ENABLED = "comments_enabled";
    public static final String KEY_MAX_LOGIN_ATTEMPTS = "max_login_attempts";
    public static final String KEY_LOCKOUT_DURATION_MINUTES = "lockout_duration_minutes";
}
