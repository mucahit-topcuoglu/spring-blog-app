package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.Role;
import com.blog.blogprojesi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Repository Interface
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.posts WHERE u.username = :username")
    Optional<User> findByUsernameWithPosts(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    // ==================== ADMIN QUERIES ====================

    // Role'e göre kullanıcıları getir
    List<User> findByRole(Role role);

    // Admin kullanıcıları getir
    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    // Aktif/pasif kullanıcıları getir
    List<User> findByIsEnabled(boolean isEnabled);

    // Sayfalama ile tüm kullanıcıları getir
    Page<User> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Admin sayısını getir
    long countByRole(Role role);

    // Aktif kullanıcı sayısı
    long countByIsEnabled(boolean isEnabled);

    // Belirli tarihten sonra kayıt olanları getir
    @Query("SELECT u FROM User u WHERE u.createdAt >= :since ORDER BY u.createdAt DESC")
    List<User> findUsersRegisteredSince(@Param("since") LocalDateTime since);

    // Bugün kayıt olan kullanıcı sayısı
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :today")
    long countUsersRegisteredToday(@Param("today") LocalDateTime today);

    // Bu ay kayıt olan kullanıcı sayısı
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startOfMonth")
    long countUsersRegisteredThisMonth(@Param("startOfMonth") LocalDateTime startOfMonth);

    // Günlük kayıt istatistikleri (son 30 gün)
    @Query("SELECT FUNCTION('DATE', u.createdAt) as regDate, COUNT(u) as count FROM User u " +
           "WHERE u.createdAt >= :startDate GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY regDate DESC")
    List<Object[]> countRegistrationsByDay(@Param("startDate") LocalDateTime startDate);
}
