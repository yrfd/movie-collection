package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class MoviePublic {
    private Integer movieId;
    private Integer tmdbId;
    private String movieName;
    private String director;
    private Integer year;
    private String posterUrl;
    private String genre;
    private String region;
    private String actors;
    private Double avgRating;
    private Integer ratingCount;
    private Date createTime;
}