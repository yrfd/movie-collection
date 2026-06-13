package com.movie.service;

import com.movie.dto.CommentByTmdbRequest;
import com.movie.dto.CommentRequest;
import com.movie.dto.ReplyRequest;
import com.movie.entity.Comment;
import com.movie.entity.MovieCollection;
import com.movie.entity.MoviePublic;
import com.movie.mapper.CommentLikeMapper;
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

    @Autowired
    private CommentLikeMapper commentLikeMapper;


    public List<Comment> getCommentsByMovie(Integer movieId) {
        return commentMapper.findCommentsByMovieId(movieId);
    }

    public List<Comment> getCommentsByUserId(Integer userId) {
        return commentMapper.findCommentsByUserId(userId);
    }

    public List<Comment> getCommentsByTmdbId(Integer tmdbId) {
        return commentMapper.findCommentsByTmdbId(tmdbId);
    }

    /**
     * 获取电影评分统计 - 从收藏表统计
     */
    public Map<String, Object> getMovieRating(Integer movieId) {
        Map<String, Object> result = new HashMap<>();
        Double avgRating = movieMapper.getAveragePersonalRatingByMovie(movieId);
        Integer count = movieMapper.getRatingCountFromCollections(movieId);

        result.put("avgRating", avgRating != null ? avgRating : 0.0);
        result.put("count", count != null ? count : 0);
        result.put("avgRatingFormatted", String.format("%.1f", avgRating != null ? avgRating : 0.0));
        result.put("hasRating", count != null && count > 0);

        return result;
    }

    /**
     * 获取电影评分统计（根据TMDB ID）
     */
    public Map<String, Object> getMovieRatingByTmdbId(Integer tmdbId) {
        MoviePublic movie = movieMapper.findMovieByTmdbId(tmdbId);
        if (movie == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("avgRating", 0.0);
            result.put("count", 0);
            result.put("hasRating", false);
            return result;
        }
        return getMovieRating(movie.getMovieId());
    }

    /**
     * 发布评论 - 未收藏也能评论，评分为0
     */
    @Transactional
    public Map<String, Object> addComment(Integer userId, CommentRequest request) {
        Map<String, Object> result = new HashMap<>();

        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, request.getMovieId());

        Comment existingComment = commentMapper.findCommentByUserAndMovie(userId, request.getMovieId());
        if (existingComment != null) {
            result.put("success", false);
            result.put("message", "你已经评价过这部电影了");
            return result;
        }

        Double rating = 0.0;
        boolean isRated = false;
        if (collection != null && collection.getPersonalRating() != null && collection.getPersonalRating() > 0) {
            rating = collection.getPersonalRating();
            isRated = true;
        }

        Comment comment = new Comment();
        comment.setMovieId(request.getMovieId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setReplyTo(0);

        commentMapper.insertComment(comment);

        result.put("success", true);
        result.put("message", "发布成功");
        result.put("commentId", comment.getCommentId());
        result.put("rating", rating);
        result.put("isRated", isRated);
        if (!isRated) {
            result.put("warning", "您尚未收藏该电影或未评分");
        }
        return result;
    }

    /**
     * 回复他人评论
     */
    @Transactional
    public Map<String, Object> replyComment(Integer userId, ReplyRequest request) {
        Map<String, Object> result = new HashMap<>();

        Comment parentComment = commentMapper.findCommentById(request.getParentCommentId());
        if (parentComment == null) {
            result.put("success", false);
            result.put("message", "要回复的评论不存在");
            return result;
        }

        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, parentComment.getMovieId());
        Double rating = 0.0;
        if (collection != null && collection.getPersonalRating() != null && collection.getPersonalRating() > 0) {
            rating = collection.getPersonalRating();
        }

        Comment reply = new Comment();
        reply.setMovieId(parentComment.getMovieId());
        reply.setUserId(userId);
        reply.setContent(request.getContent());
        reply.setReplyTo(parentComment.getCommentId());

        commentMapper.insertComment(reply);

        result.put("success", true);
        result.put("message", "回复成功");
        result.put("commentId", reply.getCommentId());
        result.put("rating", rating);
        return result;
    }

    /**
     * 通过TMDB发布评论
     */
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

        Comment existingComment = commentMapper.findCommentByUserAndMovie(userId, movie.getMovieId());
        if (existingComment != null) {
            result.put("success", false);
            result.put("message", "你已经评价过这部电影了");
            return result;
        }

        Double rating = 0.0;
        boolean isRated = false;
        if (collection != null && collection.getPersonalRating() != null && collection.getPersonalRating() > 0) {
            rating = collection.getPersonalRating();
            isRated = true;
        }

        Comment comment = new Comment();
        comment.setMovieId(movie.getMovieId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setReplyTo(0);

        commentMapper.insertComment(comment);

        result.put("success", true);
        result.put("message", "评价发布成功");
        result.put("rating", rating);
        result.put("isRated", isRated);
        if (!isRated) {
            result.put("warning", "您尚未收藏该电影或未评分");
        }
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
        }

        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    /**
     * 更新电影综合评分（从收藏表统计）
     * 供 MovieService 调用
     */
    public void updateMovieRatingFromCollections(Integer movieId) {
        Double avgRating = movieMapper.getAveragePersonalRatingByMovie(movieId);
        Integer count = movieMapper.getRatingCountFromCollections(movieId);

        if (count == null || count == 0) {
            avgRating = 0.0;
            count = 0;
        }

        movieMapper.updateMovieRating(movieId, avgRating, count);

        System.out.println("📊 更新电影评分: movieId=" + movieId +
                ", avgRating=" + avgRating +
                ", 评分人数=" + count);
    }

    @Transactional
    public Map<String, Object> toggleLike(Integer userId, Integer commentId) {
        Map<String, Object> result = new HashMap<>();

        // 检查评论是否存在
        Comment comment = commentMapper.findCommentById(commentId);
        if (comment == null) {
            result.put("success", false);
            result.put("message", "评论不存在");
            return result;
        }

        // 检查是否已点赞
        boolean liked = commentLikeMapper.isLiked(userId, commentId);

        if (liked) {
            // 取消点赞
            commentLikeMapper.deleteLike(userId, commentId);
            // 减少点赞数
            commentMapper.decrementLikeCount(commentId);
            result.put("success", true);
            result.put("message", "取消点赞");
            result.put("liked", false);
        } else {
            // 添加点赞
            commentLikeMapper.insertLike(userId, commentId);
            // 增加点赞数
            commentMapper.incrementLikeCount(commentId);
            result.put("success", true);
            result.put("message", "点赞成功");
            result.put("liked", true);
        }

        // 获取最新点赞数
        int newLikeCount = commentMapper.getLikeCount(commentId);
        result.put("likeCount", newLikeCount);

        return result;
    }

    public boolean isLiked(Integer userId, Integer commentId) {
        return commentLikeMapper.isLiked(userId, commentId);
    }
}