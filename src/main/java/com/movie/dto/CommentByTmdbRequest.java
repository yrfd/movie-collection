package com.movie.dto;

import lombok.Data;

@Data
public class CommentByTmdbRequest {
    private Integer tmdbId;
    private String movieName;
    private String posterUrl;
    private Integer year;
    private String director;
    private String genre;
    private String region;
    private String content;
}