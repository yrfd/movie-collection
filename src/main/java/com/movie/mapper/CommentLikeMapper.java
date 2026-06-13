package com.movie.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface CommentLikeMapper {

    /**
     * 检查用户是否已点赞
     */
    @Select("SELECT COUNT(*) FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    boolean isLiked(@Param("userId") Integer userId, @Param("commentId") Integer commentId);

    /**
     * 添加点赞记录
     */
    @Insert("INSERT INTO comment_like (user_id, comment_id, create_time) VALUES (#{userId}, #{commentId}, NOW())")
    int insertLike(@Param("userId") Integer userId, @Param("commentId") Integer commentId);

    /**
     * 删除点赞记录
     */
    @Delete("DELETE FROM comment_like WHERE user_id = #{userId} AND comment_id = #{commentId}")
    int deleteLike(@Param("userId") Integer userId, @Param("commentId") Integer commentId);
}