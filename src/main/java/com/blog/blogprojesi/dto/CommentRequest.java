package com.blog.blogprojesi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Yorum oluşturma formu için DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Yorum içeriği boş olamaz")
    @Size(min = 1, max = 2000, message = "Yorum 1-2000 karakter arasında olmalıdır")
    private String content;
}
