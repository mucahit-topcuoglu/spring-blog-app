package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.Comment;
import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.repository.CommentRepository;
import com.blog.blogprojesi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Comment işlemleri için Service sınıfı
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    /**
     * Yeni yorum ekle
     */
    public Comment addComment(User user, Long postId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        // Yorumlar kapalıysa hata ver
        if (!post.isCommentsEnabled()) {
            throw new RuntimeException("Bu gönderi için yorumlar kapalı");
        }

        Comment comment = Comment.builder()
                .content(content)
                .user(user)
                .post(post)
                .build();

        return commentRepository.save(comment);
    }

    /**
     * ID'ye göre yorum getir
     */
    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    /**
     * Post'un yorumlarını getir
     */
    @Transactional(readOnly = true)
    public List<Comment> findCommentsByPost(Post post) {
        return commentRepository.findByPostOrderByCreatedAtDesc(post);
    }

    /**
     * Post ID'ye göre yorumları getir (kullanıcı bilgisiyle)
     */
    @Transactional(readOnly = true)
    public List<Comment> findCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdWithUser(postId);
    }

    /**
     * Kullanıcının yorumlarını getir
     */
    @Transactional(readOnly = true)
    public List<Comment> findCommentsByUser(User user) {
        return commentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Son yorumları getir
     */
    @Transactional(readOnly = true)
    public List<Comment> findRecentComments() {
        return commentRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * Yorum güncelle
     */
    public Comment updateComment(Long commentId, String content, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı"));

        // Sadece yorum sahibi güncelleyebilir
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bu yorumu düzenleme yetkiniz yok");
        }

        comment.setContent(content);
        return commentRepository.save(comment);
    }

    /**
     * Yorum sil
     */
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı"));

        // Sadece yorum sahibi veya post sahibi silebilir
        if (!comment.getUser().getId().equals(currentUser.getId()) && 
            !comment.getPost().getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bu yorumu silme yetkiniz yok");
        }

        commentRepository.delete(comment);
    }

    /**
     * Post'un yorum sayısı
     */
    @Transactional(readOnly = true)
    public long countCommentsByPost(Post post) {
        return commentRepository.countByPost(post);
    }

    /**
     * Kullanıcının yorum sayısı
     */
    @Transactional(readOnly = true)
    public long countCommentsByUser(User user) {
        return commentRepository.countByUser(user);
    }
}
