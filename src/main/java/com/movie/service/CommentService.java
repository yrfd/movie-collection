package com.movie.service;

import com.movie.dto.CommentByTmdbRequest;
import com.movie.dto.CommentRequest;
import com.movie.entity.Comment;
import com.movie.entity.MovieCollection;
import com.movie.entity.MoviePublic;
import com.movie.mapper.CommentMapper;
import com.movie.mapper.MovieMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MovieMapper movieMapper;

    public List<Comment> getCommentsByMovie(Integer movieId) {
        return commentMapper.findCommentsByMovieId(movieId);
    }

    public List<Comment> getCommentsByUserId(Integer userId) {
        return commentMapper.findCommentsByUserId(userId);
    }

    public List<Comment> getCommentsByTmdbId(Integer tmdbId) {
        return commentMapper.findCommentsByTmdbId(tmdbId);
    }

    // 获取电影评分统计（根据movieId）
    public Map<String, Object> getMovieRating(Integer movieId) {
        Map<String, Object> result = new HashMap<>();
        Double avgRating = commentMapper.getAverageRating(movieId);
        Integer count = commentMapper.getRatingCount(movieId);

        result.put("avgRating", avgRating != null ? avgRating : 0.0);
        result.put("count", count != null ? count : 0);
        result.put("avgRatingFormatted", String.format("%.1f", avgRating != null ? avgRating : 0.0));
        result.put("hasRating", count != null && count > 0);

        return result;
    }

    // 获取电影评分统计（根据TMDB ID）
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
            // ✅ 直接从 movie_public 表读取（统一数据源）
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

    @Transactional
    public Map<String, Object> addComment(Integer userId, CommentRequest request) {
        Map<String, Object> result = new HashMap<>();

        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, request.getMovieId());
        if (collection == null) {
            result.put("success", false);
            result.put("message", "请先收藏该电影再进行评价");
            return result;
        }

        if (collection.getPersonalRating() == null || collection.getPersonalRating() == 0) {
            result.put("success", false);
            result.put("message", "请先在收藏中给电影评分（点击星星）再进行评价");
            return result;
        }

        Comment comment = new Comment();
        comment.setMovieId(request.getMovieId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());

        commentMapper.insertComment(comment);

        updateMovieRating(request.getMovieId());

        result.put("success", true);
        result.put("message", "发布成功");
        result.put("commentId", comment.getCommentId());
        result.put("rating", collection.getPersonalRating());
        return result;
    }

    @Transactional
    public Map<String, Object> addCommentByTmdb(Integer userId, CommentByTmdbRequest request) {
        Map<String, Object> result = new HashMap<>();

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

        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, movie.getMovieId());
        if (collection == null) {
            result.put("success", false);
            result.put("message", "请先收藏该电影再进行评价");
            return result;
        }

        Comment existingComment = commentMapper.findCommentByUserAndMovie(userId, movie.getMovieId());
        if (existingComment != null) {
            result.put("success", false);
            result.put("message", "你已经评价过这部电影了");
            return result;
        }

        if (collection.getPersonalRating() == null || collection.getPersonalRating() == 0) {
            result.put("success", false);
            result.put("message", "请先在收藏中给电影评分（点击星星）再进行评价");
            return result;
        }

        Comment comment = new Comment();
        comment.setMovieId(movie.getMovieId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        commentMapper.insertComment(comment);

        updateMovieRating(movie.getMovieId());

        result.put("success", true);
        result.put("message", "评价发布成功");
        result.put("rating", collection.getPersonalRating());
        return result;
    }

    @Transactional
    public Map<String, Object> updateComment(Integer commentId, CommentRequest request) {
        Map<String, Object> result = new HashMap<>();

        Comment comment = commentMapper.findCommentById(commentId);
        if (comment == null) {
            result.put("success", false);
            result.put("message", "评论不存在");
            return result;
        }

        comment.setContent(request.getContent());
        comment.setIsEdited(true);
        commentMapper.updateComment(comment);

        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }

    @Transactional
    public Map<String, Object> deleteComment(Integer commentId) {
        Map<String, Object> result = new HashMap<>();

        Comment comment = commentMapper.findCommentById(commentId);
        if (comment != null) {
            commentMapper.deleteComment(commentId);
            updateMovieRating(comment.getMovieId());
        }

        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    // ✅ 核心方法：更新电影综合评分（从 movie_collection 表计算）
    public void updateMovieRating(Integer movieId) {
        Double avgRating = movieMapper.getAveragePersonalRatingByMovie(movieId);
        Integer count = movieMapper.getRatingCountFromCollections(movieId);

        if (avgRating == null || count == null || count == 0) {
            avgRating = 0.0;
            count = 0;
        }

        int updated = movieMapper.updateMovieRating(movieId, avgRating, count);

        System.out.println("📊 更新电影评分: movieId=" + movieId +
                ", avgRating=" + String.format("%.1f", avgRating) +
                ", 评分人数=" + count +
                ", 结果=" + (updated > 0 ? "成功" : "失败"));
    }
}