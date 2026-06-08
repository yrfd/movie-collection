package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class MovieCategory {
    private Integer categoryId;
    private Integer userId;
    private String categoryName;
    private Date createTime;
}