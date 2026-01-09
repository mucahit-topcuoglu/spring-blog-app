package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.Bookmark;
import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Bookmark Repository Interface
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // Kullanıcının belirli bir posta olan yer imini getir
    Optional<Bookmark> findByUserAndPost(User user, Post post);

    // Kullanıcının tüm yer imlerini getir (postlarla birlikte)
    @Query("SELECT b FROM Bookmark b LEFT JOIN FETCH b.post p LEFT JOIN FETCH p.author WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Bookmark> findByUserIdWithPost(@Param("userId") Long userId);

    // Kullanıcının bu postu yer imlerine ekleyip eklemediğini kontrol et
    boolean existsByUserAndPost(User user, Post post);

    // Kullanıcı ID ve Post ID ile kontrol et
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Bookmark b WHERE b.user.id = :userId AND b.post.id = :postId")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    // Bir postun kaç kez yer imlerine eklendiğini say
    long countByPost(Post post);

    // Bir kullanıcının yer imi sayısı
    long countByUser(User user);

    // Kullanıcı ve Post ID ile sil
    void deleteByUserAndPost(User user, Post post);
}
