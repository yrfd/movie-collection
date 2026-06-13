// AdminLog.java
package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class AdminLog {
    private Integer id;
    private String adminName;
    private String action;
    private String target;
    private String details;
    private String ip;
    private Date createTime;
}