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

        // Örnek kullanıcılar
        User user1 = User.builder()
                .username("mucahit")
                .email("mucahit@blog.com")
                .password(passwordEncoder.encode("123456"))
                .firstName("Mücahit")
                .lastName("Topçuoğlu")
                .role(Role.USER)
                .bio("Yazılım öğrencisi, web geliştirme ve AI ile ilgileniyorum")
                .isEnabled(true)
                .build();
        userRepository.save(user1);
        log.info("Kullanıcı oluşturuldu: mucahit / 123456");

        User user2 = User.builder()
                .username("suna")
                .email("suna@blog.com")
                .password(passwordEncoder.encode("123456"))
                .firstName("Suna")
                .lastName("Şalgalı")
                .role(Role.USER)
                .bio("Öğrenci - Teknoloji ve tasarım tutkunu")
                .isEnabled(true)
                .build();
        userRepository.save(user2);
        log.info("Kullanıcı oluşturuldu: suna / 123456");

        User user3 = User.builder()
                .username("deneme")
                .email("deneme@blog.com")
                .password(passwordEncoder.encode("123456"))
                .firstName("Deneme")
                .lastName("Kullanıcı")
                .role(Role.USER)
                .bio("Test kullanıcısı")
                .isEnabled(true)
                .build();
        userRepository.save(user3);
        log.info("Kullanıcı oluşturuldu: deneme / 123456");

        // Örnek blog postları
        Post post1 = Post.builder()
                .title("İlk Deneme Yazım - Railway Deploy Süreci")
                .content("Bugün ilk kez Railway'e proje deploy ettim. Sanırım başarılı oldu!\n\n" +
                        "## Yaşadığım Sorunlar\n\n" +
                        "PostgreSQL bağlantısı kurarken biraz zorlandım ama sonunda çözdüm. Environment variable'ları " +
                        "doğru ayarlamak çok önemliymiş. PGHOST, PGPORT gibi değişkenleri tek tek eklemek gerekiyormuş.\n\n" +
                        "## Öğrendiklerim\n\n" +
                        "- Railway ücretsiz plan 500 saat/ay sunuyormuş\n" +
                        "- GitHub'a push yapınca otomatik deploy oluyor\n" +
                        "- PostgreSQL içinde hazır geliyor\n\n" +
                        "Şimdi sırada projeye yeni özellikler eklemek var!")
                .postType(PostType.TEXT)
                .category("Deneyimler")
                .author(user1)
                .isPublished(true)
                .isFeatured(true)
                .commentsEnabled(true)
                .build();
        postRepository.save(post1);

        Post post2 = Post.builder()
                .title("Spring Boot Öğrenirken Notlarım")
                .content("Spring Boot öğrenmeye yeni başladım. İlk izlenimlerim:\n\n" +
                        "**Artıları:**\n" +
                        "- Auto-configuration çok pratik\n" +
                        "- Embedded Tomcat sayesinde ekstra server kurmaya gerek yok\n" +
                        "- JPA ile database işlemleri çok kolay\n\n" +
                        "**Zorluklar:**\n" +
                        "- Annotation'lar başta kafa karıştırıcı olabiliyor\n" +
                        "- Security konfigürasyonu biraz karmaşık\n\n" +
                        "Ama genel olarak öğrenmesi keyifli bir framework!")
                .postType(PostType.TEXT)
                .category("Öğrenim")
                .author(user2)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(post2);

        Post post3 = Post.builder()
                .title("CSS Grid ile Layout Denemeleri")
                .content("Bu hafta CSS Grid öğrenmeye çalışıyorum. Flexbox'tan sonra Grid öğrenmek biraz daha kolay oldu.\n\n" +
                        "```css\n" +
                        ".container {\n" +
                        "  display: grid;\n" +
                        "  grid-template-columns: repeat(3, 1fr);\n" +
                        "  gap: 20px;\n" +
                        "}\n" +
                        "```\n\n" +
                        "Bu kadar basit bir kod ile responsive layout yapabiliyorsunuz. Harika!\n\n" +
                        "Haftaya Grid ile daha karmaşık layoutlar deneyeceğim.")
                .postType(PostType.TEXT)
                .category("Web Design")
                .author(user2)
                .isPublished(true)
                .isFeatured(true)
                .commentsEnabled(true)
                .build();
        postRepository.save(post3);

        Post post4 = Post.builder()
                .title("Bugün JavaScript Promise'leri anlamaya çalıştım")
                .content("JavaScript'te asynchronous işlemler başta kafamı karıştırmıştı ama Promise'leri anladıktan sonra her şey yerine oturdu.\n\n" +
                        "Callback hell'den kurtulmak için Promise kullanmak gerçekten mantıklı. Async/await ile de daha da okunabilir hale geliyor.\n\n" +
                        "Yarın fetch API ile bir deneme yapacağım.")
                .postType(PostType.TEXT)
                .category("JavaScript")
                .author(user1)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(post4);

        Post post5 = Post.builder()
                .title("Test Yazısı - Silme İşlevi Denemesi")
                .content("Bu yazıyı silme özelliğini test etmek için yazdım.\n\nSonra sileceğim muhtemelen...")
                .postType(PostType.TEXT)
                .category("Test")
                .author(user3)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(post5);

        Post post6 = Post.builder()
                .title("PostgreSQL vs MySQL - Hangisi Daha İyi?")
                .content("Okul projesinde PostgreSQL kullanıyoruz ama merak ettim MySQL ile farkları neler?\n\n" +
                        "Araştırdığım kadarıyla:\n" +
                        "- PostgreSQL daha fazla SQL standardına uygun\n" +
                        "- JSON desteği daha iyi\n" +
                        "- Karmaşık sorgularda daha performanslı\n\n" +
                        "Ama MySQL'in de PHP ile entegrasyonu daha yaygınmış. Henüz net karar veremedim.")
                .postType(PostType.TEXT)
                .category("Veritabanı")
                .author(user1)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(post6);

        Post post7 = Post.builder()
                .title("React Hook'ları Öğreniyorum")
                .content("useState ve useEffect hook'larını kullanmaya başladım.\n\n" +
                        "Class component'lerden fonksiyonel component'lere geçiş yapmak başta garip geldi ama şimdi daha mantıklı geliyor.\n\n" +
                        "useEffect dependency array'i konusu biraz kafa karıştırıcı, tekrar tekrar dökümantasyona bakmam gerekiyor.")
                .postType(PostType.TEXT)
                .category("React")
                .author(user2)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(post7);

        // Draft (yayınlanmamış) yazı
        Post draftPost = Post.builder()
                .title("Taslak: Git Branch Stratejileri")
                .content("Bu yazıyı henüz bitirmedim. Git Flow ve trunk-based development hakkında araştırma yapıyorum.\n\n" +
                        "Daha sonra tamamlayacağım...")
                .postType(PostType.TEXT)
                .category("Git")
                .author(user1)
                .isPublished(false)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(draftPost);

        // Link paylaşımı
        Post linkPost = Post.builder()
                .title("Faydalı: Spring Boot Best Practices")
                .content("Spring Boot için çok iyi bir rehber buldum. Özellikle security ve testing kısımları detaylı anlatılmış.")
                .url("https://spring.io/guides")
                .postType(PostType.LINK)
                .category("Kaynaklar")
                .author(user1)
                .isPublished(true)
                .isFeatured(false)
                .commentsEnabled(true)
                .build();
        postRepository.save(linkPost);

        log.info("9 örnek post oluşturuldu (8 yayınlanmış, 1 taslak).");
    }
}
