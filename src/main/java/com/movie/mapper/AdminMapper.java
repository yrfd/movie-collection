package com.movie.mapper;

import com.movie.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {

    // ========== 用户管理 ==========

    /** 获取所有用户列表（含统计信息） */
    List<AdminUserInfo> getAllUsers();

    /** 禁用用户 */
    int disableUser(@Param("userId") Integer userId);

    /** 启用用户 */
    int enableUser(@Param("userId") Integer userId);

    /** 删除用户 */
    int deleteUser(@Param("userId") Integer userId);

    /** 检查用户是否存在 */
    int checkUserExists(@Param("userId") Integer userId);

    // ========== 电影管理 ==========

    /** 获取所有电影列表（管理员视图） */
    List<AdminMovieInfo> getAllMoviesForAdmin();

    /** 删除电影（级联删除收藏和评论） */
    int deleteMovie(@Param("movieId") Integer movieId);

    /** 删除电影的所有收藏记录 */
    int deleteMovieCollections(@Param("movieId") Integer movieId);

    /** 删除电影的所有评论 */
    int deleteMovieComments(@Param("movieId") Integer movieId);

    // ========== 评论管理 ==========

    /** 获取所有评论 */
    List<AdminCommentInfo> getAllComments();

    /** 删除评论 */
    int deleteComment(@Param("commentId") Integer commentId);

    // ========== 数据统计 ==========

    /** 获取系统统计概览 */
    Map<String, Object> getSystemStats();

    /** 获取近7天新增用户数 */
    List<Map<String, Object>> getLast7DaysNewUsers();

    /** 获取近7天新增评论数 */
    List<Map<String, Object>> getLast7DaysNewComments();
}