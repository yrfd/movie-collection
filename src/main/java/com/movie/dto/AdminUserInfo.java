package com.movie.dto;

import lombok.Data;
import java.util.Date;

@Data
public class AdminUserInfo {
    private Integer userId;
    private String username;
    private String email;
    private Date createTime;
    private Integer movieCount;
    private Integer commentCount;
}