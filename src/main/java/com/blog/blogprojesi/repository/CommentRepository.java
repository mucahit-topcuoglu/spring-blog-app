package com.blog.blogprojesi.repository;

import com.blog.blogprojesi.entity.Comment;
import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Comment Repository Interface
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Belirli bir postun yorumlarını getir
    List<Comment> findByPostOrderByCreatedAtDesc(Post post);

    // Belirli bir postun yorumlarını getir (ID ile)
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    List<Comment> findByPostIdWithUser(@Param("postId") Long postId);

    // Belirli bir kullanıcının yorumlarını getir
    List<Comment> findByUserOrderByCreatedAtDesc(User user);

    // Bir postun yorum sayısı
    long countByPost(Post post);

    // Bir kullanıcının yorum sayısı
    long countByUser(User user);

    // Son yorumlar
    List<Comment> findTop10ByOrderByCreatedAtDesc();
}
