package com.blog.blogprojesi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Puanlama formu için DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {

    @Min(value = 1, message = "Puan en az 1 olmalıdır")
    @Max(value = 5, message = "Puan en fazla 5 olmalıdır")
    private int score;

    public boolean isValid() {
        return score >= 1 && score <= 5;
    }
}
