package com.movie.controller;

import com.movie.dto.ApiResponse;
import com.movie.dto.MyReviewDTO;
import com.movie.dto.PrivateReviewRequest;
import com.movie.entity.MovieCollection;
import com.movie.service.MovieService;
import com.movie.service.ReviewService;
import com.movie.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private JwtUtil jwtUtil;

    private Integer getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    /**
     * 获取我的所有评价（公开+私人）
     * @param type 类型：all(全部), public(公开), private(私人), both(两者都有)
     */
    @GetMapping("/my/all")
    public ApiResponse<?> getAllMyReviews(
            @RequestParam(required = false, defaultValue = "all") String type,
            HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<MyReviewDTO> reviews = reviewService.getAllMyReviews(userId, type);
        return ApiResponse.success(reviews);
    }

    /**
     * 获取我的私人评价
     */
    @GetMapping("/my/private")
    public ApiResponse<?> getMyPrivateReviews(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        List<MovieCollection> privateReviews = reviewService.getPrivateReviews(userId);
        return ApiResponse.success(privateReviews);
    }

    /**
     * 获取评价统计
     */
    @GetMapping("/my/stats")
    public ApiResponse<?> getReviewStats(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Map<String, Object> stats = reviewService.getReviewStats(userId);
        return ApiResponse.success(stats);
    }

    /**
     * 更新私人评价
     */
    @PutMapping("/private")
    public ApiResponse<?> updatePrivateReview(@RequestBody PrivateReviewRequest request,
                                              HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Map<String, Object> result = movieService.updatePrivateReview(userId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }
}