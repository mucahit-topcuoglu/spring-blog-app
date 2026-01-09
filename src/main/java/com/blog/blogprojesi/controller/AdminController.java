package com.blog.blogprojesi.controller;

import com.blog.blogprojesi.entity.*;
import com.blog.blogprojesi.entity.AdminLog.AdminActionType;
import com.blog.blogprojesi.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin Controller
 * Admin paneli için tüm endpoint'leri yönetir
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final AdminLogService adminLogService;
    private final SystemSettingsService systemSettingsService;
    private final LoginAttemptService loginAttemptService;
    private final UserService userService;
    private final PostService postService;

    // ==================== HELPER METHODS ====================

    /**
     * Giriş yapmış admin kullanıcısını getir
     */
    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userService.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * Model'e ortak admin verilerini ekle
     */
    private void addCommonAttributes(Model model) {
        User currentAdmin = getCurrentAdmin();
        model.addAttribute("currentAdmin", currentAdmin);
        model.addAttribute("siteName", systemSettingsService.getSiteName());
        model.addAttribute("isMaintenanceMode", systemSettingsService.isMaintenanceMode());
    }

    /**
     * IP adresini al
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ==================== ADMIN LOGIN ====================

    /**
     * Admin login sayfası
     */
    @GetMapping("/login")
    public String adminLoginPage(Model model,
                                  @RequestParam(required = false) String error,
                                  @RequestParam(required = false) String logout,
                                  @RequestParam(required = false) String blocked,
                                  HttpServletRequest request) {
        User currentUser = getCurrentAdmin();
        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }

        if (error != null) {
            model.addAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        }
        if (logout != null) {
            model.addAttribute("message", "Başarıyla çıkış yaptınız.");
        }
        if (blocked != null) {
            model.addAttribute("error", "Çok fazla başarısız giriş denemesi. Lütfen daha sonra tekrar deneyiniz.");
        }

        model.addAttribute("siteName", systemSettingsService.getSiteName());
        return "admin/login";
    }

    // ==================== DASHBOARD ====================

    /**
     * Admin dashboard
     */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        addCommonAttributes(model);

        Map<String, Object> stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);

        List<AdminLog> recentLogs = adminLogService.getRecentLogs();
        model.addAttribute("recentLogs", recentLogs);

        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Kullanıcı listesi
     */
    @GetMapping("/users")
    public String userList(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(required = false) String search) {
        addCommonAttributes(model);

        if (search != null && !search.isEmpty()) {
            List<User> users = adminService.searchUsers(search);
            model.addAttribute("users", users);
            model.addAttribute("searchQuery", search);
        } else {
            Page<User> usersPage = adminService.getAllUsers(page, size);
            model.addAttribute("users", usersPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", usersPage.getTotalPages());
            model.addAttribute("totalElements", usersPage.getTotalElements());
        }

        model.addAttribute("activePage", "users");
        return "admin/users";
    }

    /**
     * Kullanıcı detay
     */
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Optional<User> userOpt = adminService.getUserById(id);
        if (userOpt.isEmpty()) {
            return "redirect:/admin/users?error=notfound";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("userLogs", adminLogService.getLogsByTarget("USER", id));
        model.addAttribute("activePage", "users");
        return "admin/user-detail";
    }

    /**
     * Yeni kullanıcı formu
     */
    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        addCommonAttributes(model);
        model.addAttribute("roles", Role.values());
        model.addAttribute("activePage", "users");
        return "admin/user-form";
    }

    /**
     * Yeni kullanıcı oluştur
     */
    @PostMapping("/users/new")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             @RequestParam(required = false) String firstName,
                             @RequestParam(required = false) String lastName,
                             @RequestParam String role,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);
            Role userRole = Role.valueOf(role);

            adminService.createUser(username, email, password, firstName, lastName, userRole, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Kullanıcı başarıyla oluşturuldu.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    /**
     * Kullanıcı düzenleme formu
     */
    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Optional<User> userOpt = adminService.getUserById(id);
        if (userOpt.isEmpty()) {
            return "redirect:/admin/users?error=notfound";
        }

        model.addAttribute("user", userOpt.get());
        model.addAttribute("roles", Role.values());
        model.addAttribute("activePage", "users");
        return "admin/user-edit";
    }

    /**
     * Kullanıcı güncelle
     */
    @PostMapping("/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String email,
                             @RequestParam(required = false) String firstName,
                             @RequestParam(required = false) String lastName,
                             @RequestParam(required = false) String bio,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            adminService.updateUser(id, email, firstName, lastName, bio, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Kullanıcı başarıyla güncellendi.");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    /**
     * Kullanıcı aktif/pasif toggle
     */
    @PostMapping("/users/{id}/toggle-enabled")
    public String toggleUserEnabled(@PathVariable Long id,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            User user = adminService.toggleUserEnabled(id, admin, ip);
            String message = user.isEnabled() ? "Kullanıcı aktif edildi." : "Kullanıcı pasif yapıldı.";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    /**
     * Kullanıcı rolü değiştir
     */
    @PostMapping("/users/{id}/change-role")
    public String changeUserRole(@PathVariable Long id,
                                 @RequestParam String role,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            Role newRole = Role.valueOf(role);
            adminService.changeUserRole(id, newRole, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Kullanıcı rolü değiştirildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    /**
     * Kullanıcı şifre sıfırla
     */
    @PostMapping("/users/{id}/reset-password")
    public String resetUserPassword(@PathVariable Long id,
                                    @RequestParam String newPassword,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            adminService.resetUserPassword(id, newPassword, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Şifre başarıyla sıfırlandı.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    /**
     * Kullanıcı sil
     */
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            adminService.deleteUser(id, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Kullanıcı başarıyla silindi.");
            return "redirect:/admin/users";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
            return "redirect:/admin/users/" + id;
        }
    }

    // ==================== POST MANAGEMENT ====================

    /**
     * Post listesi
     */
    @GetMapping("/posts")
    public String postList(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        addCommonAttributes(model);

        Page<Post> postsPage = adminService.getAllPosts(page, size);
        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postsPage.getTotalPages());
        model.addAttribute("totalElements", postsPage.getTotalElements());

        model.addAttribute("activePage", "posts");
        return "admin/posts";
    }

    /**
     * Post detay
     */
    @GetMapping("/posts/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Optional<Post> postOpt = adminService.getPostById(id);
        if (postOpt.isEmpty()) {
            return "redirect:/admin/posts?error=notfound";
        }

        model.addAttribute("post", postOpt.get());
        model.addAttribute("postLogs", adminLogService.getLogsByTarget("POST", id));
        model.addAttribute("activePage", "posts");
        return "admin/post-detail";
    }

    /**
     * Post yayınla/gizle toggle
     */
    @PostMapping("/posts/{id}/toggle-published")
    public String togglePostPublished(@PathVariable Long id,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            Post post = adminService.togglePostPublished(id, admin, ip);
            String message = post.isPublished() ? "Post yayınlandı." : "Post gizlendi.";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/posts/" + id;
    }

    /**
     * Post öne çıkar toggle
     */
    @PostMapping("/posts/{id}/toggle-featured")
    public String togglePostFeatured(@PathVariable Long id,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            Post post = adminService.togglePostFeatured(id, admin, ip);
            String message = post.isFeatured() ? "Post öne çıkarıldı." : "Post öne çıkarmadan kaldırıldı.";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/posts/" + id;
    }

    /**
     * Post sil
     */
    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            adminService.deletePost(id, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Post başarıyla silindi.");
            return "redirect:/admin/posts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
            return "redirect:/admin/posts/" + id;
        }
    }

    // ==================== COMMENT MANAGEMENT ====================

    /**
     * Yorum listesi
     */
    @GetMapping("/comments")
    public String commentList(Model model) {
        addCommonAttributes(model);

        List<Comment> comments = adminService.getAllComments();
        model.addAttribute("comments", comments);
        model.addAttribute("activePage", "comments");
        return "admin/comments";
    }

    /**
     * Yorum sil
     */
    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String ip = getClientIp(request);

            adminService.deleteComment(id, admin, ip);
            redirectAttributes.addFlashAttribute("success", "Yorum başarıyla silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/comments";
    }

    // ==================== REPORTS ====================

    /**
     * Raporlar sayfası
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        addCommonAttributes(model);

        Map<String, Object> stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("registrationChartData", adminService.getRegistrationChartData());

        model.addAttribute("activePage", "reports");
        return "admin/reports";
    }

    // ==================== SETTINGS ====================

    /**
     * Ayarlar sayfası
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        addCommonAttributes(model);

        Map<String, String> settings = systemSettingsService.getSettingsAsMap();
        model.addAttribute("settings", settings);

        model.addAttribute("activePage", "settings");
        return "admin/settings";
    }

    /**
     * Ayarları kaydet
     */
    @PostMapping("/settings")
    public String saveSettings(@RequestParam String siteName,
                               @RequestParam(required = false) String siteDescription,
                               @RequestParam(required = false, defaultValue = "false") boolean maintenanceMode,
                               @RequestParam(required = false) String maintenanceMessage,
                               @RequestParam(required = false, defaultValue = "true") boolean registrationEnabled,
                               @RequestParam(required = false, defaultValue = "true") boolean commentsEnabled,
                               @RequestParam(required = false, defaultValue = "5") int maxLoginAttempts,
                               @RequestParam(required = false, defaultValue = "30") int lockoutDuration,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            User admin = getCurrentAdmin();
            String username = admin.getUsername();
            String ip = getClientIp(request);

            // Ayarları güncelle
            String oldSiteName = systemSettingsService.getSiteName();
            systemSettingsService.setSiteName(siteName, username);
            if (!oldSiteName.equals(siteName)) {
                adminLogService.logSettingsUpdate(admin, "site_name", oldSiteName, siteName, ip);
            }

            systemSettingsService.setSetting(SystemSettings.KEY_SITE_DESCRIPTION, siteDescription, username);
            
            boolean wasMaintenanceMode = systemSettingsService.isMaintenanceMode();
            systemSettingsService.setMaintenanceMode(maintenanceMode, username);
            if (wasMaintenanceMode != maintenanceMode) {
                AdminActionType actionType = maintenanceMode ? 
                    AdminActionType.MAINTENANCE_MODE_ON : AdminActionType.MAINTENANCE_MODE_OFF;
                adminLogService.logAction(admin, actionType, 
                    maintenanceMode ? "Bakım modu açıldı" : "Bakım modu kapatıldı", 
                    "SYSTEM", null, null, ip);
            }

            systemSettingsService.setSetting(SystemSettings.KEY_MAINTENANCE_MESSAGE, maintenanceMessage, username);
            systemSettingsService.setBooleanSetting(SystemSettings.KEY_REGISTRATION_ENABLED, registrationEnabled, username);
            systemSettingsService.setBooleanSetting(SystemSettings.KEY_COMMENTS_ENABLED, commentsEnabled, username);
            systemSettingsService.setIntSetting(SystemSettings.KEY_MAX_LOGIN_ATTEMPTS, maxLoginAttempts, username);
            systemSettingsService.setIntSetting(SystemSettings.KEY_LOCKOUT_DURATION_MINUTES, lockoutDuration, username);

            redirectAttributes.addFlashAttribute("success", "Ayarlar başarıyla kaydedildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Hata: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }

    // ==================== LOGS ====================

    /**
     * Log listesi
     */
    @GetMapping("/logs")
    public String logs(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "50") int size) {
        addCommonAttributes(model);

        Page<AdminLog> logsPage = adminLogService.getLogs(page, size);
        model.addAttribute("logs", logsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", logsPage.getTotalPages());
        model.addAttribute("totalElements", logsPage.getTotalElements());

        model.addAttribute("activePage", "logs");
        return "admin/logs";
    }
}
