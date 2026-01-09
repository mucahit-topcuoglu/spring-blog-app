package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.AdminLog;
import com.blog.blogprojesi.entity.AdminLog.AdminActionType;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin Log Service
 * Admin işlemlerini loglar
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;

    /**
     * Admin işlemini logla
     */
    public AdminLog logAction(User admin, AdminActionType actionType, String action, 
                               String targetType, Long targetId, String details, String ipAddress) {
        AdminLog adminLog = AdminLog.builder()
                .adminId(admin.getId())
                .adminUsername(admin.getUsername())
                .actionType(actionType)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ipAddress)
                .build();

        AdminLog savedLog = adminLogRepository.save(adminLog);
        log.info("Admin action logged: {} - {} by {}", actionType, action, admin.getUsername());
        return savedLog;
    }

    /**
     * Admin girişini logla
     */
    public void logLogin(User admin, String ipAddress) {
        logAction(admin, AdminActionType.LOGIN, "Admin girişi yapıldı", 
                  "USER", admin.getId(), null, ipAddress);
    }

    /**
     * Admin çıkışını logla
     */
    public void logLogout(User admin, String ipAddress) {
        logAction(admin, AdminActionType.LOGOUT, "Admin çıkışı yapıldı", 
                  "USER", admin.getId(), null, ipAddress);
    }

    /**
     * Kullanıcı işlemini logla
     */
    public void logUserAction(User admin, AdminActionType actionType, User targetUser, 
                               String action, String ipAddress) {
        logAction(admin, actionType, action, "USER", targetUser.getId(), 
                  "Hedef kullanıcı: " + targetUser.getUsername(), ipAddress);
    }

    /**
     * Post işlemini logla
     */
    public void logPostAction(User admin, AdminActionType actionType, Long postId, 
                               String action, String ipAddress) {
        logAction(admin, actionType, action, "POST", postId, null, ipAddress);
    }

    /**
     * Ayar değişikliğini logla
     */
    public void logSettingsUpdate(User admin, String settingKey, String oldValue, 
                                   String newValue, String ipAddress) {
        String details = String.format("Ayar: %s, Eski: %s, Yeni: %s", settingKey, oldValue, newValue);
        logAction(admin, AdminActionType.SETTINGS_UPDATE, "Sistem ayarı güncellendi", 
                  "SETTINGS", null, details, ipAddress);
    }

    /**
     * Son logları getir
     */
    @Transactional(readOnly = true)
    public List<AdminLog> getRecentLogs() {
        return adminLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    /**
     * Sayfalama ile logları getir
     */
    @Transactional(readOnly = true)
    public Page<AdminLog> getLogs(int page, int size) {
        return adminLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    /**
     * Belirli bir admin'in loglarını getir
     */
    @Transactional(readOnly = true)
    public List<AdminLog> getLogsByAdmin(Long adminId) {
        return adminLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }

    /**
     * Belirli bir hedefe yapılan işlemleri getir
     */
    @Transactional(readOnly = true)
    public List<AdminLog> getLogsByTarget(String targetType, Long targetId) {
        return adminLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }

    /**
     * Bugünkü admin girişlerini say
     */
    @Transactional(readOnly = true)
    public long getTodayLoginCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return adminLogRepository.countTodayLogins(today);
    }
}
