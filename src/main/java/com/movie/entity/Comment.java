package com.movie.entity;

import lombok.Data;
import java.util.Date;

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

    // 提供一个 getRating 方法供前端使用（兼容性）
    public Double getRating() {
        return this.snapshotRating;
    }
}