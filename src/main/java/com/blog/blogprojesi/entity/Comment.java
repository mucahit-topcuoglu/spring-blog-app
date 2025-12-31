package com.blog.blogprojesi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Comment (Yorum) Entity sınıfı
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Yorum içeriği boş olamaz")
    @Size(min = 1, max = 2000, message = "Yorum 1-2000 karakter arasında olmalıdır")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // İlişkiler
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Helper metotlar
    public String getFormattedDate() {
        if (createdAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
        return createdAt.format(formatter);
    }

    public String getRelativeTime() {
        if (createdAt == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();
        
        if (minutes < 1) return "Az önce";
        if (minutes < 60) return minutes + " dakika önce";
        
        long hours = minutes / 60;
        if (hours < 24) return hours + " saat önce";
        
        long days = hours / 24;
        if (days < 7) return days + " gün önce";
        
        long weeks = days / 7;
        if (weeks < 4) return weeks + " hafta önce";
        
        long months = days / 30;
        if (months < 12) return months + " ay önce";
        
        return (days / 365) + " yıl önce";
    }

    public String getUserInitials() {
        if (user == null || user.getUsername() == null) return "U";
        return user.getUsername().substring(0, 1).toUpperCase();
    }

    /**
     * Alias for user - used in templates as 'author'
     */
    public User getAuthor() {
        return user;
    }
}
