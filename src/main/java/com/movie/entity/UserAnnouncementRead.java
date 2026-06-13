package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class UserAnnouncementRead {
    private Integer id;
    private Integer announcementId;
    private Integer userId;
    private Integer isRead;
    private Date readTime;
    private Date createTime;
}