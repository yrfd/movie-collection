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

    // 关联字段
    private String username;
    private String movieName;
    private Double currentRating;
    private String replyToUsername;
    private List<Comment> replies;

    private String userAvatar;  // 用户头像

    // 添加 getter 和 setter
    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
}