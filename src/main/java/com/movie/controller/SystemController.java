package com.movie.controller;

import com.movie.dto.ApiResponse;
import com.movie.dto.AnnouncementRequest;
import com.movie.dto.CarouselRequest;
import com.movie.entity.Announcement;
import com.movie.entity.Carousel;
import com.movie.entity.AdminLog;
import com.movie.entity.SystemConfig;
import com.movie.service.SystemService;
import com.movie.util.JwtUtil;  // ✅ 添加导入
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/system")
public class SystemController {

    @Autowired
    private SystemService systemService;

    @Autowired
    private JwtUtil jwtUtil;  // ✅ 添加注入

    private static final String ADMIN_SESSION_KEY = "ADMIN_LOGIN";

    private boolean isAdminLogin(HttpSession session) {
        Boolean isLogin = (Boolean) session.getAttribute(ADMIN_SESSION_KEY);
        return isLogin != null && isLogin;
    }

    // ========== 公告管理 ==========

    @GetMapping("/announcements")
    public ApiResponse<?> getAnnouncements(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        List<Announcement> list = systemService.getAllAnnouncements();
        return ApiResponse.success(list);
    }

    @GetMapping("/announcements/active")
    public ApiResponse<?> getActiveAnnouncements() {
        List<Announcement> list = systemService.getActiveAnnouncements();
        return ApiResponse.success(list);
    }

    @PostMapping("/announcement")
    public ApiResponse<?> saveAnnouncement(@RequestBody AnnouncementRequest request,
                                           HttpSession session, HttpServletRequest req) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        systemService.saveAnnouncement(request);
        systemService.log(adminName, "保存公告", request.getTitle(),
                request.getId() == null ? "新增" : "编辑", req);
        return ApiResponse.success("保存成功");
    }

    @DeleteMapping("/announcement/{id}")
    public ApiResponse<?> deleteAnnouncement(@PathVariable Integer id,
                                             HttpSession session, HttpServletRequest req) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        systemService.deleteAnnouncement(id);
        systemService.log(adminName, "删除公告", "ID:" + id, null, req);
        return ApiResponse.success("删除成功");
    }

    // ========== 轮播图管理 ==========

    @GetMapping("/carousels")
    public ApiResponse<?> getCarousels(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        List<Carousel> list = systemService.getAllCarousels();
        return ApiResponse.success(list);
    }

    @GetMapping("/carousels/active")
    public ApiResponse<?> getActiveCarousels() {
        List<Carousel> list = systemService.getActiveCarousels();
        return ApiResponse.success(list);
    }

    @PostMapping("/carousel")
    public ApiResponse<?> saveCarousel(@RequestBody CarouselRequest request,
                                       HttpSession session, HttpServletRequest req) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        systemService.saveCarousel(request);
        systemService.log(adminName, "保存轮播图", request.getTitle(),
                request.getId() == null ? "新增" : "编辑", req);
        return ApiResponse.success("保存成功");
    }

    @DeleteMapping("/carousel/{id}")
    public ApiResponse<?> deleteCarousel(@PathVariable Integer id,
                                         HttpSession session, HttpServletRequest req) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        systemService.deleteCarousel(id);
        systemService.log(adminName, "删除轮播图", "ID:" + id, null, req);
        return ApiResponse.success("删除成功");
    }

    // ========== 操作日志 ==========

    @GetMapping("/logs")
    public ApiResponse<?> getLogs(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        List<AdminLog> list = systemService.getAdminLogs(page, pageSize);
        int total = systemService.getAdminLogsCount();
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        return ApiResponse.success(result);
    }

    // ========== 系统配置 ==========

    @GetMapping("/configs")
    public ApiResponse<?> getConfigs(HttpSession session) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        List<SystemConfig> list = systemService.getAllConfigs();
        return ApiResponse.success(list);
    }

    @PutMapping("/config")
    public ApiResponse<?> updateConfig(@RequestBody SystemConfig config,
                                       HttpSession session, HttpServletRequest req) {
        if (!isAdminLogin(session)) {
            return ApiResponse.error(401, "未登录");
        }
        String adminName = (String) session.getAttribute("ADMIN_NAME");
        systemService.updateConfig(config.getConfigKey(), config.getConfigValue());
        systemService.log(adminName, "修改配置", config.getConfigKey(), config.getConfigValue(), req);
        return ApiResponse.success("保存成功");
    }

    // ========== 用户端公告接口 ==========

    @GetMapping("/user/announcements")
    public ApiResponse<?> getUserAnnouncements(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        List<Announcement> announcements = systemService.getAnnouncementsByUserId(userId);
        return ApiResponse.success(announcements);
    }

    @GetMapping("/public/announcements")
    public ApiResponse<?> getPublicAnnouncements() {
        List<Announcement> announcements = systemService.getAnnouncementsByUserId(null);
        return ApiResponse.success(announcements);
    }

    // 辅助方法：从token获取用户ID
    private Integer getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }
}