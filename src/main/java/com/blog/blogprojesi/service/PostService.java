package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.PostType;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.repository.PostRepository;
import com.blog.blogprojesi.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Post işlemleri için Service sınıfı
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final RatingRepository ratingRepository;

    /**
     * Yeni text post oluştur (imageUrl ile)
     */
    public Post createTextPost(User author, String title, String content, String category, boolean commentsEnabled, boolean featured, String imageUrl) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .postType(PostType.TEXT)
                .category(category)
                .author(author)
                .isPublished(true)
                .commentsEnabled(commentsEnabled)
                .isFeatured(featured)
                .imageUrl(imageUrl)
                .build();

        // Excerpt oluştur
        if (content != null && content.length() > 200) {
            post.setExcerpt(content.substring(0, 200) + "...");
        } else {
            post.setExcerpt(content);
        }

        return postRepository.save(post);
    }
    
    /**
     * Yeni text post oluştur (eski versiyon uyumluluk)
     */
    public Post createTextPost(User author, String title, String content, String category, boolean commentsEnabled, boolean featured) {
        return createTextPost(author, title, content, category, commentsEnabled, featured, null);
    }

    /**
     * Yeni link post oluştur (imageUrl ile)
     */
    public Post createLinkPost(User author, String title, String url, String content, String category, boolean commentsEnabled, boolean featured, String imageUrl) {
        Post post = Post.builder()
                .title(title)
                .url(url)
                .content(content)
                .postType(PostType.LINK)
                .category(category)
                .author(author)
                .isPublished(true)
                .commentsEnabled(commentsEnabled)
                .isFeatured(featured)
                .imageUrl(imageUrl)
                .build();

        // Link postları için excerpt
        if (content != null && !content.isEmpty()) {
            post.setExcerpt(content.length() > 200 ? content.substring(0, 200) + "..." : content);
        } else {
            post.setExcerpt("Link: " + url);
        }

        return postRepository.save(post);
    }
    
    /**
     * Yeni link post oluştur (eski versiyon uyumluluk)
     */
    public Post createLinkPost(User author, String title, String url, String content, String category, boolean commentsEnabled, boolean featured) {
        return createLinkPost(author, title, url, content, category, commentsEnabled, featured, null);
    }

    /**
     * Post oluştur (generic)
     */
    public Post createPost(User author, String title, String content, String url, PostType postType, 
                          String category, boolean commentsEnabled, boolean featured, String imageUrl) {
        if (postType == PostType.LINK) {
            return createLinkPost(author, title, url, content, category, commentsEnabled, featured, imageUrl);
        }
        return createTextPost(author, title, content, category, commentsEnabled, featured, imageUrl);
    }
    
    /**
     * Post oluştur (generic) - eski versiyon uyumluluk için
     */
    public Post createPost(User author, String title, String content, String url, PostType postType, 
                          String category, boolean commentsEnabled, boolean featured) {
        return createPost(author, title, content, url, postType, category, commentsEnabled, featured, null);
    }

    /**
     * ID'ye göre post getir
     */
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    /**
     * ID'ye göre post getir (author ile)
     */
    @Transactional(readOnly = true)
    public Optional<Post> findByIdWithAuthor(Long id) {
        return postRepository.findByIdWithAuthor(id);
    }

    /**
     * ID'ye göre post getir (tüm detaylarla)
     */
    @Transactional(readOnly = true)
    public Optional<Post> findByIdWithDetails(Long id) {
        return postRepository.findByIdWithDetails(id);
    }

    /**
     * Tüm yayınlanmış postları getir
     */
    @Transactional(readOnly = true)
    public List<Post> findAllPublishedPosts() {
        return postRepository.findByIsPublishedTrueOrderByCreatedAtDesc();
    }

    /**
     * Sayfalama ile yayınlanmış postları getir
     */
    @Transactional(readOnly = true)
    public Page<Post> findAllPublishedPosts(Pageable pageable) {
        return postRepository.findByIsPublishedTrueOrderByCreatedAtDesc(pageable);
    }

    /**
     * Kullanıcının postlarını getir
     */
    @Transactional(readOnly = true)
    public List<Post> findPostsByAuthor(User author) {
        return postRepository.findByAuthorOrderByCreatedAtDesc(author);
    }

    /**
     * Kullanıcının yayınlanmış postlarını getir
     */
    @Transactional(readOnly = true)
    public List<Post> findPublishedPostsByAuthor(User author) {
        return postRepository.findByAuthorAndIsPublishedTrueOrderByCreatedAtDesc(author);
    }

    /**
     * Kategoriye göre postları getir
     */
    @Transactional(readOnly = true)
    public List<Post> findPostsByCategory(String category) {
        return postRepository.findByCategoryAndIsPublishedTrueOrderByCreatedAtDesc(category);
    }

    /**
     * Post tipine göre getir
     */
    @Transactional(readOnly = true)
    public List<Post> findPostsByType(PostType postType) {
        return postRepository.findByPostTypeAndIsPublishedTrueOrderByCreatedAtDesc(postType);
    }

    /**
     * Öne çıkan postları getir
     */
    @Transactional(readOnly = true)
    public List<Post> findFeaturedPosts() {
        return postRepository.findByIsFeaturedTrueAndIsPublishedTrueOrderByCreatedAtDesc();
    }

    /**
     * Post ara
     */
    @Transactional(readOnly = true)
    public List<Post> searchPosts(String keyword) {
        return postRepository.searchPosts(keyword);
    }

    /**
     * En çok görüntülenen postlar
     */
    @Transactional(readOnly = true)
    public List<Post> findMostViewedPosts() {
        return postRepository.findTop10ByIsPublishedTrueOrderByViewCountDesc();
    }

    /**
     * En çok puanlanan postlar
     */
    @Transactional(readOnly = true)
    public List<Post> findTopRatedPosts(int limit) {
        return postRepository.findTopRatedPosts(PageRequest.of(0, limit));
    }

    /**
     * Son postlar
     */
    @Transactional(readOnly = true)
    public List<Post> findRecentPosts() {
        return postRepository.findTop5ByIsPublishedTrueOrderByCreatedAtDesc();
    }

    /**
     * Kategorileri getir
     */
    @Transactional(readOnly = true)
    public List<String> findAllCategories() {
        return postRepository.findAllCategories();
    }

    /**
     * Kategori bazlı post sayılarını getir
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCategoryPostCounts() {
        List<Object[]> results = postRepository.countByCategory();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }

    /**
     * Kullanıcıya özel kategori bazlı post sayılarını getir
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getCategoryPostCountsByAuthor(User author) {
        List<Object[]> results = postRepository.countByCategoryAndAuthor(author);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] result : results) {
            counts.put((String) result[0], (Long) result[1]);
        }
        return counts;
    }

    /**
     * Toplam post sayısı
     */
    @Transactional(readOnly = true)
    public long getTotalPostCount() {
        return postRepository.countByIsPublishedTrue();
    }

    /**
     * Toplam yazar sayısı
     */
    @Transactional(readOnly = true)
    public long getTotalAuthorCount() {
        return postRepository.findAll().stream()
                .filter(Post::isPublished)
                .map(p -> p.getAuthor().getId())
                .distinct()
                .count();
    }

    /**
     * Kategoriye göre son yazıları getir
     */
    @Transactional(readOnly = true)
    public List<Post> findRecentPostsByCategory(String category, int limit) {
        return postRepository.findByIsPublishedTrueAndCategoryOrderByCreatedAtDesc(category)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Post güncelle
     */
    public Post updatePost(Long postId, String title, String content, String url, String category, 
                          boolean commentsEnabled, boolean featured, String imageUrl) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setCommentsEnabled(commentsEnabled);
        post.setFeatured(featured);
        post.setImageUrl(imageUrl);

        if (post.getPostType() == PostType.LINK) {
            post.setUrl(url);
        }

        // Excerpt güncelle
        if (content != null && content.length() > 200) {
            post.setExcerpt(content.substring(0, 200) + "...");
        } else {
            post.setExcerpt(content);
        }

        return postRepository.save(post);
    }

    /**
     * Görüntüleme sayısını artır
     */
    public void incrementViewCount(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.incrementViewCount();
            postRepository.save(post);
        });
    }

    /**
     * Post sil
     */
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }

    /**
     * Post'un ortalama puanını getir
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long postId) {
        return ratingRepository.getAverageRatingByPostId(postId);
    }

    /**
     * Post'un puan sayısını getir
     */
    @Transactional(readOnly = true)
    public Long getRatingCount(Long postId) {
        return ratingRepository.countByPostId(postId);
    }

    /**
     * Kullanıcının post sayısı
     */
    @Transactional(readOnly = true)
    public long countPostsByAuthor(User author) {
        return postRepository.countByAuthor(author);
    }

    /**
     * Yayın durumunu değiştir
     */
    public Post togglePublishStatus(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));
        post.setPublished(!post.isPublished());
        return postRepository.save(post);
    }

    /**
     * Kullanıcının taslak postlarını getir
     */
    @Transactional(readOnly = true)
    public List<Post> findDraftsByAuthor(User author) {
        return postRepository.findByAuthorAndIsPublishedFalseOrderByCreatedAtDesc(author);
    }

    /**
     * Kullanıcının taslak sayısını getir
     */
    @Transactional(readOnly = true)
    public long countDraftsByAuthor(User author) {
        return postRepository.countByAuthorAndIsPublishedFalse(author);
    }

    /**
     * Post'u taslak olarak kaydet
     */
    public Post saveDraft(String title, String content, String category, String url, String imageUrl, 
                          PostType postType, User author) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .category(category)
                .url(url)
                .imageUrl(imageUrl)
                .postType(postType)
                .author(author)
                .isPublished(false)
                .isFeatured(false)
                .viewCount(0L)
                .build();
        return postRepository.save(post);
    }
}
