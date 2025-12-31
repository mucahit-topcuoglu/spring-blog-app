package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.Role;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Kullanıcı işlemleri için Service sınıfı
 * Spring Security UserDetailsService'i implement eder
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Kullanıcı aranıyor: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + username));
    }

    /**
     * Yeni kullanıcı kaydı
     */
    public User registerUser(String username, String email, String password, String firstName, String lastName) {
        log.info("Kayıt işlemi başlıyor - Username: {}, Email: {}", username, email);
        
        // Kullanıcı adı kontrolü
        if (userRepository.existsByUsername(username)) {
            log.warn("Kullanıcı adı zaten mevcut: {}", username);
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor");
        }

        // E-posta kontrolü
        if (userRepository.existsByEmail(email)) {
            log.warn("E-posta zaten mevcut: {}", email);
            throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
        }

        // Yeni kullanıcı oluştur
        String encodedPassword = passwordEncoder.encode(password);
        log.info("Şifre encode edildi");
        
        User user = User.builder()
                .username(username)
                .email(email)
                .password(encodedPassword)
                .firstName(firstName)
                .lastName(lastName)
                .role(Role.USER)
                .isEnabled(true)
                .build();

        log.info("User entity oluşturuldu, kayıt ediliyor...");
        User savedUser = userRepository.save(user);
        log.info("Kullanıcı başarıyla kaydedildi - ID: {}", savedUser.getId());
        
        return savedUser;
    }

    /**
     * Kullanıcı adına göre getir
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * E-posta adresine göre getir
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * ID'ye göre getir
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Tüm kullanıcıları getir
     */
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Kullanıcı bilgilerini güncelle
     */
    public User updateUser(Long userId, String email, String bio, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
            }
            user.setEmail(email);
        }

        if (bio != null) {
            user.setBio(bio);
        }

        user.setProfileImageUrl(profileImageUrl);

        return userRepository.save(user);
    }

    /**
     * Kullanıcı bilgilerini tam güncelle (firstName, lastName dahil)
     */
    public User updateUserFull(Long userId, String firstName, String lastName, String email, String bio, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        if (firstName != null && !firstName.trim().isEmpty()) {
            user.setFirstName(firstName.trim());
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            user.setLastName(lastName.trim());
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Bu e-posta adresi zaten kullanılıyor");
            }
            user.setEmail(email);
        }

        if (bio != null) {
            user.setBio(bio);
        }

        if (profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
        }

        return userRepository.save(user);
    }

    /**
     * Şifre değiştir
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Mevcut şifre yanlış");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Kullanıcı adı müsait mi?
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * E-posta müsait mi?
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Kullanıcı ara
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    /**
     * Kullanıcıyı sil
     */
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Şifre sıfırlama token oluştur (basit UUID token)
     */
    public String createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bu e-posta adresiyle kayıtlı kullanıcı bulunamadı"));
        
        // Basit token oluştur (gerçek uygulamada expiration ve database kaydı yapılmalı)
        String token = java.util.UUID.randomUUID().toString();
        log.info("Password reset token created for user: {}", email);
        
        // TODO: Token'ı veritabanına kaydet ve expiration ekle
        // Şimdilik sadece log'layalım
        
        return token;
    }

    /**
     * Şifre güncelle
     */
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password updated for user: {}", user.getUsername());
    }
}
