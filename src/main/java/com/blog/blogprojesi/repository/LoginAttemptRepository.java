package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LoginAttempt Repository Interface
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    // Belirli kullanıcı için son başarısız denemeleri getir
    @Query("SELECT l FROM LoginAttempt l WHERE l.username = :username AND l.success = false " +
           "AND l.attemptTime >= :since ORDER BY l.attemptTime DESC")
    List<LoginAttempt> findRecentFailedAttempts(@Param("username") String username, 
                                                  @Param("since") LocalDateTime since);

    // IP adresine göre son başarısız denemeleri getir
    @Query("SELECT l FROM LoginAttempt l WHERE l.ipAddress = :ip AND l.success = false " +
           "AND l.attemptTime >= :since ORDER BY l.attemptTime DESC")
    List<LoginAttempt> findRecentFailedAttemptsByIp(@Param("ip") String ip, 
                                                      @Param("since") LocalDateTime since);

    // Başarısız deneme sayısını getir
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.username = :username AND l.success = false " +
           "AND l.attemptTime >= :since")
    long countRecentFailedAttempts(@Param("username") String username, @Param("since") LocalDateTime since);

    // IP için başarısız deneme sayısını getir
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.ipAddress = :ip AND l.success = false " +
           "AND l.attemptTime >= :since")
    long countRecentFailedAttemptsByIp(@Param("ip") String ip, @Param("since") LocalDateTime since);

    // Admin login denemelerini getir
    List<LoginAttempt> findByIsAdminLoginTrueOrderByAttemptTimeDesc();

    // Eski kayıtları temizle
    @Modifying
    @Query("DELETE FROM LoginAttempt l WHERE l.attemptTime < :before")
    void deleteOldAttempts(@Param("before") LocalDateTime before);

    // Kullanıcı için tüm denemeleri sil (başarılı login sonrası)
    @Modifying
    @Query("DELETE FROM LoginAttempt l WHERE l.username = :username AND l.success = false")
    void clearFailedAttempts(@Param("username") String username);
}
