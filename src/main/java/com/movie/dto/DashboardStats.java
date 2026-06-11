package com.movie.dto;

import lombok.Data;

@Data
public class DashboardStats {
    private Long userCount;
    private Long movieCount;
    private Long commentCount;
    private Long collectionCount;
    private Long todayNewUsers;
    private Long todayNewComments;
    private java.util.List<DailyStats> dailyStats;

    @Data
    public static class DailyStats {
        private String date;
        private Long count;
    }
}