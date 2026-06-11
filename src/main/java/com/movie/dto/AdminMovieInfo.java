package com.movie.dto;

import lombok.Data;

@Data
public class AdminMovieInfo {
    private Integer movieId;
    private String movieName;
    private String director;
    private Integer year;
    private String genre;
    private String region;
    private Double avgRating;
    private Integer ratingCount;
    private Integer commentCount;
    private Integer collectionCount;
}