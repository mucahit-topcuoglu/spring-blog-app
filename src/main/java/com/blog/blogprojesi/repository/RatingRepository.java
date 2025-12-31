package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.Rating;
import com.blog.blogprojesi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Rating Repository Interface
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Kullanıcının belirli bir posta verdiği puanı getir
    Optional<Rating> findByUserAndPost(User user, Post post);

    // Kullanıcı ID ve Post ID ile puan getir
    @Query("SELECT r FROM Rating r WHERE r.user.id = :userId AND r.post.id = :postId")
    Optional<Rating> findByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    // Kullanıcının bu postu daha önce puanlayıp puanlamadığını kontrol et
    boolean existsByUserAndPost(User user, Post post);

    // Bir postun ortalama puanını getir
    @Query("SELECT COALESCE(AVG(r.score), 0) FROM Rating r WHERE r.post.id = :postId")
    Double getAverageRatingByPostId(@Param("postId") Long postId);

    // Bir postun toplam puan sayısını getir
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);

    // Bir postun puan dağılımını getir
    @Query("SELECT r.score, COUNT(r) FROM Rating r WHERE r.post.id = :postId GROUP BY r.score ORDER BY r.score")
    java.util.List<Object[]> getRatingDistributionByPostId(@Param("postId") Long postId);

    // Bir kullanıcının verdiği tüm puanları getir
    java.util.List<Rating> findByUser(User user);

    // Bir postun tüm puanlarını getir
    java.util.List<Rating> findByPost(Post post);
}
