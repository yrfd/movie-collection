package com.movie.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardStats {
    private Long userCount;
    private Long movieCount;
    private Long commentCount;
    private Long collectionCount;
    private Long todayNewUsers;
    private Long todayNewComments;
    private List<DailyStat> userDailyStats;
    private List<DailyStat> commentDailyStats;

    @Data
    public static class DailyStat {
        private String date;
        private Long count;
    }
}