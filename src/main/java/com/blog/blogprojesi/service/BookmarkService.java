package com.blog.blogprojesi.service;

import com.blog.blogprojesi.entity.Bookmark;
import com.blog.blogprojesi.entity.Post;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.repository.BookmarkRepository;
import com.blog.blogprojesi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Bookmark (Yer İmi) işlemleri için Service sınıfı
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;

    /**
     * Post'u yer imlerine ekle
     */
    public Bookmark addBookmark(User user, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        // Zaten yer imlerinde mi kontrol et
        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            throw new RuntimeException("Bu gönderi zaten yer imlerinde");
        }

        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .post(post)
                .build();

        return bookmarkRepository.save(bookmark);
    }

    /**
     * Post'u yer imlerinden çıkar
     */
    public void removeBookmark(User user, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        bookmarkRepository.findByUserAndPost(user, post)
                .ifPresent(bookmarkRepository::delete);
    }

    /**
     * Yer imi durumunu toggle et (ekle veya çıkar)
     */
    public boolean toggleBookmark(User user, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı"));

        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            bookmarkRepository.deleteByUserAndPost(user, post);
            return false; // Yer imi kaldırıldı
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .post(post)
                    .build();
            bookmarkRepository.save(bookmark);
            return true; // Yer imi eklendi
        }
    }

    /**
     * Kullanıcının yer imlerini getir
     */
    @Transactional(readOnly = true)
    public List<Bookmark> getUserBookmarks(User user) {
        return bookmarkRepository.findByUserIdWithPost(user.getId());
    }

    /**
     * Kullanıcının yer imli postlarını getir
     */
    @Transactional(readOnly = true)
    public List<Post> getBookmarkedPosts(User user) {
        return bookmarkRepository.findByUserIdWithPost(user.getId())
                .stream()
                .map(Bookmark::getPost)
                .collect(Collectors.toList());
    }

    /**
     * Post'un yer imlerde olup olmadığını kontrol et
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(User user, Long postId) {
        return bookmarkRepository.existsByUserIdAndPostId(user.getId(), postId);
    }

    /**
     * Post'un kaç kez yer imlerine eklendiğini say
     */
    @Transactional(readOnly = true)
    public long getBookmarkCount(Long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) return 0L;
        return bookmarkRepository.countByPost(post);
    }

    /**
     * Kullanıcının toplam yer imi sayısı
     */
    @Transactional(readOnly = true)
    public long getUserBookmarkCount(User user) {
        return bookmarkRepository.countByUser(user);
    }
}
