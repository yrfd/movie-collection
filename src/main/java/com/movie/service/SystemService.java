// SystemService.java
package com.movie.service;

import com.movie.dto.*;
import com.movie.entity.*;
import com.movie.mapper.SystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemService {

    @Autowired
    private SystemMapper systemMapper;

    // ========== 公告管理 ==========
    public List<Announcement> getAllAnnouncements() {
        return systemMapper.getAllAnnouncements();
    }

    public List<Announcement> getActiveAnnouncements() {
        return systemMapper.getActiveAnnouncements();
    }

    public Announcement getAnnouncementById(Integer id) {
        return systemMapper.getAnnouncementById(id);
    }

    public void saveAnnouncement(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setIsActive(request.getIsActive());
        announcement.setPriority(request.getPriority());
        announcement.setTargetType(request.getTargetType());

        // 处理目标用户ID
        if (request.getTargetIds() != null && !request.getTargetIds().isEmpty()) {
            announcement.setTargetIds(String.join(",", request.getTargetIds()));
        }

        if (request.getId() != null && request.getId() > 0) {
            announcement.setId(request.getId());
            systemMapper.updateAnnouncement(announcement);
        } else {
            systemMapper.insertAnnouncement(announcement);
            // 为新公告创建阅读记录（延迟创建，在用户首次访问时创建）
        }
    }

    public List<Announcement> getVisibleAnnouncements(Integer userId) {
        List<Announcement> allAnnouncements = systemMapper.getActiveAnnouncements();
        List<Announcement> visible = new ArrayList<>();

        for (Announcement ann : allAnnouncements) {
            if ("all".equals(ann.getTargetType())) {
                visible.add(ann);
            } else if ("user".equals(ann.getTargetType()) && ann.getTargetIds() != null) {
                String[] targetUserIds = ann.getTargetIds().split(",");
                for (String idStr : targetUserIds) {
                    if (Integer.parseInt(idStr) == userId) {
                        visible.add(ann);
                        break;
                    }
                }
            }
        }
        return visible;
    }

    public void deleteAnnouncement(Integer id) {
        systemMapper.deleteAnnouncement(id);
    }

    // ========== 轮播图管理 ==========
    public List<Carousel> getAllCarousels() {
        return systemMapper.getAllCarousels();
    }

    public List<Carousel> getActiveCarousels() {
        return systemMapper.getActiveCarousels();
    }

    public void saveCarousel(CarouselRequest request) {
        Carousel carousel = new Carousel();
        carousel.setImageUrl(request.getImageUrl());
        carousel.setLinkUrl(request.getLinkUrl());
        carousel.setTitle(request.getTitle());
        carousel.setSortOrder(request.getSortOrder());
        carousel.setIsActive(request.getIsActive());

        if (request.getId() != null && request.getId() > 0) {
            carousel.setId(request.getId());
            systemMapper.updateCarousel(carousel);
        } else {
            systemMapper.insertCarousel(carousel);
        }
    }

    public void deleteCarousel(Integer id) {
        systemMapper.deleteCarousel(id);
    }

    // ========== 操作日志 ==========
    public void log(String adminName, String action, String target, String details, HttpServletRequest request) {
        AdminLog log = new AdminLog();
        log.setAdminName(adminName);
        log.setAction(action);
        log.setTarget(target);
        log.setDetails(details);

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        log.setIp(ip);

        systemMapper.insertAdminLog(log);
    }

    public List<AdminLog> getAdminLogs(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return systemMapper.getAdminLogs(offset, pageSize);
    }

    public int getAdminLogsCount() {
        return systemMapper.countAdminLogs();
    }

    // ========== 系统配置 ==========
    public String getConfig(String key) {
        SystemConfig config = systemMapper.getConfigByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    public List<SystemConfig> getAllConfigs() {
        return systemMapper.getAllConfigs();
    }

    public void updateConfig(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        systemMapper.updateConfig(config);
    }

    public void markAsRead(Integer announcementId, Integer userId) {
        int exists = systemMapper.checkReadStatus(announcementId, userId);
        if (exists > 0) {
            systemMapper.updateReadRecord(announcementId, userId);
        } else {
            systemMapper.insertReadRecord(announcementId, userId);
        }
    }

    public List<Announcement> getAnnouncementsWithStatus(Integer userId) {
        List<Announcement> announcements = systemMapper.getActiveAnnouncements();
        for (Announcement ann : announcements) {
            ann.setIsRead(systemMapper.checkReadStatus(ann.getId(), userId) > 0);
        }
        return announcements;
    }

    public List<Announcement> getAnnouncementsByUserId(Integer userId) {
        if (userId == null) {
            // 未登录用户只能看到全体公告
            return systemMapper.getActiveAnnouncementsByType("all");
        }
        return systemMapper.getAnnouncementsByUserId(userId);
    }
}