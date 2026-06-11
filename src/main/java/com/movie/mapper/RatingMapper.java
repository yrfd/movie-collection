package com.movie.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RatingMapper {

    // 保存或更新用户评分
    int insertOrUpdateRating(@Param("userId") Integer userId,
                             @Param("movieId") Integer movieId,
                             @Param("rating") Double rating);

    // 获取电影平均分
    Double getAverageRatingByMovie(@Param("movieId") Integer movieId);

    // 获取电影评分人数
    Integer getRatingCountByMovie(@Param("movieId") Integer movieId);

    // 删除用户评分
    int deleteRating(@Param("userId") Integer userId, @Param("movieId") Integer movieId);
}