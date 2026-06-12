package com.movie.dto;

import lombok.Data;

import java.util.List;

@Data
public class AnnouncementRequest {
    private Integer id;
    private String title;
    private String content;
    private Integer isActive;
    private Integer priority;
    private String targetType;
    private String targetIds;
}