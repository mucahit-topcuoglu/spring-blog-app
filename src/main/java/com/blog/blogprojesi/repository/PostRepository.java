package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.PostType;
import com.blog.blogprojesi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Post Repository Interface
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Yayınlanmış tüm postları getir (sayfalama ile)
    Page<Post> findByIsPublishedTrueOrderByCreatedAtDesc(Pageable pageable);

    // Yayınlanmış tüm postları getir
    List<Post> findByIsPublishedTrueOrderByCreatedAtDesc();

    // Belirli bir kullanıcının postlarını getir
    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    // Belirli bir kullanıcının yayınlanmış postlarını getir
    List<Post> findByAuthorAndIsPublishedTrueOrderByCreatedAtDesc(User author);

    // Kategori bazlı postları getir
    List<Post> findByCategoryAndIsPublishedTrueOrderByCreatedAtDesc(String category);

    // Post tipine göre getir
    List<Post> findByPostTypeAndIsPublishedTrueOrderByCreatedAtDesc(PostType postType);

    // Öne çıkan postları getir
    List<Post> findByIsFeaturedTrueAndIsPublishedTrueOrderByCreatedAtDesc();

    // ID ile post getir (fetch join ile author)
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.author WHERE p.id = :id")
    Optional<Post> findByIdWithAuthor(@Param("id") Long id);

    // ID ile post getir (fetch join ile tüm ilişkiler)
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.author " +
           "LEFT JOIN FETCH p.comments c " +
           "LEFT JOIN FETCH c.user " +
           "WHERE p.id = :id")
    Optional<Post> findByIdWithDetails(@Param("id") Long id);

    // Başlık veya içerikte arama
    @Query("SELECT p FROM Post p WHERE p.isPublished = true AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Post> searchPosts(@Param("keyword") String keyword);

    // En çok görüntülenen postlar
    List<Post> findTop10ByIsPublishedTrueOrderByViewCountDesc();

    // En çok puanlanan postlar
    @Query("SELECT p FROM Post p WHERE p.isPublished = true " +
           "ORDER BY (SELECT COALESCE(AVG(r.score), 0) FROM Rating r WHERE r.post = p) DESC")
    List<Post> findTopRatedPosts(Pageable pageable);

    // Son eklenen postlar (limit)
    List<Post> findTop5ByIsPublishedTrueOrderByCreatedAtDesc();

    // Kullanıcının post sayısı
    long countByAuthor(User author);

    // Kategorileri getir
    @Query("SELECT DISTINCT p.category FROM Post p WHERE p.category IS NOT NULL AND p.isPublished = true")
    List<String> findAllCategories();

    // Kategoriye göre post sayısı
    @Query("SELECT p.category, COUNT(p) FROM Post p WHERE p.isPublished = true AND p.category IS NOT NULL " +
           "GROUP BY p.category ORDER BY COUNT(p) DESC")
    List<Object[]> countByCategory();

    // Kullanıcının kategorilerine göre post sayısı
    @Query("SELECT p.category, COUNT(p) FROM Post p WHERE p.author = :author AND p.category IS NOT NULL " +
           "GROUP BY p.category ORDER BY COUNT(p) DESC")
    List<Object[]> countByCategoryAndAuthor(@Param("author") User author);

    // Yayınlanmış post sayısı
    long countByIsPublishedTrue();

    // Kategoriye göre postlar
    List<Post> findByIsPublishedTrueAndCategoryOrderByCreatedAtDesc(String category);

    // Kullanıcının taslak postlarını getir (isPublished = false)
    List<Post> findByAuthorAndIsPublishedFalseOrderByCreatedAtDesc(User author);

    // Kullanıcının taslak sayısı
    long countByAuthorAndIsPublishedFalse(User author);
}
