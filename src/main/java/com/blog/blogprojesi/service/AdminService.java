package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.*;
import com.blog.blogprojesi.entity.AdminLog.AdminActionType;
import com.blog.blogprojesi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin Service
 * Admin paneli işlemlerini yönetir
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AdminLogService adminLogService;
    private final PasswordEncoder passwordEncoder;

    // ==================== USER MANAGEMENT ====================

    /**
     * Tüm kullanıcıları getir (sayfalama ile)
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(int page, int size) {
        return userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    /**
     * Tüm kullanıcıları getir
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Kullanıcı ara
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    /**
     * Kullanıcıyı ID ile getir
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Kullanıcıyı aktif/pasif yap
     */
    public User toggleUserEnabled(Long userId, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        boolean wasEnabled = user.isEnabled();
        user.setEnabled(!wasEnabled);
        User savedUser = userRepository.save(user);

        AdminActionType actionType = wasEnabled ? AdminActionType.USER_DISABLE : AdminActionType.USER_ENABLE;
        String action = wasEnabled ? "Kullanıcı pasif yapıldı" : "Kullanıcı aktif yapıldı";
        adminLogService.logUserAction(admin, actionType, user, action, ipAddress);

        log.info("User {} {} by admin {}", user.getUsername(), wasEnabled ? "disabled" : "enabled", admin.getUsername());
        return savedUser;
    }

    /**
     * Kullanıcı rolünü değiştir
     */
    public User changeUserRole(Long userId, Role newRole, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Admin kendi rolünü değiştiremez
        if (user.getId().equals(admin.getId())) {
            throw new RuntimeException("Kendi rolünüzü değiştiremezsiniz");
        }

        Role oldRole = user.getRole();
        user.setRole(newRole);
        User savedUser = userRepository.save(user);

        String action = String.format("Kullanıcı rolü değiştirildi: %s -> %s", oldRole, newRole);
        adminLogService.logUserAction(admin, AdminActionType.USER_ROLE_CHANGE, user, action, ipAddress);

        log.info("User {} role changed from {} to {} by admin {}", 
                 user.getUsername(), oldRole, newRole, admin.getUsername());
        return savedUser;
    }

    /**
     * Kullanıcıyı sil
     */
    public void deleteUser(Long userId, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Admin kendini silemez
        if (user.getId().equals(admin.getId())) {
            throw new RuntimeException("Kendinizi silemezsiniz");
        }

        // Son admin silinmeye çalışılıyor mu?
        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new RuntimeException("Sistemde en az bir admin bulunmalıdır");
            }
        }

        String username = user.getUsername();
        adminLogService.logAction(admin, AdminActionType.USER_DELETE, 
                                   "Kullanıcı silindi: " + username, 
                                   "USER", userId, null, ipAddress);

        userRepository.delete(user);
        log.info("User {} deleted by admin {}", username, admin.getUsername());
    }

    /**
     * Yeni kullanıcı oluştur (admin tarafından)
     */
    public User createUser(String username, String email, String password, 
                           String firstName, String lastName, Role role,
                           User admin, String ipAddress) {
        // Validasyonlar
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .isEnabled(true)
                .build();

        User savedUser = userRepository.save(user);

        adminLogService.logUserAction(admin, AdminActionType.USER_CREATE, savedUser, 
                                       "Yeni kullanıcı oluşturuldu", ipAddress);

        log.info("User {} created by admin {}", username, admin.getUsername());
        return savedUser;
    }

    /**
     * Kullanıcı bilgilerini güncelle
     */
    public User updateUser(Long userId, String email, String firstName, String lastName,
                           String bio, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
            }
            user.setEmail(email);
        }

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (bio != null) user.setBio(bio);

        User savedUser = userRepository.save(user);

        adminLogService.logUserAction(admin, AdminActionType.USER_UPDATE, savedUser, 
                                       "Kullanıcı bilgileri güncellendi", ipAddress);

        log.info("User {} updated by admin {}", user.getUsername(), admin.getUsername());
        return savedUser;
    }

    /**
     * Kullanıcı şifresini sıfırla
     */
    public void resetUserPassword(Long userId, String newPassword, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        adminLogService.logUserAction(admin, AdminActionType.USER_UPDATE, user, 
                                       "Kullanıcı şifresi sıfırlandı", ipAddress);

        log.info("Password reset for user {} by admin {}", user.getUsername(), admin.getUsername());
    }

    // ==================== POST MANAGEMENT ====================

    /**
     * Tüm postları getir (sayfalama ile)
     */
    @Transactional(readOnly = true)
    public Page<Post> getAllPosts(int page, int size) {
        return postRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * Tüm postları getir
     */
    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    /**
     * Post'u ID ile getir
     */
    @Transactional(readOnly = true)
    public Optional<Post> getPostById(Long id) {
        return postRepository.findByIdWithAuthor(id);
    }

    /**
     * Post yayınla/gizle
     */
    public Post togglePostPublished(Long postId, User admin, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        boolean wasPublished = post.isPublished();
        post.setPublished(!wasPublished);
        Post savedPost = postRepository.save(post);

        AdminActionType actionType = wasPublished ? AdminActionType.POST_UNPUBLISH : AdminActionType.POST_PUBLISH;
        String action = wasPublished ? "Post gizlendi" : "Post yayınlandı";
        adminLogService.logPostAction(admin, actionType, postId, action, ipAddress);

        log.info("Post {} {} by admin {}", postId, wasPublished ? "unpublished" : "published", admin.getUsername());
        return savedPost;
    }

    /**
     * Post'u öne çıkar/çıkarma
     */
    public Post togglePostFeatured(Long postId, User admin, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        boolean wasFeatured = post.isFeatured();
        post.setFeatured(!wasFeatured);
        Post savedPost = postRepository.save(post);

        String action = wasFeatured ? "Post öne çıkarmadan kaldırıldı" : "Post öne çıkarıldı";
        adminLogService.logPostAction(admin, AdminActionType.POST_FEATURE, postId, action, ipAddress);

        log.info("Post {} {} by admin {}", postId, wasFeatured ? "unfeatured" : "featured", admin.getUsername());
        return savedPost;
    }

    /**
     * Post'u sil
     */
    public void deletePost(Long postId, User admin, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        String title = post.getTitle();
        adminLogService.logPostAction(admin, AdminActionType.POST_DELETE, postId, 
                                       "Post silindi: " + title, ipAddress);

        postRepository.delete(post);
        log.info("Post {} deleted by admin {}", postId, admin.getUsername());
    }

    /**
     * Post bilgilerini güncelle
     */
    public Post updatePost(Long postId, String title, String content, String category,
                           boolean commentsEnabled, User admin, String ipAddress) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        if (title != null) post.setTitle(title);
        if (content != null) {
            post.setContent(content);
            if (content.length() > 200) {
                post.setExcerpt(content.substring(0, 200) + "...");
            } else {
                post.setExcerpt(content);
            }
        }
        if (category != null) post.setCategory(category);
        post.setCommentsEnabled(commentsEnabled);
        post.setUpdatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);

        adminLogService.logPostAction(admin, AdminActionType.POST_UPDATE, postId, 
                                       "Post güncellendi", ipAddress);

        log.info("Post {} updated by admin {}", postId, admin.getUsername());
        return savedPost;
    }

    // ==================== COMMENT MANAGEMENT ====================

    /**
     * Tüm yorumları getir
     */
    @Transactional(readOnly = true)
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    /**
     * Yorumu sil
     */
    public void deleteComment(Long commentId, User admin, String ipAddress) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı"));

        adminLogService.logAction(admin, AdminActionType.COMMENT_DELETE, 
                                   "Yorum silindi", "COMMENT", commentId, 
                                   "Post ID: " + comment.getPost().getId(), ipAddress);

        commentRepository.delete(comment);
        log.info("Comment {} deleted by admin {}", commentId, admin.getUsername());
    }

    // ==================== STATISTICS ====================

    /**
     * Dashboard istatistiklerini getir
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfMonth = today.withDayOfMonth(1);
        LocalDateTime last7Days = today.minusDays(7);
        LocalDateTime last30Days = today.minusDays(30);

        // Kullanıcı istatistikleri
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAdmins", userRepository.countByRole(Role.ADMIN));
        stats.put("activeUsers", userRepository.countByIsEnabled(true));
        stats.put("newUsersToday", userRepository.countUsersRegisteredToday(today));
        stats.put("newUsersThisMonth", userRepository.countUsersRegisteredThisMonth(startOfMonth));

        // Post istatistikleri
        stats.put("totalPosts", postRepository.count());
        stats.put("publishedPosts", postRepository.countByIsPublishedTrue());
        stats.put("featuredPosts", postRepository.findByIsFeaturedTrueAndIsPublishedTrueOrderByCreatedAtDesc().size());

        // Yorum istatistikleri
        stats.put("totalComments", commentRepository.count());

        // Son kayıt olanlar
        stats.put("recentUsers", userRepository.findUsersRegisteredSince(last7Days));

        // Kategori istatistikleri
        List<Object[]> categoryStats = postRepository.countByCategory();
        Map<String, Long> categoryMap = new LinkedHashMap<>();
        for (Object[] row : categoryStats) {
            if (row[0] != null) {
                categoryMap.put((String) row[0], (Long) row[1]);
            }
        }
        stats.put("categoryStats", categoryMap);

        return stats;
    }

    /**
     * Son 30 günlük kayıt grafiği verileri
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRegistrationChartData() {
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
        List<Object[]> data = userRepository.countRegistrationsByDay(last30Days);

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", row[0].toString());
            point.put("count", row[1]);
            chartData.add(point);
        }
        return chartData;
    }

    // ==================== ADMIN USERS ====================

    /**
     * Admin kullanıcılarını getir
     */
    @Transactional(readOnly = true)
    public List<User> getAdminUsers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(Role.ADMIN);
    }
}
