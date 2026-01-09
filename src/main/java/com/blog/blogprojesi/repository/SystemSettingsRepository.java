package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SystemSettings Repository Interface
 */
@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    Optional<SystemSettings> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);
}
