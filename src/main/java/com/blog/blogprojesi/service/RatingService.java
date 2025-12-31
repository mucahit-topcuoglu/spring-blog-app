package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.Rating;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.repository.PostRepository;
import com.blog.blogprojesi.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rating (Puanlama) işlemleri için Service sınıfı
 * Kullanıcılar gönderileri 1-5 arası puanlayabilir
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final PostRepository postRepository;

    /**
     * Post'a puan ver veya güncelle
     * Kullanıcı daha önce puan verdiyse günceller, vermemişse yeni puan ekler
     */
    public Rating ratePost(User user, Long postId, int score) {
        // Puan validasyonu
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Puan 1-5 arasında olmalıdır");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        // Kullanıcının mevcut puanını kontrol et
        Optional<Rating> existingRating = ratingRepository.findByUserAndPost(user, post);

        if (existingRating.isPresent()) {
            // Mevcut puanı güncelle
            Rating rating = existingRating.get();
            rating.setScore(score);
            return ratingRepository.save(rating);
        } else {
            // Yeni puan oluştur
            Rating rating = Rating.builder()
                    .user(user)
                    .post(post)
                    .score(score)
                    .build();
            return ratingRepository.save(rating);
        }
    }

    /**
     * Kullanıcının bir posta verdiği puanı getir
     */
    @Transactional(readOnly = true)
    public Optional<Rating> getUserRatingForPost(User user, Long postId) {
        return ratingRepository.findByUserIdAndPostId(user.getId(), postId);
    }

    /**
     * Kullanıcının bir posta puan verip vermediğini kontrol et
     */
    @Transactional(readOnly = true)
    public boolean hasUserRatedPost(User user, Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return false;
        return ratingRepository.existsByUserAndPost(user, post);
    }

    /**
     * Post'un ortalama puanını getir
     */
    @Transactional(readOnly = true)
    public double getAverageRating(Long postId) {
        Double avg = ratingRepository.getAverageRatingByPostId(postId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0; // Bir ondalık basamağa yuvarla
    }

    /**
     * Post'un toplam puan sayısını getir
     */
    @Transactional(readOnly = true)
    public long getRatingCount(Long postId) {
        Long count = ratingRepository.countByPostId(postId);
        return count != null ? count : 0L;
    }

    /**
     * Post'un puan dağılımını getir
     * Returns: Map<score, count> (1-5 arası puanların dağılımı)
     */
    @Transactional(readOnly = true)
    public Map<Integer, Long> getRatingDistribution(Long postId) {
        List<Object[]> results = ratingRepository.getRatingDistributionByPostId(postId);
        Map<Integer, Long> distribution = new HashMap<>();
        
        // Tüm puanları 0 ile başlat
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        
        // Mevcut değerleri ekle
        for (Object[] result : results) {
            Integer score = (Integer) result[0];
            Long count = (Long) result[1];
            distribution.put(score, count);
        }
        
        return distribution;
    }

    /**
     * Kullanıcının puanını sil
     */
    public void removeRating(User user, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));
        
        ratingRepository.findByUserAndPost(user, post)
                .ifPresent(ratingRepository::delete);
    }

    /**
     * Kullanıcının tüm puanlarını getir
     */
    @Transactional(readOnly = true)
    public List<Rating> getUserRatings(User user) {
        return ratingRepository.findByUser(user);
    }

    /**
     * Post'un tüm puanlarını getir
     */
    @Transactional(readOnly = true)
    public List<Rating> getPostRatings(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return List.of();
        return ratingRepository.findByPost(post);
    }

    /**
     * Puan özet bilgisi döndür
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRatingSummary(Long postId) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("average", getAverageRating(postId));
        summary.put("count", getRatingCount(postId));
        summary.put("distribution", getRatingDistribution(postId));
        return summary;
    }
}
