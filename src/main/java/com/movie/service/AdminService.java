// AdminService.java
package com.movie.service;

import com.movie.dto.*;
import com.movie.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.*;

@Service
public class AdminService {

    @Value("${app.admin.secret}")
    private String adminSecret;

    @Autowired
    private AdminMapper adminMapper;

    public boolean validateSecret(String secret) {
        return secret != null && secret.equals(adminSecret);
    }

    public List<AdminUserInfo> getAllUsers() {
        return adminMapper.getAllUsers();
    }

    public Map<String, Object> disableUser(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        if (adminMapper.checkUserExists(userId) == 0) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        adminMapper.disableUser(userId);
        result.put("success", true);
        result.put("message", "用户已禁用");
        return result;
    }

    public Map<String, Object> enableUser(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        if (adminMapper.checkUserExists(userId) == 0) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        adminMapper.enableUser(userId);
        result.put("success", true);
        result.put("message", "用户已启用");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteUser(Integer userId) {
        Map<String, Object> result = new HashMap<>();
        if (adminMapper.checkUserExists(userId) == 0) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        adminMapper.deleteUser(userId);
        result.put("success", true);
        result.put("message", "用户已删除");
        return result;
    }

    public List<AdminMovieInfo> getAllMovies() {
        return adminMapper.getAllMoviesForAdmin();
    }

    @Transactional
    public Map<String, Object> deleteMovie(Integer movieId) {
        Map<String, Object> result = new HashMap<>();
        adminMapper.deleteMovieCollections(movieId);
        adminMapper.deleteMovieComments(movieId);
        int rows = adminMapper.deleteMovie(movieId);
        if (rows > 0) {
            result.put("success", true);
            result.put("message", "电影已删除");
        } else {
            result.put("success", false);
            result.put("message", "电影不存在");
        }
        return result;
    }

    public List<AdminCommentInfo> getAllComments() {
        return adminMapper.getAllComments();
    }

    public Map<String, Object> deleteComment(Integer commentId) {
        Map<String, Object> result = new HashMap<>();
        int rows = adminMapper.deleteComment(commentId);
        if (rows > 0) {
            result.put("success", true);
            result.put("message", "评论已删除");
        } else {
            result.put("success", false);
            result.put("message", "评论不存在");
        }
        return result;
    }

    /**
     * ✅ 重构：获取仪表盘统计数据
     */
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        // 1. 获取系统总体统计
        Map<String, Object> systemStats = adminMapper.getSystemStats();
        if (systemStats != null) {
            stats.setUserCount((Long) systemStats.getOrDefault("userCount", 0L));
            stats.setMovieCount((Long) systemStats.getOrDefault("movieCount", 0L));
            stats.setCommentCount((Long) systemStats.getOrDefault("commentCount", 0L));
            stats.setCollectionCount((Long) systemStats.getOrDefault("collectionCount", 0L));
            stats.setTodayNewUsers((Long) systemStats.getOrDefault("todayNewUsers", 0L));
            stats.setTodayNewComments((Long) systemStats.getOrDefault("todayNewComments", 0L));
        }

        // 2. 获取近7天新增用户每日数据
        List<Map<String, Object>> userDailyData = adminMapper.getLast7DaysNewUsers();
        List<DashboardStats.DailyStat> userDailyStats = new ArrayList<>();

        // 生成过去7天的完整日期列表
        List<String> last7Days = getLast7DaysDates();

        // 将数据库查询结果转换为 Map
        Map<String, Long> userCountMap = new HashMap<>();
        for (Map<String, Object> item : userDailyData) {
            Object dateObj = item.get("date");
            Object countObj = item.get("count");
            if (dateObj != null && countObj != null) {
                String dateStr = dateObj.toString();
                Long count = ((Number) countObj).longValue();
                userCountMap.put(dateStr, count);
            }
        }

        // 填充完整7天数据（没有数据的日期补0）
        for (String date : last7Days) {
            DashboardStats.DailyStat dailyStat = new DashboardStats.DailyStat();
            dailyStat.setDate(date);
            dailyStat.setCount(userCountMap.getOrDefault(date, 0L));
            userDailyStats.add(dailyStat);
        }
        stats.setUserDailyStats(userDailyStats);

        // 3. 获取近7天新增评论每日数据
        List<Map<String, Object>> commentDailyData = adminMapper.getLast7DaysNewComments();
        List<DashboardStats.DailyStat> commentDailyStats = new ArrayList<>();

        Map<String, Long> commentCountMap = new HashMap<>();
        for (Map<String, Object> item : commentDailyData) {
            Object dateObj = item.get("date");
            Object countObj = item.get("count");
            if (dateObj != null && countObj != null) {
                String dateStr = dateObj.toString();
                Long count = ((Number) countObj).longValue();
                commentCountMap.put(dateStr, count);
            }
        }

        // 填充完整7天数据
        for (String date : last7Days) {
            DashboardStats.DailyStat dailyStat = new DashboardStats.DailyStat();
            dailyStat.setDate(date);
            dailyStat.setCount(commentCountMap.getOrDefault(date, 0L));
            commentDailyStats.add(dailyStat);
        }
        stats.setCommentDailyStats(commentDailyStats);

        return stats;
    }

    /**
     * 获取过去7天的日期列表（格式：yyyy-MM-dd）
     */
    private List<String> getLast7DaysDates() {
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // 从7天前开始，到昨天结束
        for (int i = 6; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
            dates.add(dateStr);
        }
        return dates;
    }

    public List<Map<String, Object>> getHotMovies(int limit) {
        return adminMapper.selectHotMovies(limit);
    }

    public List<Map<String, Object>> getActiveUsers(int limit) {
        return adminMapper.selectActiveUsers(limit);
    }

    public void exportUsers(HttpServletResponse response) {
        List<AdminUserInfo> users = adminMapper.getAllUsers();
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=users.csv");
        try {
            PrintWriter writer = response.getWriter();
            writer.println("ID,用户名,邮箱,注册时间,收藏数,评论数,状态");
            for (AdminUserInfo user : users) {
                writer.printf("%d,%s,%s,%s,%d,%d,%s%n",
                        user.getUserId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getCreateTime(),
                        user.getMovieCount(),
                        user.getCommentCount(),
                        user.getStatus() == 1 ? "正常" : "禁用"
                );
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}