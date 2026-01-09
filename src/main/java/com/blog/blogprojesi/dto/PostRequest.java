package com.blog.blogprojesi.dto;

import com.blog.blogprojesi.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Post oluşturma/güncelleme formu için DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "Başlık boş olamaz")
    @Size(min = 3, max = 200, message = "Başlık 3-200 karakter arasında olmalıdır")
    private String title;

    private String content;

    private String url;

    private PostType postType = PostType.TEXT;

    private String category;

    private boolean commentsEnabled = true;

    private boolean featured = false;

    public boolean isValid() {
        if (postType == PostType.LINK) {
            return url != null && !url.trim().isEmpty();
        } else {
            return content != null && !content.trim().isEmpty();
        }
    }
}
