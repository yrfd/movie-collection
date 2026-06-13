package com.movie.mapper;

import com.movie.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper {
    // 基础CRUD
    List<Comment> findCommentsByMovieId(@Param("movieId") Integer movieId);
    Comment findCommentById(@Param("commentId") Integer commentId);
    int insertComment(Comment comment);
    int updateComment(Comment comment);
    int deleteComment(@Param("commentId") Integer commentId);
    List<Comment> findCommentsByUserId(@Param("userId") Integer userId);
    List<Comment> findCommentsByTmdbId(@Param("tmdbId") Integer tmdbId);

    // 根据用户ID和电影ID查找公开评价
    Comment findCommentByUserAndMovie(@Param("userId") Integer userId, @Param("movieId") Integer movieId);

    /**
     * 增加点赞数
     */
    @Update("UPDATE movie_comment SET like_count = like_count + 1 WHERE comment_id = #{commentId}")
    int incrementLikeCount(@Param("commentId") Integer commentId);

    /**
     * 减少点赞数
     */
    @Update("UPDATE movie_comment SET like_count = like_count - 1 WHERE comment_id = #{commentId}")
    int decrementLikeCount(@Param("commentId") Integer commentId);

    /**
     * 获取点赞数
     */
    @Select("SELECT like_count FROM movie_comment WHERE comment_id = #{commentId}")
    int getLikeCount(@Param("commentId") Integer commentId);

    /**
     * 更新用户对某部电影的公开评价中的评分快照
     * 当用户在收藏中修改个人评分时，同步更新公开评价表
     *
     * @param userId 用户ID
     * @param movieId 电影ID
     * @param newRating 新评分
     * @return 更新的记录数（0或1）
     */
    // 只保留方法声明，SQL 在 XML 中定义
    int updateCommentRatingByUserAndMovie(@Param("userId") Integer userId,
                                          @Param("movieId") Integer movieId,
                                          @Param("newRating") Double newRating);
}