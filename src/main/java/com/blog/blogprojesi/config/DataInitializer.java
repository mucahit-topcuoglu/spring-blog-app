package com.blog.blogprojesi.config;

import com.blog.blogprojesi.entity.*;
import com.blog.blogprojesi.repository.*;
import com.blog.blogprojesi.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Uygulama başlatıldığında örnek veriler oluşturur
 * İlk çalıştırmada test için kullanışlıdır
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingsService systemSettingsService;

    @Override
    public void run(String... args) {
        // Sistem ayarlarını başlat
        log.info("Sistem ayarları kontrol ediliyor...");
        systemSettingsService.initializeDefaultSettings();
        log.info("Sistem ayarları hazır.");

        // Eğer veritabanında kullanıcı yoksa örnek veriler oluştur
        if (userRepository.count() == 0) {
            log.info("Veritabanı boş, örnek veriler oluşturuluyor...");
            createSampleData();
            log.info("Örnek veriler başarıyla oluşturuldu!");
        } else {
            log.info("Veritabanında mevcut veriler bulundu, örnek veri oluşturma atlandı.");
            // Admin kullanıcısının var olduğundan emin ol
            ensureAdminExists();
        }
    }

    /**
     * En az bir admin kullanıcısının var olduğundan emin ol
     */
    private void ensureAdminExists() {
        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount == 0) {
            log.warn("Sistemde admin kullanıcısı bulunamadı, yeni admin oluşturuluyor...");
            User admin = User.builder()
                    .username("admin")
                    .email("admin@blog.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("User")
                    .role(Role.ADMIN)
                    .bio("Blog yöneticisi")
                    .isEnabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Admin kullanıcısı oluşturuldu: admin / admin123");
        }
    }

    private void createSampleData() {
        // Admin kullanıcısı oluştur
        User admin = User.builder()
                .username("admin")
                .email("admin@blog.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .role(Role.ADMIN)
                .bio("Blog yöneticisi")
                .isEnabled(true)
                .build();
        userRepository.save(admin);
        log.info("Admin kullanıcısı oluşturuldu: admin / admin123");

        // Normal kullanıcı oluştur
        User user1 = User.builder()
                .username("ahmet")
                .email("ahmet@blog.com")
                .password(passwordEncoder.encode("ahmet123"))
                .firstName("Ahmet")
                .lastName("Yılmaz")
                .role(Role.USER)
                .bio("Yazılım geliştirici ve blog yazarı")
                .isEnabled(true)
                .build();
        userRepository.save(user1);
        log.info("Kullanıcı oluşturuldu: ahmet / ahmet123");

        User user2 = User.builder()
                .username("zeynep")
                .email("zeynep@blog.com")
                .password(passwordEncoder.encode("zeynep123"))
                .firstName("Zeynep")
                .lastName("Kaya")
                .role(Role.USER)
                .bio("Teknoloji meraklısı")
                .isEnabled(true)
                .build();
        userRepository.save(user2);
        log.info("Kullanıcı oluşturuldu: zeynep / zeynep123");

        // Örnek TEXT postlar
        Post post1 = Post.builder()
                .title("Spring Boot ile Web Geliştirme")
                .content("Spring Boot, Java tabanlı web uygulamaları geliştirmek için harika bir framework'tür.\n\n" +
                        "## Neden Spring Boot?\n\n" +
                        "1. **Hızlı başlangıç**: Auto-configuration sayesinde minimum konfigürasyon ile çalışmaya başlayabilirsiniz.\n" +
                        "2. **Gömülü sunucu**: Tomcat, Jetty veya Undertow ile birlikte gelir.\n" +
                        "3. **Production-ready**: Actuator ile monitoring ve yönetim özellikleri sunar.\n\n" +
                        "Spring Boot ile geliştirme yapmak gerçekten keyifli!")
                .postType(PostType.TEXT)
                .category("Teknoloji")
                .author(user1)
                .isPublished(true)
                .isFeatured(true)
                .commentsEnabled(true)
                .build();
        postRepository.save(post1);

        Post post2 = Post.builder()
                .title("PostgreSQL Veritabanı Optimizasyonu")
                .content("PostgreSQL performansını artırmak için kullanabileceğiniz teknikler:\n\n" +
                        "## 1. Index Kullanımı\n\n" +
                        "Sık sorguladığınız alanlar için index oluşturun.\n\n" +
                        "## 2. EXPLAIN ANALYZE\n\n" +
                        "Sorgularınızın performansını analiz edin.\n\n" +
                        "## 3. Connection Pooling\n\n" +
                        "HikariCP gibi connection pool kütüphaneleri kullanın.\n\n" +
                        "Bu teknikler uygulamanızın veritabanı performansını önemli ölçüde artıracaktır.")
                .postType(PostType.TEXT)
                .category("Veritabanı")
                .author(admin)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(post2);

        Post post3 = Post.builder()
                .title("Modern CSS Teknikleri")
                .content("CSS ile yapabileceğiniz harika şeyler:\n\n" +
                        "## Flexbox\n\n" +
                        "Esnek düzenler için flexbox kullanın.\n\n" +
                        "## CSS Grid\n\n" +
                        "Karmaşık grid layoutları için CSS Grid kullanın.\n\n" +
                        "## CSS Variables\n\n" +
                        "Tema değişkenleri için CSS Custom Properties kullanın.\n\n" +
                        "Modern CSS özellikleri ile responsive ve güzel tasarımlar oluşturabilirsiniz!")
                .postType(PostType.TEXT)
                .category("Web Tasarım")
                .author(user2)
                .isPublished(true)
                .isFeatured(true)
                .commentsEnabled(true)
                .build();
        postRepository.save(post3);

        // Örnek LINK post
        Post linkPost = Post.builder()
                .title("Spring Boot Resmi Dokümantasyonu")
                .content("Spring Boot'un resmi dokümantasyonu için harika bir kaynak. Başlangıç rehberleri ve detaylı API referansları içerir.")
                .url("https://spring.io/projects/spring-boot")
                .postType(PostType.LINK)
                .category("Kaynaklar")
                .author(user1)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(linkPost);

        log.info("4 örnek post oluşturuldu.");
    }
}
