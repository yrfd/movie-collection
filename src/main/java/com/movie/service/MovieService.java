package com.movie.service;

import com.movie.dto.CategoryRequest;
import com.movie.dto.MovieRequest;
import com.movie.entity.MovieCategory;
import com.movie.dto.PrivateReviewRequest;
import com.movie.entity.MovieCategory;
import com.movie.entity.MovieCollection;
import com.movie.entity.MoviePublic;
import com.movie.mapper.CommentMapper;
import com.movie.mapper.MovieMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MovieService {

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private RestTemplate restTemplate;

    // 地区映射（解决中英文匹配问题）
    private static final Map<String, String> REGION_MAP = new HashMap<>();

    static {
        REGION_MAP.put("中国大陆", "China");
        REGION_MAP.put("中国香港", "Hong Kong");
        REGION_MAP.put("中国台湾", "Taiwan");
        REGION_MAP.put("美国", "United States of America");
        REGION_MAP.put("英国", "United Kingdom");
        REGION_MAP.put("日本", "Japan");
        REGION_MAP.put("韩国", "South Korea");
        REGION_MAP.put("法国", "France");
        REGION_MAP.put("德国", "Germany");
        REGION_MAP.put("意大利", "Italy");
        REGION_MAP.put("西班牙", "Spain");
        REGION_MAP.put("印度", "India");
        REGION_MAP.put("泰国", "Thailand");
        REGION_MAP.put("俄罗斯", "Russia");
        REGION_MAP.put("加拿大", "Canada");
        REGION_MAP.put("澳大利亚", "Australia");
    }

    /**
     * 转换地区中文到英文
     */
    private String convertRegionToEnglish(String chineseRegion) {
        if (chineseRegion == null || chineseRegion.isEmpty()) {
            return null;
        }
        return REGION_MAP.getOrDefault(chineseRegion, chineseRegion);
    }

    // ========== 收藏管理 ==========

    public List<MovieCollection> getUserCollections(Integer userId) {
        return movieMapper.findCollectionsByUserId(userId);
    }

    // 添加电影收藏
    @Transactional
    public Map<String, Object> addCollection(Integer userId, MovieRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 查找或创建公共电影
        MoviePublic movie = movieMapper.findMovieByName(request.getMovieName());
        if (movie == null) {
            movie = new MoviePublic();
            movie.setTmdbId(request.getTmdbId());
            movie.setMovieName(request.getMovieName());
            movie.setDirector(request.getDirector());
            movie.setYear(request.getYear());
            movie.setRegion(request.getRegion());
            movie.setGenre(request.getGenre());
            movie.setPosterUrl(request.getPosterUrl());
            movie.setActors(request.getActors());

            movieMapper.insertMovie(movie);
        } else {
            // 如果电影已存在但演员信息为空，且请求中有演员信息，则更新演员信息
            if ((movie.getActors() == null || movie.getActors().isEmpty())
                    && request.getActors() != null && !request.getActors().isEmpty()) {
                movie.setActors(request.getActors());
                movieMapper.updateMovieActors(movie.getMovieId(), request.getActors());
            }
        }

        // 检查是否已收藏
        MovieCollection existing = movieMapper.findCollectionByUserAndMovie(userId, movie.getMovieId());
        if (existing != null) {
            // 如果已收藏，直接返回成功（不重复添加）
            result.put("success", true);
            result.put("message", "电影已在收藏列表中");
            result.put("collectionId", existing.getCollectionId());
            return result;
        }

        // 添加收藏
        MovieCollection collection = new MovieCollection();
        collection.setUserId(userId);
        collection.setMovieId(movie.getMovieId());
        collection.setPersonalRating(request.getPersonalRating() != null ? request.getPersonalRating() : 0);
        collection.setWatchStatus(request.getWatchStatus() != null ? request.getWatchStatus() : "想看");
        collection.setPrivateReview(request.getPrivateReview() != null ? request.getPrivateReview() : "");
        collection.setCategoryId(request.getCategoryId());

        movieMapper.insertCollection(collection);

        // 如果有评分，立即更新综合评分
        if (request.getPersonalRating() != null && request.getPersonalRating() > 0) {
            commentService.updateMovieRating(movie.getMovieId());
        }

        result.put("success", true);
        result.put("message", "添加成功");
        result.put("collectionId", collection.getCollectionId());
        return result;
    }

    // 更新电影收藏
    @Transactional
    public Map<String, Object> updateCollection(Integer collectionId, MovieRequest request) {
        Map<String, Object> result = new HashMap<>();

        MovieCollection collection = movieMapper.findCollectionById(collectionId);
        if (collection == null) {
            result.put("success", false);
            result.put("message", "收藏记录不存在");
            return result;
        }

        Double oldRating = collection.getPersonalRating();
        Double newRating = request.getPersonalRating();

        // 更新收藏信息
        collection.setPersonalRating(request.getPersonalRating());
        collection.setWatchStatus(request.getWatchStatus());
        collection.setPrivateReview(request.getPrivateReview());

        movieMapper.updateCollection(collection);

        // 如果用户有公开评价，同步更新公开评价中的评分快照
        if (newRating != null && newRating > 0) {
            int updated = commentMapper.updateCommentRatingByUserAndMovie(
                    collection.getUserId(),
                    collection.getMovieId(),
                    newRating
            );
            if (updated > 0) {
                System.out.println("✅ 已同步更新公开评价评分: userId=" + collection.getUserId() +
                        ", movieId=" + collection.getMovieId() + ", rating=" + newRating);
            }
        }

        // 评分发生变化时，更新综合评分
        boolean hasOldRating = oldRating != null && oldRating > 0;
        boolean hasNewRating = newRating != null && newRating > 0;

        if (hasOldRating != hasNewRating || (hasOldRating && hasNewRating && !oldRating.equals(newRating))) {
            commentService.updateMovieRating(collection.getMovieId());
            System.out.println("✅ 评分变化，已更新综合评分: movieId=" + collection.getMovieId() +
                    ", 旧评分=" + oldRating + ", 新评分=" + newRating);
        }

        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }

    // 删除电影收藏
    public Map<String, Object> deleteCollection(Integer collectionId) {
        Map<String, Object> result = new HashMap<>();

        MovieCollection collection = movieMapper.findCollectionById(collectionId);
        if (collection != null) {
            movieMapper.deleteCollection(collectionId);
            // 删除后重新计算该电影的综合评分
            commentService.updateMovieRating(collection.getMovieId());
        }

        result.put("success", true);
        result.put("message", "删除成功");
        return result;
    }

    public List<MovieCollection> searchMovies(Integer userId, String keyword, String director,
                                              Double minRating, String region, String genre,
                                              Integer categoryId, String watchStatus,
                                              Integer minYear, Integer maxYear, String sortBy) {
        // 转换地区为英文
        String englishRegion = convertRegionToEnglish(region);
        return movieMapper.searchCollections(userId, keyword, director, minRating, englishRegion,
                genre, categoryId, watchStatus, minYear, maxYear, sortBy);
    }

    /**
     * 获取按评分排序的电影排行榜
     */
    public List<Map<String, Object>> getMoviesOrderByRating() {
        return movieMapper.selectMoviesOrderByRating();
    }

    /**
     * 获取按评论数排序的电影排行榜
     */
    public List<Map<String, Object>> getMoviesOrderByCommentCount() {
        return movieMapper.selectMoviesOrderByCommentCount();
    }

    // ========== 分类管理 ==========

    public List<MovieCategory> getUserCategories(Integer userId) {
        return movieMapper.findCategoriesByUserId(userId);
    }

    @Transactional
    public Map<String, Object> createCategory(Integer userId, CategoryRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 检查分类名是否已存在
        List<MovieCategory> existing = movieMapper.findCategoriesByUserId(userId);
        if (existing.stream().anyMatch(c -> c.getCategoryName().equals(request.getCategoryName()))) {
            result.put("success", false);
            result.put("message", "分类名称已存在");
            return result;
        }

        MovieCategory category = new MovieCategory();
        category.setUserId(userId);
        category.setCategoryName(request.getCategoryName());
        movieMapper.insertCategory(category);

        result.put("success", true);
        result.put("message", "分类创建成功");
        result.put("categoryId", category.getCategoryId());
        return result;
    }

    @Transactional
    public Map<String, Object> deleteCategory(Integer userId, Integer categoryId) {
        Map<String, Object> result = new HashMap<>();

        // 验证分类是否属于该用户
        MovieCategory category = movieMapper.findCategoryById(categoryId);
        if (category == null || !category.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "分类不存在或无权限删除");
            return result;
        }

        // 将该分类下的所有收藏的category_id设置为NULL
        movieMapper.updateCollectionCategoryToNull(categoryId);

        // 删除分类
        movieMapper.deleteCategory(categoryId, userId);

        result.put("success", true);
        result.put("message", "分类删除成功");
        return result;
    }

    @Transactional
    public Map<String, Object> moveToCategory(Integer collectionId, Integer categoryId) {
        Map<String, Object> result = new HashMap<>();

        // 验证收藏是否存在
        MovieCollection collection = movieMapper.findCollectionById(collectionId);
        if (collection == null) {
            result.put("success", false);
            result.put("message", "收藏记录不存在");
            return result;
        }

        movieMapper.updateCollectionCategory(collectionId, categoryId);
        result.put("success", true);
        result.put("message", "移动成功");
        return result;
    }

    @Transactional
    public Map<String, Object> batchMoveToCategory(Integer userId, List<Integer> collectionIds, Integer categoryId) {
        Map<String, Object> result = new HashMap<>();

        if (collectionIds == null || collectionIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要移动的收藏");
            return result;
        }

        int updated = movieMapper.batchMoveToCategory(categoryId, collectionIds, userId);
        result.put("success", true);
        result.put("message", "成功移动 " + updated + " 部电影");
        return result;
    }

    public List<MovieCollection> getCollectionsByCategory(Integer userId, Integer categoryId) {
        return movieMapper.searchCollections(userId, null, null, null, null, null,
                categoryId, null, null, null, null);
    }

    // ========== 私人评价管理 ==========
    /**
     * 更新私人评价
     * @param userId 用户ID
     * @param request 私人评价请求（包含tmdbId和私人评价内容）
     * @return 操作结果
     */
    @Transactional
    public Map<String, Object> updatePrivateReview(Integer userId, PrivateReviewRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 1. 根据TMDB ID查找电影
        MoviePublic movie = movieMapper.findMovieByTmdbId(request.getTmdbId());
        if (movie == null) {
            result.put("success", false);
            result.put("message", "电影不存在");
            return result;
        }

        // 2. 查找用户的收藏记录
        MovieCollection collection = movieMapper.findCollectionByUserAndMovie(userId, movie.getMovieId());
        if (collection == null) {
            result.put("success", false);
            result.put("message", "请先收藏该电影");
            return result;
        }

        // 3. 更新私人评价
        collection.setPrivateReview(request.getPrivateReview());
        movieMapper.updateCollection(collection);

        result.put("success", true);
        result.put("message", "私人评价更新成功");
        return result;
    }

    @Transactional
    public Map<String, Object> updateCategory(Integer userId, Integer categoryId, CategoryRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 验证分类是否存在且属于当前用户
        MovieCategory category = movieMapper.findCategoryById(categoryId);
        if (category == null) {
            result.put("success", false);
            result.put("message", "分类不存在");
            return result;
        }
        if (!category.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "无权限修改此分类");
            return result;
        }

        // 检查新名称是否已存在
        List<MovieCategory> existing = movieMapper.findCategoriesByUserId(userId);
        if (existing.stream().anyMatch(c -> c.getCategoryName().equals(request.getCategoryName())
                && !c.getCategoryId().equals(categoryId))) {
            result.put("success", false);
            result.put("message", "分类名称已存在");
            return result;
        }

        // 更新分类名称
        int rows = movieMapper.updateCategoryName(categoryId, request.getCategoryName());
        if (rows > 0) {
            result.put("success", true);
            result.put("message", "分类修改成功");
        } else {
            result.put("success", false);
            result.put("message", "修改失败");
        }
        return result;
    }
}