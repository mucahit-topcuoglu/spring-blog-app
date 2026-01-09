package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.SystemSettings;
import com.blog.blogprojesi.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * System Settings Service
 * Sistem ayarlarını yönetir
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    /**
     * Ayar değerini getir
     */
    @Transactional(readOnly = true)
    public String getSetting(String key) {
        return systemSettingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue)
                .orElse(null);
    }

    /**
     * Ayar değerini getir (varsayılan değerli)
     */
    @Transactional(readOnly = true)
    public String getSetting(String key, String defaultValue) {
        return systemSettingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue)
                .orElse(defaultValue);
    }

    /**
     * Boolean ayar değerini getir
     */
    @Transactional(readOnly = true)
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        String value = getSetting(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    /**
     * Integer ayar değerini getir
     */
    @Transactional(readOnly = true)
    public int getIntSetting(String key, int defaultValue) {
        String value = getSetting(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Ayarı güncelle veya oluştur
     */
    public SystemSettings setSetting(String key, String value, String updatedBy) {
        Optional<SystemSettings> existingSetting = systemSettingsRepository.findBySettingKey(key);
        
        SystemSettings setting;
        if (existingSetting.isPresent()) {
            setting = existingSetting.get();
            setting.setSettingValue(value);
            setting.setUpdatedBy(updatedBy);
        } else {
            setting = SystemSettings.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .settingType("STRING")
                    .updatedBy(updatedBy)
                    .build();
        }

        SystemSettings savedSetting = systemSettingsRepository.save(setting);
        log.info("Setting updated: {} = {} by {}", key, value, updatedBy);
        return savedSetting;
    }

    /**
     * Boolean ayarı güncelle
     */
    public void setBooleanSetting(String key, boolean value, String updatedBy) {
        setSetting(key, String.valueOf(value), updatedBy);
    }

    /**
     * Integer ayarı güncelle
     */
    public void setIntSetting(String key, int value, String updatedBy) {
        setSetting(key, String.valueOf(value), updatedBy);
    }

    /**
     * Tüm ayarları getir
     */
    @Transactional(readOnly = true)
    public List<SystemSettings> getAllSettings() {
        return systemSettingsRepository.findAll();
    }

    /**
     * Ayarları Map olarak getir
     */
    @Transactional(readOnly = true)
    public Map<String, String> getSettingsAsMap() {
        Map<String, String> settingsMap = new HashMap<>();
        List<SystemSettings> settings = systemSettingsRepository.findAll();
        for (SystemSettings setting : settings) {
            settingsMap.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return settingsMap;
    }

    /**
     * Bakım modu aktif mi?
     */
    @Transactional(readOnly = true)
    public boolean isMaintenanceMode() {
        return getBooleanSetting(SystemSettings.KEY_MAINTENANCE_MODE, false);
    }

    /**
     * Bakım modunu aç/kapa
     */
    public void setMaintenanceMode(boolean enabled, String updatedBy) {
        setBooleanSetting(SystemSettings.KEY_MAINTENANCE_MODE, enabled, updatedBy);
        log.info("Maintenance mode {} by {}", enabled ? "enabled" : "disabled", updatedBy);
    }

    /**
     * Site adını getir
     */
    @Transactional(readOnly = true)
    public String getSiteName() {
        return getSetting(SystemSettings.KEY_SITE_NAME, "Blog Projesi");
    }

    /**
     * Site adını güncelle
     */
    public void setSiteName(String siteName, String updatedBy) {
        setSetting(SystemSettings.KEY_SITE_NAME, siteName, updatedBy);
    }

    /**
     * Kayıt aktif mi?
     */
    @Transactional(readOnly = true)
    public boolean isRegistrationEnabled() {
        return getBooleanSetting(SystemSettings.KEY_REGISTRATION_ENABLED, true);
    }

    /**
     * Maksimum login deneme sayısını getir
     */
    @Transactional(readOnly = true)
    public int getMaxLoginAttempts() {
        return getIntSetting(SystemSettings.KEY_MAX_LOGIN_ATTEMPTS, 5);
    }

    /**
     * Kilitleme süresini getir (dakika)
     */
    @Transactional(readOnly = true)
    public int getLockoutDurationMinutes() {
        return getIntSetting(SystemSettings.KEY_LOCKOUT_DURATION_MINUTES, 30);
    }

    /**
     * Varsayılan ayarları oluştur
     */
    public void initializeDefaultSettings() {
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_SITE_NAME)) {
            setSetting(SystemSettings.KEY_SITE_NAME, "Blog Projesi", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_SITE_DESCRIPTION)) {
            setSetting(SystemSettings.KEY_SITE_DESCRIPTION, "Harika bir blog platformu", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_MAINTENANCE_MODE)) {
            setSetting(SystemSettings.KEY_MAINTENANCE_MODE, "false", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_MAINTENANCE_MESSAGE)) {
            setSetting(SystemSettings.KEY_MAINTENANCE_MESSAGE, "Site bakım modundadır. Lütfen daha sonra tekrar deneyiniz.", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_DEFAULT_USER_ROLE)) {
            setSetting(SystemSettings.KEY_DEFAULT_USER_ROLE, "USER", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_REGISTRATION_ENABLED)) {
            setSetting(SystemSettings.KEY_REGISTRATION_ENABLED, "true", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_COMMENTS_ENABLED)) {
            setSetting(SystemSettings.KEY_COMMENTS_ENABLED, "true", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_MAX_LOGIN_ATTEMPTS)) {
            setSetting(SystemSettings.KEY_MAX_LOGIN_ATTEMPTS, "5", "SYSTEM");
        }
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.KEY_LOCKOUT_DURATION_MINUTES)) {
            setSetting(SystemSettings.KEY_LOCKOUT_DURATION_MINUTES, "30", "SYSTEM");
        }
        log.info("Default system settings initialized");
    }
}
