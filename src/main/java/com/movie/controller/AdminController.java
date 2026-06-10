package com.movie.controller;

import com.movie.dto.*;
import com.movie.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    private static final String ADMIN_SESSION_KEY = "ADMIN_LOGIN";

    /**
     * 管理员登录验证
     */
    @PostMapping("/login")
    public ApiResponse<?> adminLogin(@RequestBody AdminLoginRequest request, HttpSession session) {
        if (adminService.validateSecret(request.getSecret())) {
            session.setAttribute(ADMIN_SESSION_KEY, true);
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            return ApiResponse.success("登录成功", data);
        }
        return ApiResponse.error(401, "密钥错误");
    }

    /**
     * 检查管理员登录状态
     */
    @GetMapping("/check")
    public ApiResponse<?> checkLogin(HttpSession session) {
        Boolean isLogin = (Boolean) session.getAttribute(ADMIN_SESSION_KEY);
        if (isLogin != null && isLogin) {
            return ApiResponse.success(true);
        }
        return ApiResponse.success(false);
    }

    /**
     * 管理员退出
     */
    @PostMapping("/logout")
    public ApiResponse<?> adminLogout(HttpSession session) {
        session.removeAttribute(ADMIN_SESSION_KEY);
        return ApiResponse.success("已退出");
    }

    // ========== 数据统计 ==========

    @GetMapping("/dashboard")
    public ApiResponse<?> getDashboardStats(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        DashboardStats stats = adminService.getDashboardStats();
        return ApiResponse.success(stats);
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public ApiResponse<?> getAllUsers(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(adminService.getAllUsers());
    }

    @PutMapping("/user/{userId}/disable")
    public ApiResponse<?> disableUser(@PathVariable Integer userId, HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = adminService.disableUser(userId);
        if ((Boolean) result.get("success")) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PutMapping("/user/{userId}/enable")
    public ApiResponse<?> enableUser(@PathVariable Integer userId, HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = adminService.enableUser(userId);
        if ((Boolean) result.get("success")) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @DeleteMapping("/user/{userId}")
    public ApiResponse<?> deleteUser(@PathVariable Integer userId, HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = adminService.deleteUser(userId);
        if ((Boolean) result.get("success")) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    // ========== 电影管理 ==========

    @GetMapping("/movies")
    public ApiResponse<?> getAllMovies(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(adminService.getAllMovies());
    }

    @DeleteMapping("/movie/{movieId}")
    public ApiResponse<?> deleteMovie(@PathVariable Integer movieId, HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = adminService.deleteMovie(movieId);
        if ((Boolean) result.get("success")) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    // ========== 评论管理 ==========

    @GetMapping("/comments")
    public ApiResponse<?> getAllComments(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(adminService.getAllComments());
    }

    @DeleteMapping("/comment/{commentId}")
    public ApiResponse<?> deleteComment(@PathVariable Integer commentId, HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = adminService.deleteComment(commentId);
        if ((Boolean) result.get("success")) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    // ========== 辅助方法 ==========

    private boolean isAdminLogin(HttpSession session) {
        Boolean isLogin = (Boolean) session.getAttribute(ADMIN_SESSION_KEY);
        return isLogin != null && isLogin;
    }
}