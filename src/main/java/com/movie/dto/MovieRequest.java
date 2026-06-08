package com.movie.dto;

import lombok.Data;

@Data
public class MovieRequest {
    private String movieName;
    private String director;
    private Integer year;
    private String region;
    private String genre;
    private String posterUrl;
    private Double personalRating;
    private String watchStatus;
    private String privateReview;
    private Integer tmdbId;
    private Integer categoryId;  // 新增：分类ID
}