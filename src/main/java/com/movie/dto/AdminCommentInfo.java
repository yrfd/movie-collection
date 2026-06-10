package com.movie.dto;

import lombok.Data;
import java.util.Date;

@Data
public class AdminCommentInfo {
    private Integer commentId;
    private String username;
    private String movieName;
    private Double rating;
    private String content;
    private Date createTime;
}