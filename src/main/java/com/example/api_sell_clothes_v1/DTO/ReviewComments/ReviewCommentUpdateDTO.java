package com.example.api_sell_clothes_v1.DTO.ReviewComments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommentUpdateDTO {
    private String content;
}
