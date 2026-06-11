package com.movie.dto;

import lombok.Data;

@Data
public class PrivateReviewRequest {
    private Integer tmdbId;           // TMDB电影ID
    private Integer movieId;
    private String privateReview;     // 私人评价内容
}