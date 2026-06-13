// Carousel.java
package com.movie.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Carousel {
    private Integer id;
    private String imageUrl;
    private String linkUrl;
    private String title;
    private Integer sortOrder;
    private Integer isActive;
    private Date createTime;
    private Date updateTime;
}