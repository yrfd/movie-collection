package com.movie.service;

import com.movie.dto.CommentByTmdbRequest;
import com.movie.dto.CommentRequest;
import com.movie.entity.Comment;
import com.movie.entity.MovieCollection;
import com.movie.entity.MoviePublic;
import com.movie.mapper.CommentMapper;
import com.movie.mapper.MovieMapper;
import com.movie.mapper.RatingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    @Autowired
    private RatingMapper ratingMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MovieMapper movieMapper;

    // 获取电影评论（根据movieId）
    public List<Comment> getCommentsByMovie(Integer movieId) {
        return commentMapper.findCommentsByMovieId(movieId);
    }

    // 获取用户自己的所有评论
    public List<Comment> getCommentsByUserId(Integer userId) {
        return commentMapper.findCommentsByUserId(userId);
    }

    // 根据TMDB ID获取评论
    public List<Comment> getCommentsByTmdbId(Integer tmdbId) {
        return commentMapper.findCommentsByTmdbId(tmdbId);
    }

    // 获取电影评分统计（根据movieId）- 修复问题7
    public Map<String, Object> getMovieRating(Integer movieId) {
        Map<String, Object> result = new HashMap<>();
        Double avgRating = commentMapper.getAverageRating(movieId);
        Integer count = commentMapper.getRatingCount(movieId);

        // 处理空值，返回0而不是null
        result.put("avgRating", avgRating != null ? avgRating : 0.0);
        result.put("count", count != null ? count : 0);
        result.put("avgRatingFormatted", String.format("%.1f", avgRating != null ? avgRating : 0.0));
        result.put("hasRating", count != null && count > 0);

        return result;
    }

    // 获取电影评分统计（根据TMDB ID）- 修复问题7
    public Map<String, Object> getMovieRatingByTmdbId(Integer tmdbId) {
        Map<String, Object> result = new HashMap<>();

        if (tmdbId == null) {
            result.put("avgRating", 0.0);
            result.put("count", 0);
            result.put("avgRatingFormatted", "0.0");
            result.put("hasRating", false);
            return result;
        }

        try {
            // ✅ 直接从 movie_public 表读取
            MoviePublic movie = movieMapper.findMovieByTmdbId(tmdbId);

            if (movie != null && movie.getAvgRating() != null && movie.getAvgRating() > 0) {
                result.put("avgRating", movie.getAvgRating());
                result.put("count", movie.getRatingCount() != null ? movie.getRatingCount() : 0);
                result.put("avgRatingFormatted", String.format("%.1f", movie.getAvgRating()));
                result.put("hasRating", true);
            } else {
                result.put("avgRating", 0.0);
                result.put("count", 0);
                result.put("avgRatingFormatted", "0.0");
                result.put("hasRating", false);
            }
        } catch (Exception e) {
            System.err.println("获取电影评分失败, tmdbId: " + tmdbId);
            e.printStackTrace();
            result.put("avgRating", 0.0);
            result.put("count", 0);
            result.put("avgRatingFormatted", "0.0");
            result.put("hasRating", false);
        }

        return result;
    }

    /**
     * 发布评论 - 自动使用收藏中的评分（修复问题5）
     */
    @Transactional
    public Map<String, Object> addComment(Integer userId, CommentRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 检查用户是否收藏了该电影
        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, request.getMovieId());
        if (collection == null) {
            result.put("success", false);
            result.put("message", "请先收藏该电影再进行评价");
            return result;
        }

        // 检查用户是否已经评分
        if (collection.getPersonalRating() == null || collection.getPersonalRating() == 0) {
            result.put("success", false);
            result.put("message", "请先在收藏中给电影评分（点击星星）再进行评价");
            return result;
        }

        // 创建公开评论
        Comment comment = new Comment();
        comment.setMovieId(request.getMovieId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());

        commentMapper.insertComment(comment);

        // ✅ 确保评分表中有记录
        ratingMapper.insertOrUpdateRating(userId, request.getMovieId(), collection.getPersonalRating());

        // 更新电影综合评分
        updateMovieRating(request.getMovieId());

        result.put("success", true);
        result.put("message", "发布成功");
        result.put("commentId", comment.getCommentId());
        result.put("rating", collection.getPersonalRating());
        return result;
    }

    /**
     * 通过TMDB发布评论 - 自动使用收藏中的评分（修复问题5）
     */
    @Transactional
    public Map<String, Object> addCommentByTmdb(Integer userId, CommentByTmdbRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查找或创建电影
        MoviePublic movie = movieMapper.findMovieByTmdbId(request.getTmdbId());
        if (movie == null) {
            movie = new MoviePublic();
            movie.setTmdbId(request.getTmdbId());
            movie.setMovieName(request.getMovieName());
            movie.setPosterUrl(request.getPosterUrl());
            movie.setYear(request.getYear());
            movie.setDirector(request.getDirector());
            movie.setGenre(request.getGenre());
            movie.setRegion(request.getRegion());
            movieMapper.insertMovie(movie);
        }

        // 2. 检查用户是否收藏了该电影
        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, movie.getMovieId());
        if (collection == null) {
            result.put("success", false);
            result.put("message", "请先收藏该电影再进行评价");
            return result;
        }

        // 3. 检查用户是否已经有公开评价
        Comment existingComment = commentMapper.findCommentByUserAndMovie(userId, movie.getMovieId());
        if (existingComment != null) {
            result.put("success", false);
            result.put("message", "你已经评价过这部电影了");
            return result;
        }

        // 4. 检查用户是否已经评分
        if (collection.getPersonalRating() == null || collection.getPersonalRating() == 0) {
            result.put("success", false);
            result.put("message", "请先在收藏中给电影评分（点击星星）再进行评价");
            return result;
        }

        // 5. 创建公开评论，评分使用收藏中的评分
        Comment comment = new Comment();
        comment.setMovieId(movie.getMovieId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        commentMapper.insertComment(comment);

        // 6. 更新电影综合评分
        updateMovieRating(movie.getMovieId());

        result.put("success", true);
        result.put("message", "评价发布成功");
        result.put("rating", collection.getPersonalRating());
        return result;
    }

    // 更新评论（评分仍然使用收藏中的评分）
    @Transactional
    public Map<String, Object> updateComment(Integer commentId, CommentRequest request) {
        Map<String, Object> result = new HashMap<>();

        Comment comment = commentMapper.findCommentById(commentId);
        if (comment == null) {
            result.put("success", false);
            result.put("message", "评论不存在");
            return result;
        }

        // 获取用户的收藏评分
        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(comment.getUserId(), comment.getMovieId());
        Double ratingFromCollection = collection != null && collection.getPersonalRating() != null
                ? collection.getPersonalRating() : 0.0;

        // 更新评论时，评分仍然使用收藏中的评分
        comment.setContent(request.getContent());
        comment.setIsEdited(true);

        commentMapper.updateComment(comment);

        // 更新电影综合评分

        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }

    // 删除评论
    @Transactional
    public Map<String, Object> deleteComment(Integer commentId) {
        Map<String, Object> result = new HashMap<>();

        Comment comment = commentMapper.findCommentById(commentId);
        if (comment != null) {
            commentMapper.deleteComment(commentId);
        }

        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // 更新电影综合
    public void updateMovieRating(Integer movieId) {
        // 从 movie_collection 表计算
        Double avgRating = movieMapper.getAveragePersonalRatingByMovie(movieId);
        Integer count = movieMapper.getRatingCountFromCollections(movieId);

        if (avgRating == null || count == null || count == 0) {
            avgRating = 0.0;
            count = 0;
        }

        // 更新 movie_public 表
        movieMapper.updateMovieRating(movieId, avgRating, count);
        System.out.println("📊 更新电影评分: movieId=" + movieId +
                ", avgRating=" + avgRating + ", 人数=" + count);
    }

    private void updateMovieRatingFromCollections(Integer movieId) {
        // 从收藏表计算平均分
        Double avgRating = movieMapper.getAveragePersonalRatingByMovie(movieId);
        Integer count = movieMapper.getRatingCountFromCollections(movieId);

        if (count == null || count == 0) {
            avgRating = 0.0;
            count = 0;
        }

        movieMapper.updateMovieRating(movieId, avgRating, count);

        System.out.println("更新电影综合评分: movieId=" + movieId +
                ", avgRating=" + avgRating +
                ", 评分人数=" + count);
    }
}