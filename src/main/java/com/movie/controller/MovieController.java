package com.movie.controller;

import com.movie.dto.ApiResponse;
import com.movie.dto.CategoryRequest;
import com.movie.dto.MovieRequest;
import com.movie.entity.MovieCategory;
import com.movie.entity.MovieCollection;
import com.movie.entity.MoviePublic;
import com.movie.service.MovieService;
import com.movie.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movie")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.movie.mapper.MovieMapper movieMapper;

    private Integer getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    // ========== 收藏管理接口 ==========

    @GetMapping("/list")
    public ApiResponse<?> getMovieList(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        List<MovieCollection> movies = movieService.getUserCollections(userId);
        return ApiResponse.success(movies);
    }

    @PostMapping("/add")
    public ApiResponse<?> addMovie(@RequestBody MovieRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = movieService.addCollection(userId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            String message = (String) result.get("message");
            Object collectionId = result.get("collectionId");
            Map<String, Object> data = new HashMap<>();
            data.put("collectionId", collectionId);
            return ApiResponse.success(message, data);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PutMapping("/update/{collectionId}")
    public ApiResponse<?> updateMovie(@PathVariable Integer collectionId,
                                      @RequestBody MovieRequest request) {
        Map<String, Object> result = movieService.updateCollection(collectionId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @DeleteMapping("/delete/{collectionId}")
    public ApiResponse<?> deleteMovie(@PathVariable Integer collectionId) {
        Map<String, Object> result = movieService.deleteCollection(collectionId);
        return ApiResponse.success((String) result.get("message"));
    }

    @GetMapping("/search")
    public ApiResponse<?> searchMovies(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String director,
                                       @RequestParam(required = false) Double minRating,
                                       @RequestParam(required = false) String region,
                                       @RequestParam(required = false) String genre,
                                       @RequestParam(required = false) Integer categoryId,
                                       @RequestParam(required = false) String watchStatus,
                                       @RequestParam(required = false) Integer minYear,
                                       @RequestParam(required = false) Integer maxYear,
                                       @RequestParam(required = false) String sortBy,
                                       HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        List<MovieCollection> movies = movieService.searchMovies(userId, keyword, director, minRating,
                region, genre, categoryId, watchStatus,
                minYear, maxYear, sortBy);
        return ApiResponse.success(movies);
    }

    // ========== 排行榜接口 ==========

    @GetMapping("/rankings")
    public ApiResponse<?> getMovieRankings(@RequestParam String type) {
        List<Map<String, Object>> rankings;
        if ("popular".equals(type)) {
            rankings = movieService.getMoviesOrderByCommentCount();
        } else {
            rankings = movieService.getMoviesOrderByRating();
        }
        return ApiResponse.success(rankings);
    }

    // ========== 分类管理接口 ==========

    @GetMapping("/categories")
    public ApiResponse<?> getCategories(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        List<MovieCategory> categories = movieService.getUserCategories(userId);
        return ApiResponse.success(categories);
    }

    @PostMapping("/categories")
    public ApiResponse<?> createCategory(@RequestBody CategoryRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = movieService.createCategory(userId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<?> deleteCategory(@PathVariable Integer categoryId, HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = movieService.deleteCategory(userId, categoryId);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PutMapping("/collections/{collectionId}/move-to/{categoryId}")
    public ApiResponse<?> moveToCategory(@PathVariable Integer collectionId,
                                         @PathVariable Integer categoryId) {
        Map<String, Object> result = movieService.moveToCategory(collectionId, categoryId);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PostMapping("/collections/batch-move-to/{categoryId}")
    public ApiResponse<?> batchMoveToCategory(@RequestBody List<Integer> collectionIds,
                                              @PathVariable Integer categoryId,
                                              HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = movieService.batchMoveToCategory(userId, collectionIds, categoryId);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @GetMapping("/collections/by-category/{categoryId}")
    public ApiResponse<?> getCollectionsByCategory(@PathVariable Integer categoryId, HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        List<MovieCollection> movies = movieService.getCollectionsByCategory(userId, categoryId);
        return ApiResponse.success(movies);
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<?> updateCategory(@PathVariable Integer categoryId,
                                         @RequestBody CategoryRequest request,
                                         HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = movieService.updateCategory(userId, categoryId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @GetMapping("/info/{movieId}")
    public ApiResponse<?> getMovieInfo(@PathVariable Integer movieId) {
        MoviePublic movie = movieMapper.findMovieById(movieId);
        if (movie == null) {
            return ApiResponse.error(404, "电影不存在");
        }
        return ApiResponse.success(movie);
    }

    /**
     * 添加电影到公共库（不创建收藏记录）
     * 用于用户未收藏但想查看详情/评价的场景
     */
    @PostMapping("/add-to-library")
    public ApiResponse<?> addMovieToLibrary(@RequestBody MovieRequest request) {
        // 查找或创建公共电影（不创建收藏记录）
        MoviePublic movie = movieMapper.findMovieByName(request.getMovieName());
        if (movie == null) {
            movie = new MoviePublic();
            movie.setTmdbId(request.getTmdbId());
            movie.setMovieName(request.getMovieName());
            movie.setDirector(request.getDirector() != null ? request.getDirector() : "");
            movie.setYear(request.getYear() != null ? request.getYear() : 0);
            movie.setRegion(request.getRegion() != null ? request.getRegion() : "");
            movie.setGenre(request.getGenre() != null ? request.getGenre() : "");
            movie.setPosterUrl(request.getPosterUrl() != null ? request.getPosterUrl() : "");
            movieMapper.insertMovie(movie);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("movieId", movie.getMovieId());
        return ApiResponse.success("成功", data);
    }
}