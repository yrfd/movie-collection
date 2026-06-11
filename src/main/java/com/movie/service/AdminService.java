package com.movie.service;

import com.movie.dto.*;
import com.movie.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class AdminService {

    @Value("${app.admin.secret}")
    private String adminSecret;

    @Autowired
    private AdminMapper adminMapper;

    /**
     * 验证管理员密钥
     */
    public boolean validateSecret(String secret) {
        return secret != null && secret.equals(adminSecret);
    }

    /**
     * 获取所有用户
     */
    public List<AdminUserInfo> getAllUsers() {
        return adminMapper.getAllUsers();
    }

    /**
     * 禁用用户
     */
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

    /**
     * 启用用户
     */
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

    /**
     * 删除用户
     */
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

    /**
     * 获取所有电影
     */
    public List<AdminMovieInfo> getAllMovies() {
        return adminMapper.getAllMoviesForAdmin();
    }

    /**
     * 删除电影
     */
    @Transactional
    public Map<String, Object> deleteMovie(Integer movieId) {
        Map<String, Object> result = new HashMap<>();
        // 先删除关联的收藏和评论
        adminMapper.deleteMovieCollections(movieId);
        adminMapper.deleteMovieComments(movieId);
        // 再删除电影
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

    /**
     * 获取所有评论
     */
    public List<AdminCommentInfo> getAllComments() {
        return adminMapper.getAllComments();
    }

    /**
     * 删除评论
     */
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
     * 获取仪表盘统计数据
     */
    public DashboardStats getDashboardStats() {
        DashboardStats stats = new DashboardStats();

        Map<String, Object> systemStats = adminMapper.getSystemStats();
        if (systemStats != null) {
            stats.setUserCount((Long) systemStats.getOrDefault("userCount", 0L));
            stats.setMovieCount((Long) systemStats.getOrDefault("movieCount", 0L));
            stats.setCommentCount((Long) systemStats.getOrDefault("commentCount", 0L));
            stats.setCollectionCount((Long) systemStats.getOrDefault("collectionCount", 0L));
            stats.setTodayNewUsers((Long) systemStats.getOrDefault("todayNewUsers", 0L));
            stats.setTodayNewComments((Long) systemStats.getOrDefault("todayNewComments", 0L));
        }

        // 获取每日统计数据
        List<DashboardStats.DailyStats> dailyStats = new ArrayList<>();
        List<Map<String, Object>> newUsers = adminMapper.getLast7DaysNewUsers();
        for (Map<String, Object> item : newUsers) {
            DashboardStats.DailyStats ds = new DashboardStats.DailyStats();
            ds.setDate(item.get("date").toString());
            ds.setCount((Long) item.get("count"));
            dailyStats.add(ds);
        }
        stats.setDailyStats(dailyStats);

        return stats;
    }
}