package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class MovieCollection {
    private Integer collectionId;
    private Integer userId;
    private Integer movieId;
    private Double personalRating;
    private String watchStatus;
    private String privateReview;
    private Integer categoryId;  // 新增：分类ID
    private Date createTime;
    private Date updateTime;

    // 关联字段
    private String movieName;
    private String director;
    private Integer year;
    private String posterUrl;
    private String genre;
    private String region;
}