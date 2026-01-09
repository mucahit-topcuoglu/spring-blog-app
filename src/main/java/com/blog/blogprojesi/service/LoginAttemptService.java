package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.LoginAttempt;
import com.blog.blogprojesi.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Login Attempt Service
 * Brute-force koruması için login denemelerini yönetir
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final SystemSettingsService systemSettingsService;

    /**
     * Login denemesini kaydet
     */
    public void recordLoginAttempt(String username, String ipAddress, boolean success, boolean isAdminLogin) {
        LoginAttempt attempt = LoginAttempt.builder()
                .username(username)
                .ipAddress(ipAddress)
                .success(success)
                .isAdminLogin(isAdminLogin)
                .build();

        loginAttemptRepository.save(attempt);

        if (success) {
            // Başarılı giriş sonrası başarısız denemeleri temizle
            loginAttemptRepository.clearFailedAttempts(username);
            log.info("Successful login for user: {}", username);
        } else {
            log.warn("Failed login attempt for user: {} from IP: {}", username, ipAddress);
        }
    }

    /**
     * Kullanıcı kilitli mi kontrol et
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(String username) {
        int maxAttempts = systemSettingsService.getMaxLoginAttempts();
        int lockoutMinutes = systemSettingsService.getLockoutDurationMinutes();
        LocalDateTime since = LocalDateTime.now().minusMinutes(lockoutMinutes);

        long failedAttempts = loginAttemptRepository.countRecentFailedAttempts(username, since);
        
        if (failedAttempts >= maxAttempts) {
            log.warn("User {} is blocked due to {} failed login attempts", username, failedAttempts);
            return true;
        }
        return false;
    }

    /**
     * IP adresi kilitli mi kontrol et
     */
    @Transactional(readOnly = true)
    public boolean isIpBlocked(String ipAddress) {
        int maxAttempts = systemSettingsService.getMaxLoginAttempts() * 3; // IP için 3 kat tolerans
        int lockoutMinutes = systemSettingsService.getLockoutDurationMinutes();
        LocalDateTime since = LocalDateTime.now().minusMinutes(lockoutMinutes);

        long failedAttempts = loginAttemptRepository.countRecentFailedAttemptsByIp(ipAddress, since);
        
        if (failedAttempts >= maxAttempts) {
            log.warn("IP {} is blocked due to {} failed login attempts", ipAddress, failedAttempts);
            return true;
        }
        return false;
    }

    /**
     * Kalan deneme sayısını getir
     */
    @Transactional(readOnly = true)
    public int getRemainingAttempts(String username) {
        int maxAttempts = systemSettingsService.getMaxLoginAttempts();
        int lockoutMinutes = systemSettingsService.getLockoutDurationMinutes();
        LocalDateTime since = LocalDateTime.now().minusMinutes(lockoutMinutes);

        long failedAttempts = loginAttemptRepository.countRecentFailedAttempts(username, since);
        return Math.max(0, maxAttempts - (int) failedAttempts);
    }

    /**
     * Kullanıcının başarısız denemelerini temizle
     */
    public void clearFailedAttempts(String username) {
        loginAttemptRepository.clearFailedAttempts(username);
        log.info("Failed login attempts cleared for user: {}", username);
    }

    /**
     * Eski kayıtları temizle (7 günden eski)
     * Her gün gece 3'te çalışır
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        loginAttemptRepository.deleteOldAttempts(cutoff);
        log.info("Old login attempts cleaned up (older than {})", cutoff);
    }
}
