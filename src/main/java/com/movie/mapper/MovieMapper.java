package com.movie.mapper;

import com.movie.entity.MovieCategory;
import com.movie.entity.MovieCollection;
import com.movie.entity.MoviePublic;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface MovieMapper {
    // ========== 公共电影操作 ==========
    // 删除注解，使用 XML
    MoviePublic findMovieByName(@Param("movieName") String movieName);
    int insertMovie(MoviePublic movie);
    int updateMovieRating(@Param("movieId") Integer movieId,
                          @Param("avgRating") Double avgRating,
                          @Param("ratingCount") Integer ratingCount);

    // ========== 个人收藏操作 ==========
    // 删除 @Select 注解，使用 XML
    List<MovieCollection> findCollectionsByUserId(@Param("userId") Integer userId);
    MovieCollection findCollectionById(@Param("collectionId") Integer collectionId);
    MovieCollection findCollectionByUserAndMovie(@Param("userId") Integer userId,
                                                 @Param("movieId") Integer movieId);
    int insertCollection(MovieCollection collection);
    int updateCollection(MovieCollection collection);
    int deleteCollection(@Param("collectionId") Integer collectionId);

    // ========== 搜索筛选 ==========
    List<MovieCollection> searchCollections(@Param("userId") Integer userId,
                                            @Param("keyword") String keyword,
                                            @Param("director") String director,
                                            @Param("minRating") Double minRating,
                                            @Param("region") String region,
                                            @Param("genre") String genre,
                                            @Param("categoryId") Integer categoryId,
                                            @Param("watchStatus") String watchStatus,
                                            @Param("minYear") Integer minYear,
                                            @Param("maxYear") Integer maxYear,
                                            @Param("sortBy") String sortBy);


    MoviePublic findMovieByTmdbId(@Param("tmdbId") Integer tmdbId);

    // ========== 排行榜 ==========
    List<Map<String, Object>> selectMoviesOrderByRating();
    List<Map<String, Object>> selectMoviesOrderByCommentCount();

    // ========== 分类管理 ==========
    // 这些可以用注解，因为 XML 中没有定义
    @Select("SELECT * FROM movie_category WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<MovieCategory> findCategoriesByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM movie_category WHERE category_id = #{categoryId}")
    MovieCategory findCategoryById(@Param("categoryId") Integer categoryId);

    @Insert("INSERT INTO movie_category (user_id, category_name) VALUES (#{userId}, #{categoryName})")
    @Options(useGeneratedKeys = true, keyProperty = "categoryId")
    int insertCategory(MovieCategory category);

    @Delete("DELETE FROM movie_category WHERE category_id = #{categoryId} AND user_id = #{userId}")
    int deleteCategory(@Param("categoryId") Integer categoryId, @Param("userId") Integer userId);

    @Update("UPDATE movie_collection SET category_id = #{categoryId} WHERE collection_id = #{collectionId}")
    int updateCollectionCategory(@Param("collectionId") Integer collectionId,
                                 @Param("categoryId") Integer categoryId);

    @Update("UPDATE movie_collection SET category_id = NULL WHERE category_id = #{categoryId}")
    int updateCollectionCategoryToNull(@Param("categoryId") Integer categoryId);

    int batchMoveToCategory(@Param("categoryId") Integer categoryId,
                            @Param("collectionIds") List<Integer> collectionIds,
                            @Param("userId") Integer userId);
    MoviePublic findMovieById(@Param("movieId") Integer movieId);

    Double getAveragePersonalRatingByMovie(@Param("movieId") Integer movieId);
    Integer getRatingCountFromCollections(@Param("movieId") Integer movieId);

}