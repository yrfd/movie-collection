package com.movie.dto;

import lombok.Data;

@Data
public class SystemConfigRequest {
    private String configKey;
    private String configValue;
}