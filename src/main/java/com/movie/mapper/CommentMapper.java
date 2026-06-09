package com.movie.mapper;

import com.movie.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> findCommentsByMovieId(@Param("movieId") Integer movieId);
    Comment findCommentById(@Param("commentId") Integer commentId);
    int insertComment(Comment comment);
    int updateComment(Comment comment);
    int deleteComment(@Param("commentId") Integer commentId);
    List<Comment> findCommentsByUserId(@Param("userId") Integer userId);

    // 评分统计
    Double getAverageRating(@Param("movieId") Integer movieId);
    int getRatingCount(@Param("movieId") Integer movieId);
    List<Comment> findCommentsByTmdbId(@Param("tmdbId") Integer tmdbId);
    Double getAverageRatingByTmdbId(@Param("tmdbId") Integer tmdbId);
    Integer getRatingCountByTmdbId(@Param("tmdbId") Integer tmdbId);

    // 根据用户ID和电影ID查找公开评价（用于ReviewService）
    Comment findCommentByUserAndMovie(@Param("userId") Integer userId, @Param("movieId") Integer movieId);
}