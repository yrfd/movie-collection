package com.movie.service;

import com.movie.dto.CategoryRequest;
import com.movie.dto.MovieRequest;
import com.movie.entity.MovieCategory;
import com.movie.dto.PrivateReviewRequest;
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
public class MovieService {

    @Autowired
    private MovieMapper movieMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private CommentService commentService;

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
            movieMapper.insertMovie(movie);
        }

        // 检查是否已收藏
        MovieCollection existing = movieMapper.findCollectionByUserAndMovie(userId, movie.getMovieId());
        if (existing != null) {
            result.put("success", false);
            result.put("message", "该电影已在收藏列表中");
            return result;
        }

        // 添加收藏
        MovieCollection collection = new MovieCollection();
        collection.setUserId(userId);
        collection.setMovieId(movie.getMovieId());
        collection.setPersonalRating(request.getPersonalRating());
        collection.setWatchStatus(request.getWatchStatus());
        collection.setPrivateReview(request.getPrivateReview());
        collection.setCategoryId(request.getCategoryId()); // 设置分类

        movieMapper.insertCollection(collection);

        result.put("success", true);
        result.put("message", "添加成功");
        result.put("collectionId", collection.getCollectionId());
        return result;
    }

    // 更新电影收藏
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

        collection.setPersonalRating(request.getPersonalRating());
        collection.setWatchStatus(request.getWatchStatus());
        collection.setPrivateReview(request.getPrivateReview());

        movieMapper.updateCollection(collection);

        if (oldRating != null && newRating != null && !oldRating.equals(newRating)) {
            // 更新该用户对该电影的评论评分
            commentMapper.updateCommentRatingByUserAndMovie(
                    collection.getUserId(),
                    collection.getMovieId(),
                    newRating
            );
            // 重新计算电影综合评分
            commentService.updateMovieRating(collection.getMovieId());
        }
        result.put("success", true);
        result.put("message", "更新成功");
        return result;
    }

    // 删除电影收藏
    public Map<String, Object> deleteCollection(Integer collectionId) {
        Map<String, Object> result = new HashMap<>();
        movieMapper.deleteCollection(collectionId);
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

        movieMapper.batchMoveToCategory(categoryId, collectionIds, userId);
        result.put("success", true);
        result.put("message", "批量移动成功");
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
}
