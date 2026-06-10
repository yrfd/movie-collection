package com.movie.controller;

import com.movie.dto.ApiResponse;
import com.movie.dto.CommentByTmdbRequest;
import com.movie.dto.CommentRequest;
import com.movie.entity.Comment;
import com.movie.entity.MovieCollection;
import com.movie.mapper.MovieMapper;
import com.movie.service.CommentService;

import com.movie.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MovieMapper movieMapper;

    private Integer getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    @GetMapping("/list/{movieId}")
    public ApiResponse<?> getComments(@PathVariable Integer movieId) {
        List<Comment> comments = commentService.getCommentsByMovie(movieId);

        // 可选：为每个评论附加当前的收藏评分
        for (Comment comment : comments) {
            // 获取用户当前的收藏评分
            MovieCollection collection = movieMapper.findCollectionByUserAndMovie(
                    comment.getUserId(), comment.getMovieId());
            if (collection != null) {
                comment.setCurrentRating(collection.getPersonalRating());
            }
        }
        return ApiResponse.success(comments);
    }

    @GetMapping("/my")
    public ApiResponse<?> getMyComments(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        List<Comment> comments = commentService.getCommentsByUserId(userId);
        return ApiResponse.success(comments);
    }

    @GetMapping("/listByTmdb/{tmdbId}")
    public ApiResponse<?> getCommentsByTmdbId(@PathVariable Integer tmdbId) {
        List<Comment> comments = commentService.getCommentsByTmdbId(tmdbId);
        return ApiResponse.success(comments);
    }

    @GetMapping("/average/{movieId}")
    public ApiResponse<?> getAverageRating(@PathVariable Integer movieId) {
        Map<String, Object> rating = commentService.getMovieRating(movieId);
        return ApiResponse.success(rating);
    }

    @GetMapping("/averageByTmdb/{tmdbId}")
    public ApiResponse<?> getAverageRatingByTmdbId(@PathVariable Integer tmdbId) {
        Map<String, Object> rating = commentService.getMovieRatingByTmdbId(tmdbId);
        return ApiResponse.success(rating);
    }

    @PostMapping("/add")
    public ApiResponse<?> addComment(@RequestBody CommentRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = commentService.addComment(userId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            String message = (String) result.get("message");
            Object commentId = result.get("commentId");
            Map<String, Object> data = new HashMap<>();
            data.put("commentId", commentId);
            data.put("rating", result.get("rating"));
            return ApiResponse.success(message, data);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PostMapping("/addByTmdb")
    public ApiResponse<?> addCommentByTmdb(@RequestBody CommentByTmdbRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = commentService.addCommentByTmdb(userId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PutMapping("/update/{commentId}")
    public ApiResponse<?> updateComment(@PathVariable Integer commentId,
                                        @RequestBody CommentRequest request) {
        Map<String, Object> result = commentService.updateComment(commentId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            String message = (String) result.get("message");
            return ApiResponse.success(message);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @DeleteMapping("/delete/{commentId}")
    public ApiResponse<?> deleteComment(@PathVariable Integer commentId) {
        Map<String, Object> result = commentService.deleteComment(commentId);
        String message = (String) result.get("message");
        return ApiResponse.success(message);
    }
}