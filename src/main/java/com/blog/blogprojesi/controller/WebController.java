package com.blog.blogprojesi.controller;

import com.blog.blogprojesi.entity.*;
import com.blog.blogprojesi.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ana Web Controller
 * Tüm sayfa yönlendirmelerini ve form işlemlerini yönetir
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final RatingService ratingService;
    private final BookmarkService bookmarkService;
    private final PasswordEncoder passwordEncoder;

    // ==================== HELPER METHODS ====================

    /**
     * Giriş yapmış kullanıcıyı getir
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userService.findByUsername(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * Model'e ortak verileri ekle
     */
    private void addCommonAttributes(Model model) {
        User currentUser = getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAuthenticated", currentUser != null);
        if (currentUser != null) {
            model.addAttribute("userInitials", currentUser.getInitials());
        }
    }

    // ==================== PUBLIC PAGES ====================

    /**
     * Index sayfası - Login'e yönlendir
     */
    @GetMapping("/")
    public String index() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            return "redirect:/home";
        }
        return "index";
    }

    /**
     * Ana sayfa - Tüm gönderiler
     */
    @GetMapping("/home")
    public String home(Model model, 
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String sort) {
        addCommonAttributes(model);

        List<Post> posts;
        if (search != null && !search.isEmpty()) {
            posts = postService.searchPosts(search);
            model.addAttribute("searchQuery", search);
        } else if (category != null && !category.isEmpty()) {
            posts = postService.findPostsByCategory(category);
            model.addAttribute("selectedCategory", category);
        } else {
            posts = postService.findAllPublishedPosts();
        }

        // Sıralama uygula
        if (sort != null && !sort.isEmpty()) {
            switch (sort) {
                case "popular":
                    posts.sort((p1, p2) -> Long.compare(
                        p2.getViewCount() != null ? p2.getViewCount() : 0L,
                        p1.getViewCount() != null ? p1.getViewCount() : 0L
                    ));
                    break;
                case "top-rated":
                    posts.sort((p1, p2) -> {
                        double avg1 = p1.getAverageRating();
                        double avg2 = p2.getAverageRating();
                        return Double.compare(avg2, avg1);
                    });
                    break;
                default: // latest
                    posts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
                    break;
            }
        }

        // Post'lar için ek bilgiler
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            for (Post post : posts) {
                // Bookmark durumunu kontrol et
                boolean isBookmarked = bookmarkService.isBookmarked(currentUser, post.getId());
                model.addAttribute("bookmark_" + post.getId(), isBookmarked);
            }
        }

        model.addAttribute("posts", posts);
        model.addAttribute("featuredPosts", postService.findFeaturedPosts());
        model.addAttribute("categories", postService.findAllCategories());
        model.addAttribute("categoryCount", postService.getCategoryPostCounts());

        return "home";
    }

    /**
     * Konular sayfası
     */
    @GetMapping("/topics")
    public String topics(Model model) {
        addCommonAttributes(model);
        
        List<String> categories = postService.findAllCategories();
        Map<String, Long> categoryCount = postService.getCategoryPostCounts();
        
        // Eğer kategoriler boşsa varsayılan kategorileri ekle
        if (categories == null || categories.isEmpty()) {
            categories = java.util.Arrays.asList("Teknoloji", "Yazılım", "Web", "Mobil", "Yapay Zeka", "Tasarım", "Kişisel", "Genel");
        }
        
        // Her kategori için son yazıları getir
        Map<String, List<Post>> categoryRecentPosts = new java.util.HashMap<>();
        for (String category : categories) {
            List<Post> recentPosts = postService.findRecentPostsByCategory(category, 2);
            categoryRecentPosts.put(category, recentPosts);
            // categoryCount'a eklenmemişse 0 olarak ekle
            if (!categoryCount.containsKey(category)) {
                categoryCount.put(category, 0L);
            }
        }
        
        model.addAttribute("categories", categories);
        model.addAttribute("categoryCount", categoryCount);
        model.addAttribute("categoryRecentPosts", categoryRecentPosts);
        model.addAttribute("recentPosts", postService.findRecentPosts());
        model.addAttribute("totalTopics", categories.size());
        model.addAttribute("totalPosts", postService.getTotalPostCount());
        model.addAttribute("totalAuthors", postService.getTotalAuthorCount());
        
        return "topics";
    }

    // ==================== AUTHENTICATION ====================

    /**
     * Login sayfası
     */
    @GetMapping("/login")
    public String login(Model model, @RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout) {
        if (getCurrentUser() != null) {
            return "redirect:/home";
        }

        if (error != null) {
            model.addAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        }
        if (logout != null) {
            model.addAttribute("message", "Başarıyla çıkış yaptınız.");
        }

        return "login";
    }

    /**
     * Register sayfası
     */
    @GetMapping("/register")
    public String register(Model model) {
        if (getCurrentUser() != null) {
            return "redirect:/home";
        }
        return "register";
    }

    /**
     * Forgot Password sayfası
     */
    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        if (getCurrentUser() != null) {
            return "redirect:/home";
        }
        return "forgot-password";
    }

    /**
     * Forgot Password işlemi
     */
    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam String email,
                                      RedirectAttributes redirectAttributes) {
        try {
            // E-posta kontrolü
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı.");
                return "redirect:/forgot-password";
            }

            // Token oluştur
            String token = userService.createPasswordResetToken(email);
            
            // Gerçek uygulamada burada e-posta gönderilmeli
            // Şimdilik token'ı console'a yazdıralım
            String resetLink = "http://localhost:8080/reset-password?token=" + token + "&email=" + email;
            log.info("Password reset link: {}", resetLink);
            
            redirectAttributes.addFlashAttribute("message", 
                "Şifre sıfırlama talimatları " + email + " adresine gönderildi. (Geliştirme modunda - Console'a bakın)");
        } catch (Exception e) {
            log.error("Password reset error: ", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forgot-password";
    }

    /**
     * Reset Password sayfası
     */
    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                               @RequestParam String email,
                               Model model) {
        model.addAttribute("token", token);
        model.addAttribute("email", email);
        return "reset-password";
    }

    /**
     * Reset Password işlemi
     */
    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        try {
            // Şifre eşleşme kontrolü
            if (!password.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Şifreler eşleşmiyor!");
                redirectAttributes.addAttribute("token", token);
                redirectAttributes.addAttribute("email", email);
                return "redirect:/reset-password";
            }

            // Kullanıcıyı bul
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

            // TODO: Token validation ekle (expiration, database check)
            
            // Şifreyi güncelle
            userService.updatePassword(user, password);
            
            redirectAttributes.addFlashAttribute("message", 
                "Şifreniz başarıyla güncellendi. Giriş yapabilirsiniz.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Password reset submit error: ", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addAttribute("token", token);
            redirectAttributes.addAttribute("email", email);
            return "redirect:/reset-password";
        }
    }

    /**
     * Register işlemi
     */
    @PostMapping("/register")
    public String registerUser(@RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String username,
                              @RequestParam String email,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              RedirectAttributes redirectAttributes) {
        log.info("Register isteği alındı - Username: {}, Email: {}, FirstName: {}, LastName: {}", 
                username, email, firstName, lastName);
        try {
            // Şifre kontrolü
            if (!password.equals(confirmPassword)) {
                log.warn("Şifreler eşleşmiyor");
                redirectAttributes.addFlashAttribute("error", "Şifreler eşleşmiyor!");
                return "redirect:/register";
            }

            // Şifre uzunluğu kontrolü
            if (password.length() < 8) {
                log.warn("Şifre çok kısa: {} karakter", password.length());
                redirectAttributes.addFlashAttribute("error", "Şifre en az 8 karakter olmalıdır!");
                return "redirect:/register";
            }

            log.info("Validasyonlar geçildi, userService.registerUser çağrılıyor...");
            User savedUser = userService.registerUser(username, email, password, firstName, lastName);
            log.info("Kullanıcı başarıyla kaydedildi - ID: {}", savedUser.getId());
            
            redirectAttributes.addFlashAttribute("message", "Kayıt başarılı! Giriş yapabilirsiniz.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Kayıt hatası: ", e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    // ==================== POST OPERATIONS ====================

    /**
     * Yazı detay sayfası
     */
    @GetMapping("/post/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        Optional<Post> postOpt = postService.findByIdWithDetails(id);
        if (postOpt.isEmpty()) {
            return "redirect:/home";
        }

        Post post = postOpt.get();
        
        // Görüntüleme sayısını artır
        postService.incrementViewCount(id);

        // Yorumları getir
        List<Comment> comments = commentService.findCommentsByPostId(id);

        // Puanlama bilgileri
        double averageRating = ratingService.getAverageRating(id);
        long ratingCount = ratingService.getRatingCount(id);
        Map<Integer, Long> ratingDistribution = ratingService.getRatingDistribution(id);

        // Kullanıcı bilgileri
        User currentUser = getCurrentUser();
        Integer userRating = null;
        boolean isBookmarked = false;

        if (currentUser != null) {
            Optional<Rating> userRatingOpt = ratingService.getUserRatingForPost(currentUser, id);
            if (userRatingOpt.isPresent()) {
                userRating = userRatingOpt.get().getScore();
            }
            isBookmarked = bookmarkService.isBookmarked(currentUser, id);
        }

        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("averageRating", averageRating);
        model.addAttribute("ratingCount", ratingCount);
        model.addAttribute("ratingDistribution", ratingDistribution);
        model.addAttribute("userRating", userRating);
        model.addAttribute("isBookmarked", isBookmarked);
        model.addAttribute("isOwner", currentUser != null && 
                          post.getAuthor().getId().equals(currentUser.getId()));

        return "post";
    }

    /**
     * Yazı yazma sayfası
     */
    @GetMapping("/write")
    public String write(Model model) {
        addCommonAttributes(model);
        model.addAttribute("categories", postService.findAllCategories());
        return "write";
    }

    /**
     * Yazı oluşturma işlemi
     */
    @PostMapping("/write")
    public String createPost(@RequestParam String title,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) String url,
                            @RequestParam(required = false) String imageUrl,
                            @RequestParam String postType,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false, defaultValue = "true") boolean commentsEnabled,
                            @RequestParam(required = false, defaultValue = "false") boolean featured,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            PostType type = PostType.valueOf(postType.toUpperCase());
            
            // Link postu için URL kontrolü
            if (type == PostType.LINK && (url == null || url.trim().isEmpty())) {
                redirectAttributes.addFlashAttribute("error", "Link paylaşımı için URL gereklidir!");
                return "redirect:/write";
            }

            // Text postu için içerik kontrolü
            if (type == PostType.TEXT && (content == null || content.trim().isEmpty())) {
                redirectAttributes.addFlashAttribute("error", "Yazı içeriği boş olamaz!");
                return "redirect:/write";
            }
            
            // Image postu için imageUrl kontrolü
            if (type == PostType.IMAGE && (imageUrl == null || imageUrl.trim().isEmpty())) {
                redirectAttributes.addFlashAttribute("error", "Fotoğraf paylaşımı için görsel URL gereklidir!");
                return "redirect:/write";
            }

            Post post = postService.createPost(currentUser, title, content, url, type, 
                                               category, commentsEnabled, featured, imageUrl);
            
            redirectAttributes.addFlashAttribute("message", "Yazınız başarıyla yayınlandı!");
            return "redirect:/post/" + post.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Yazı oluşturulurken hata: " + e.getMessage());
            return "redirect:/write";
        }
    }

    /**
     * Taslak kaydetme işlemi
     */
    @PostMapping("/draft")
    public String saveDraft(@RequestParam String title,
                           @RequestParam(required = false) String content,
                           @RequestParam(required = false) String url,
                           @RequestParam(required = false) String imageUrl,
                           @RequestParam String postType,
                           @RequestParam(required = false) String category,
                           RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            PostType type = PostType.valueOf(postType.toUpperCase());
            
            Post draft = postService.saveDraft(title, content, category, url, imageUrl, type, currentUser);
            
            redirectAttributes.addFlashAttribute("message", "Taslak başarıyla kaydedildi!");
            return "redirect:/drafts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Taslak kaydedilirken hata: " + e.getMessage());
            return "redirect:/write";
        }
    }

    /**
     * Yazı düzenleme sayfası
     */
    @GetMapping("/post/{id}/edit")
    public String editPost(@PathVariable Long id, Model model) {
        addCommonAttributes(model);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<Post> postOpt = postService.findById(id);
        if (postOpt.isEmpty()) {
            return "redirect:/home";
        }

        Post post = postOpt.get();
        
        // Sadece yazar düzenleyebilir
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            return "redirect:/home";
        }

        model.addAttribute("post", post);
        model.addAttribute("categories", postService.findAllCategories());
        model.addAttribute("isEdit", true);
        
        return "write";
    }

    /**
     * Yazı güncelleme işlemi
     */
    @PostMapping("/post/{id}/edit")
    public String updatePost(@PathVariable Long id,
                            @RequestParam String title,
                            @RequestParam(required = false) String content,
                            @RequestParam(required = false) String url,
                            @RequestParam(required = false) String imageUrl,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false, defaultValue = "true") boolean commentsEnabled,
                            @RequestParam(required = false, defaultValue = "false") boolean featured,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<Post> postOpt = postService.findById(id);
            if (postOpt.isEmpty() || !postOpt.get().getAuthor().getId().equals(currentUser.getId())) {
                return "redirect:/home";
            }

            postService.updatePost(id, title, content, url, category, commentsEnabled, featured, imageUrl);
            redirectAttributes.addFlashAttribute("message", "Yazınız başarıyla güncellendi!");
            return "redirect:/post/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Güncelleme hatası: " + e.getMessage());
            return "redirect:/post/" + id + "/edit";
        }
    }

    /**
     * Yazı silme işlemi
     */
    @PostMapping("/post/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<Post> postOpt = postService.findById(id);
            if (postOpt.isEmpty() || !postOpt.get().getAuthor().getId().equals(currentUser.getId())) {
                return "redirect:/home";
            }

            postService.deletePost(id);
            redirectAttributes.addFlashAttribute("message", "Yazınız silindi.");
            return "redirect:/my-posts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Silme hatası: " + e.getMessage());
            return "redirect:/post/" + id;
        }
    }

    /**
     * Yazı yayın durumunu değiştirme (taslak <-> yayınlanmış)
     */
    @PostMapping("/post/{id}/toggle-publish")
    public String togglePublishStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<Post> postOpt = postService.findById(id);
            if (postOpt.isEmpty() || !postOpt.get().getAuthor().getId().equals(currentUser.getId())) {
                return "redirect:/home";
            }

            Post post = postService.togglePublishStatus(id);
            
            if (post.isPublished()) {
                redirectAttributes.addFlashAttribute("message", "Yazınız başarıyla yayınlandı!");
                return "redirect:/post/" + id;
            } else {
                redirectAttributes.addFlashAttribute("message", "Yazınız taslak olarak kaydedildi.");
                return "redirect:/drafts";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "İşlem hatası: " + e.getMessage());
            return "redirect:/my-posts";
        }
    }

    // ==================== COMMENT OPERATIONS ====================

    /**
     * Yorum ekleme işlemi (AJAX)
     */
    @PostMapping("/post/{postId}/comment")
    @ResponseBody
    public Map<String, Object> addComment(@PathVariable Long postId,
                            @RequestParam String content) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Yorum yapmak için giriş yapmalısınız.");
                return response;
            }

            Comment comment = commentService.addComment(currentUser, postId, content);
            response.put("success", true);
            response.put("message", "Yorumunuz eklendi.");
            response.put("comment", comment);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    /**
     * Yorum silme işlemi
     */
    @PostMapping("/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                               @RequestParam Long postId,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            commentService.deleteComment(commentId, currentUser);
            redirectAttributes.addFlashAttribute("message", "Yorum silindi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/post/" + postId;
    }

    // ==================== RATING OPERATIONS ====================

    /**
     * Puanlama işlemi (AJAX)
     */
    @PostMapping("/post/{postId}/rate")
    @ResponseBody
    public Map<String, Object> ratePost(@PathVariable Long postId,
                          @RequestParam int score) {
        Map<String, Object> response = new HashMap<>();
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "Puan vermek için giriş yapmalısınız.");
                return response;
            }

            ratingService.ratePost(currentUser, postId, score);
            
            // Güncel ortalama ve sayıyı getir
            Post post = postService.findById(postId).orElseThrow(() -> 
                new RuntimeException("Post bulunamadı"));
            response.put("success", true);
            response.put("message", "Puanınız kaydedildi.");
            response.put("averageRating", post.getAverageRating());
            response.put("ratingCount", post.getRatingCount());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return response;
    }

    // ==================== BOOKMARK OPERATIONS ====================

    /**
     * Yer imi toggle işlemi
     */
    @PostMapping("/post/{postId}/bookmark")
    public String toggleBookmark(@PathVariable Long postId, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Yer imi eklemek için giriş yapmalısınız.");
                return "redirect:/login";
            }

            boolean added = bookmarkService.toggleBookmark(currentUser, postId);
            if (added) {
                redirectAttributes.addFlashAttribute("message", "Yer imlerine eklendi.");
            } else {
                redirectAttributes.addFlashAttribute("message", "Yer imlerinden çıkarıldı.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/post/" + postId;
    }

    /**
     * Yer imleri sayfası
     */
    @GetMapping("/bookmarks")
    public String bookmarks(Model model) {
        addCommonAttributes(model);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Post> bookmarkedPosts = bookmarkService.getBookmarkedPosts(currentUser);
        model.addAttribute("posts", bookmarkedPosts);
        model.addAttribute("pageTitle", "Yer İmlerim");

        return "bookmarks";
    }

    // ==================== USER PAGES ====================

    /**
     * Yazılarım sayfası
     */
    @GetMapping("/my-posts")
    public String myPosts(Model model, @RequestParam(required = false) String category) {
        addCommonAttributes(model);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Post> allPosts = postService.findPostsByAuthor(currentUser);
        List<Post> posts;
        
        // Kategori filtreleme
        if (category != null && !category.isEmpty()) {
            posts = allPosts.stream()
                    .filter(p -> category.equals(p.getCategory()))
                    .toList();
            model.addAttribute("selectedCategory", category);
        } else {
            posts = allPosts;
        }
        
        long draftCount = postService.countDraftsByAuthor(currentUser);
        Map<String, Long> categoryCounts = postService.getCategoryPostCountsByAuthor(currentUser);
        
        model.addAttribute("posts", posts);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("categoryCounts", categoryCounts);
        model.addAttribute("pageTitle", "Yazılarım");

        return "my-posts";
    }

    /**
     * Taslaklar sayfası
     */
    @GetMapping("/drafts")
    public String drafts(Model model) {
        addCommonAttributes(model);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<Post> drafts = postService.findDraftsByAuthor(currentUser);
        model.addAttribute("drafts", drafts);
        model.addAttribute("pageTitle", "Taslaklarım");

        return "drafts";
    }

    /**
     * Profil sayfası - Kendi profiline yönlendir
     */
    @GetMapping("/profile")
    public String profile() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        return "redirect:/profile/" + currentUser.getUsername();
    }

    /**
     * Kullanıcı profili
     */
    @GetMapping("/profile/{username}")
    public String userProfile(@PathVariable String username, Model model) {
        addCommonAttributes(model);

        Optional<User> userOpt = userService.findByUsername(username);
        if (userOpt.isEmpty()) {
            return "redirect:/home";
        }

        User profileUser = userOpt.get();
        User currentUser = getCurrentUser();
        
        // Kendi profiliyse tüm postları göster, başkasının profiliyse sadece yayınlanmışları
        List<Post> userPosts;
        if (currentUser != null && currentUser.getUsername().equals(username)) {
            userPosts = postService.findPostsByAuthor(profileUser);
            model.addAttribute("isOwnProfile", true);
        } else {
            userPosts = postService.findPublishedPostsByAuthor(profileUser);
            model.addAttribute("isOwnProfile", false);
        }

        model.addAttribute("user", profileUser);
        model.addAttribute("posts", userPosts);

        return "profile";
    }

    /**
     * Ayarlar sayfası
     */
    @GetMapping("/settings")
    public String settings(Model model) {
        addCommonAttributes(model);

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        return "settings";
    }

    /**
     * Profil ayarları (alternatif URL)
     */
    @GetMapping("/profile/settings")
    public String profileSettings(Model model) {
        return settings(model);
    }

    /**
     * Profil güncelleme işlemi
     */
    @PostMapping("/settings/profile")
    public String updateProfile(@RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam(required = false) String bio,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            // Kullanıcı bilgilerini güncelle
            userService.updateUserFull(currentUser.getId(), firstName, lastName, email, bio, currentUser.getProfileImageUrl());
            redirectAttributes.addFlashAttribute("successMessage", "Profiliniz başarıyla güncellendi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settings";
    }

    /**
     * Şifre değiştirme işlemi
     */
    @PostMapping("/settings/password")
    public String changePassword(@RequestParam String currentPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return "redirect:/login";
            }

            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Yeni şifreler eşleşmiyor!");
                return "redirect:/settings";
            }

            if (newPassword.length() < 8) {
                redirectAttributes.addFlashAttribute("errorMessage", "Şifre en az 8 karakter olmalıdır!");
                return "redirect:/settings";
            }

            userService.changePassword(currentUser.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Şifreniz başarıyla değiştirildi.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settings";
    }

    /**
     * Profil resmi yükleme (AJAX)
     */
    @PostMapping("/settings/upload-image")
    @ResponseBody
    public Map<String, Object> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            // Dosya kontrolü
            if (file.isEmpty()) {
                return Map.of("success", false, "message", "Dosya seçilmedi");
            }

            // Dosya boyutu kontrolü (5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return Map.of("success", false, "message", "Dosya boyutu 5MB'dan küçük olmalıdır");
            }

            // Dosya türü kontrolü
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Map.of("success", false, "message", "Sadece resim dosyaları yüklenebilir");
            }

            // Gerçek uygulamada dosyayı bir sunucuya veya cloud storage'a yüklersiniz
            // Şimdilik sadece filename'i kaydedelim
            String imageUrl = "https://ui-avatars.com/api/?name=" + 
                            currentUser.getFirstName() + "+" + currentUser.getLastName() + 
                            "&size=200&background=6366f1&color=fff&bold=true";
            
            userService.updateUser(currentUser.getId(), currentUser.getEmail(), 
                                 currentUser.getBio(), imageUrl);

            return Map.of("success", true, "imageUrl", imageUrl);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Profil resmini kaldırma (AJAX)
     */
    @PostMapping("/settings/remove-image")
    @ResponseBody
    public Map<String, Object> removeProfileImage() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            userService.updateUser(currentUser.getId(), currentUser.getEmail(), 
                                 currentUser.getBio(), null);

            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Profil resmi URL ile güncelleme (AJAX)
     */
    @PostMapping("/settings/profile-image-url")
    @ResponseBody
    public Map<String, Object> updateProfileImageUrl(@RequestBody Map<String, String> request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            String imageUrl = request.get("imageUrl");
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return Map.of("success", false, "message", "Geçerli bir URL girin");
            }

            userService.updateUser(currentUser.getId(), currentUser.getEmail(), 
                                 currentUser.getBio(), imageUrl.trim());

            return Map.of("success", true, "imageUrl", imageUrl);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Gizlilik ayarları güncelleme (AJAX)
     */
    @PostMapping("/settings/privacy")
    @ResponseBody
    public Map<String, Object> updatePrivacySettings(@RequestBody Map<String, Object> settings) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            // Gizlilik ayarlarını kaydet
            Boolean profileVisibility = (Boolean) settings.get("profileVisibility");
            Boolean emailVisibility = (Boolean) settings.get("emailVisibility");
            Boolean activityStatus = (Boolean) settings.get("activityStatus");

            // TODO: Gerçek uygulamada bu ayarları User entity'sine ekleyip veritabanına kaydedin
            log.info("Privacy settings saved for user {}: profileVisibility={}, emailVisibility={}, activityStatus={}",
                    currentUser.getUsername(), profileVisibility, emailVisibility, activityStatus);
            
            return Map.of("success", true, "message", "Gizlilik ayarları kaydedildi");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Bildirim ayarları güncelleme (AJAX)
     */
    @PostMapping("/settings/notifications")
    @ResponseBody
    public Map<String, Object> updateNotificationSettings(@RequestBody Map<String, Object> settings) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            // Bildirim ayarlarını kaydet
            Boolean commentNotifications = (Boolean) settings.get("commentNotifications");
            Boolean ratingNotifications = (Boolean) settings.get("ratingNotifications");
            Boolean followerNotifications = (Boolean) settings.get("followerNotifications");
            Boolean weeklyDigest = (Boolean) settings.get("weeklyDigest");

            // TODO: Gerçek uygulamada bu ayarları User entity'sine ekleyip veritabanına kaydedin
            log.info("Notification settings saved for user {}: comments={}, ratings={}, followers={}, weekly={}",
                    currentUser.getUsername(), commentNotifications, ratingNotifications, followerNotifications, weeklyDigest);
            
            return Map.of("success", true, "message", "Bildirim ayarları kaydedildi");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Hesap silme (AJAX)
     */
    @PostMapping("/settings/delete-account")
    @ResponseBody
    public Map<String, Object> deleteAccount(@RequestBody Map<String, String> request) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            String password = request.get("password");
            
            // Şifre kontrolü
            if (!passwordEncoder.matches(password, currentUser.getPassword())) {
                return Map.of("success", false, "message", "Şifre yanlış");
            }

            // Kullanıcıyı sil
            userService.deleteUser(currentUser.getId());

            // Oturumu kapat
            SecurityContextHolder.clearContext();

            return Map.of("success", true);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // ==================== API ENDPOINTS ====================

    /**
     * Kullanıcı adı müsait mi kontrolü (AJAX)
     */
    @GetMapping("/api/check-username")
    @ResponseBody
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return Map.of("available", available);
    }

    /**
     * E-posta müsait mi kontrolü (AJAX)
     */
    @GetMapping("/api/check-email")
    @ResponseBody
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return Map.of("available", available);
    }

    /**
     * Puanlama API (AJAX)
     */
    @PostMapping("/api/post/{postId}/rate")
    @ResponseBody
    public Map<String, Object> ratePostApi(@PathVariable Long postId, @RequestParam int score) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            ratingService.ratePost(currentUser, postId, score);
            double newAverage = ratingService.getAverageRating(postId);
            long newCount = ratingService.getRatingCount(postId);

            return Map.of(
                "success", true,
                "averageRating", newAverage,
                "ratingCount", newCount,
                "userRating", score
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Yer imi toggle API (AJAX)
     */
    @PostMapping("/api/post/{postId}/bookmark")
    @ResponseBody
    public Map<String, Object> toggleBookmarkApi(@PathVariable Long postId) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            boolean isBookmarked = bookmarkService.toggleBookmark(currentUser, postId);
            return Map.of("success", true, "isBookmarked", isBookmarked);
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    /**
     * Yorum ekleme API (AJAX)
     */
    @PostMapping("/api/post/{postId}/comment")
    @ResponseBody
    public Map<String, Object> addCommentApi(@PathVariable Long postId, @RequestParam String content) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) {
                return Map.of("success", false, "message", "Giriş yapmalısınız");
            }

            Comment comment = commentService.addComment(currentUser, postId, content);
            return Map.of(
                "success", true,
                "commentId", comment.getId(),
                "username", currentUser.getUsername(),
                "userInitials", currentUser.getInitials(),
                "createdAt", comment.getRelativeTime(),
                "content", comment.getContent()
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // ==================== ERROR PAGES ====================

    /**
     * Erişim engellendi sayfası
     */
    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        addCommonAttributes(model);
        return "access-denied";
    }
}
