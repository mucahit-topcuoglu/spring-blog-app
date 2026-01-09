package com.blog.blogprojesi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Post (Yazı) Entity sınıfı
 * TEXT veya LINK tipinde yazılar içerebilir
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Başlık boş olamaz")
    @Size(min = 3, max = 200, message = "Başlık 3-200 karakter arasında olmalıdır")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "url")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    @Builder.Default
    private PostType postType = PostType.TEXT;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "category")
    private String category;

    @Column(name = "excerpt", length = 500)
    private String excerpt;

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "is_published")
    @Builder.Default
    private boolean isPublished = true;

    @Column(name = "is_featured")
    @Builder.Default
    private boolean isFeatured = false;

    @Column(name = "comments_enabled")
    @Builder.Default
    private boolean commentsEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // İlişkiler
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rating> ratings = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Bookmark> bookmarks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (content != null && excerpt == null) {
            excerpt = content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper metotlar
    public double getAverageRating() {
        if (ratings == null || ratings.isEmpty()) {
            return 0.0;
        }
        return ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);
    }

    public int getRatingCount() {
        return ratings != null ? ratings.size() : 0;
    }

    public int getCommentCount() {
        return comments != null ? comments.size() : 0;
    }

    public int getBookmarkCount() {
        return bookmarks != null ? bookmarks.size() : 0;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
    }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.toLocalDate().toString();
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

    public String getReadTimeText() {
        if (content == null || content.isEmpty()) return "1 dk";
        int wordCount = content.split("\\s+").length;
        int minutes = Math.max(1, wordCount / 200); // Ortalama okuma hızı: 200 kelime/dk
        return minutes + " dk";
    }

    /**
     * Reading time in minutes (alias for templates)
     */
    public int getReadingTime() {
        if (content == null || content.isEmpty()) return 1;
        int wordCount = content.split("\\s+").length;
        return Math.max(1, wordCount / 200);
    }

    /**
     * Like count placeholder (for future implementation)
     */
    public int getLikeCount() {
        return 0; // Placeholder
    }

    public boolean isLinkPost() {
        return postType == PostType.LINK;
    }

    public boolean isTextPost() {
        return postType == PostType.TEXT;
    }
}
