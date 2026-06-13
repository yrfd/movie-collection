// CarouselRequest.java
package com.movie.dto;

import lombok.Data;

@Data
public class CarouselRequest {
    private Integer id;
    private String imageUrl;
    private String linkUrl;
    private String title;
    private Integer sortOrder;
    private Integer isActive;
}