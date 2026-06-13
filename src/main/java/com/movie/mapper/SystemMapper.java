// SystemMapper.java
package com.movie.mapper;

import com.movie.entity.Announcement;
import com.movie.entity.Carousel;
import com.movie.entity.AdminLog;
import com.movie.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SystemMapper {

    // ========== 公告管理 ==========
    List<Announcement> getAllAnnouncements();
    List<Announcement> getActiveAnnouncements();
    Announcement getAnnouncementById(@Param("id") Integer id);
    int insertAnnouncement(Announcement announcement);
    int updateAnnouncement(Announcement announcement);
    int deleteAnnouncement(@Param("id") Integer id);

    // ========== 轮播图管理 ==========
    List<Carousel> getAllCarousels();
    List<Carousel> getActiveCarousels();
    Carousel getCarouselById(@Param("id") Integer id);
    int insertCarousel(Carousel carousel);
    int updateCarousel(Carousel carousel);
    int deleteCarousel(@Param("id") Integer id);

    // ========== 操作日志 ==========
    int insertAdminLog(AdminLog log);
    List<AdminLog> getAdminLogs(@Param("offset") int offset, @Param("limit") int limit);
    int countAdminLogs();

    // ========== 系统配置 ==========
    SystemConfig getConfigByKey(@Param("configKey") String configKey);
    List<SystemConfig> getAllConfigs();
    int updateConfig(SystemConfig config);

    int insertReadRecord(@Param("announcementId") Integer announcementId,
                         @Param("userId") Integer userId);
    int updateReadRecord(@Param("announcementId") Integer announcementId,
                         @Param("userId") Integer userId);
    int checkReadStatus(@Param("announcementId") Integer announcementId,
                        @Param("userId") Integer userId);
    List<Integer> getReadUserIds(@Param("announcementId") Integer announcementId);
    List<Announcement> getAnnouncementsByUserId(@Param("userId") Integer userId);
    List<Announcement> getActiveAnnouncementsByType(@Param("targetType") String targetType);
}