package com.movie.dto;

import lombok.Data;

@Data
public class ReplyRequest {
    private Integer parentCommentId;
    private String content;
}