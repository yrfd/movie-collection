package com.movie.dto;

import lombok.Data;
import java.util.Date;

@Data
public class MyReviewDTO {
    private Integer collectionId;      // 收藏ID
    private Integer movieId;           // 电影ID
    private Integer tmdbId;            // TMDB ID
    private String movieName;          // 电影名称
    private String posterUrl;          // 海报URL
    private Double personalRating;     // 个人评分
    private String watchStatus;        // 观看状态
    private String privateReview;      // 私人评价
    private String publicReview;       // 公开评价内容
    private Double publicRating;       // 公开评分
    private Integer commentId;         // 公开评价ID
    private Date reviewTime;           // 评价时间
    private String reviewType;         // 评价类型: PRIVATE, PUBLIC, BOTH, NONE
}