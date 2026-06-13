package com.movie.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class Comment {
    private Integer commentId;
    private Integer movieId;
    private Integer userId;
    private String content;
    private Integer likeCount;
    private Integer replyTo;
    private Boolean isEdited;
    private Date createTime;
    private Date updateTime;
    private Double snapshotRating;

    // 关联字段
    private String username;
    private String movieName;
    private Double currentRating;
    private String replyToUsername;
    private List<Comment> replies;

    public Double getRating() {
        return this.snapshotRating;
    }
}