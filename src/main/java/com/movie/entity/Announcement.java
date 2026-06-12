// Announcement.java
package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Announcement {
    private Integer id;
    private String title;
    private String content;
    private Integer isActive;
    private Integer priority;
    private String targetType;
    private String targetIds;
    private Date createTime;
    private Date updateTime;
    private boolean IsRead;


}