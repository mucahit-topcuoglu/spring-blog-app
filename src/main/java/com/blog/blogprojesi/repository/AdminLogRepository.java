package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.AdminLog;
import com.blog.blogprojesi.entity.AdminLog.AdminActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AdminLog Repository Interface
 */
@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    // Admin ID'ye göre logları getir
    List<AdminLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    // İşlem tipine göre getir
    List<AdminLog> findByActionTypeOrderByCreatedAtDesc(AdminActionType actionType);

    // Son logları getir (sayfalama ile)
    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Son N logu getir
    List<AdminLog> findTop50ByOrderByCreatedAtDesc();

    // Tarih aralığına göre getir
    @Query("SELECT a FROM AdminLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AdminLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Belirli bir hedefe yapılan işlemleri getir
    List<AdminLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);

    // Bugünkü login sayısını getir
    @Query("SELECT COUNT(a) FROM AdminLog a WHERE a.actionType = 'LOGIN' AND a.createdAt >= :today")
    long countTodayLogins(@Param("today") LocalDateTime today);

    // Son 7 günlük işlem sayıları
    @Query("SELECT FUNCTION('DATE', a.createdAt) as logDate, COUNT(a) as count FROM AdminLog a " +
           "WHERE a.createdAt >= :startDate GROUP BY FUNCTION('DATE', a.createdAt) ORDER BY logDate DESC")
    List<Object[]> countActionsByDay(@Param("startDate") LocalDateTime startDate);
}
