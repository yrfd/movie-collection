package com.movie.service;

import com.movie.dto.MyReviewDTO;
import com.movie.entity.Comment;
import com.movie.entity.MovieCollection;
import com.movie.mapper.CommentMapper;
import com.movie.mapper.MovieMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private CommentMapper commentMapper;

    /**
     * 获取用户的所有评价（公开+私人）
     * @param userId 用户ID
     * @param type 筛选类型：all, public, private, both
     */
    public List<MyReviewDTO> getAllMyReviews(Integer userId, String type) {
        List<MyReviewDTO> reviews = new ArrayList<>();

        // 获取用户的收藏（包含私人评价）
        List<MovieCollection> collections = movieMapper.findCollectionsByUserId(userId);

        // 获取用户的公开评价
        List<Comment> publicComments = commentMapper.findCommentsByUserId(userId);
        Map<Integer, Comment> publicCommentMap = publicComments.stream()
                .collect(Collectors.toMap(Comment::getMovieId, c -> c, (a, b) -> a));

        for (MovieCollection collection : collections) {
            MyReviewDTO dto = new MyReviewDTO();
            dto.setCollectionId(collection.getCollectionId());
            dto.setMovieId(collection.getMovieId());
            dto.setMovieName(collection.getMovieName());
            dto.setPosterUrl(collection.getPosterUrl());
            dto.setPersonalRating(collection.getPersonalRating());
            dto.setWatchStatus(collection.getWatchStatus());
            dto.setPrivateReview(collection.getPrivateReview());
            dto.setReviewTime(collection.getUpdateTime());

            // 获取公开评价
            Comment publicComment = publicCommentMap.get(collection.getMovieId());
            if (publicComment != null) {
                dto.setPublicReview(publicComment.getContent());
                dto.setPublicRating(publicComment.getRating());
                dto.setCommentId(publicComment.getCommentId());
            }

            // 判断评价类型
            boolean hasPrivate = dto.getPrivateReview() != null && !dto.getPrivateReview().isEmpty();
            boolean hasPublic = dto.getPublicReview() != null && !dto.getPublicReview().isEmpty();

            if (hasPrivate && hasPublic) {
                dto.setReviewType("BOTH");
            } else if (hasPrivate) {
                dto.setReviewType("PRIVATE");
            } else if (hasPublic) {
                dto.setReviewType("PUBLIC");
            } else {
                dto.setReviewType("NONE");
            }

            // 根据类型筛选
            if ("private".equals(type) && !hasPrivate) continue;
            if ("public".equals(type) && !hasPublic) continue;
            if ("both".equals(type) && !(hasPrivate && hasPublic)) continue;

            reviews.add(dto);
        }

        // 按更新时间倒序
        reviews.sort((a, b) -> {
            if (a.getReviewTime() == null && b.getReviewTime() == null) return 0;
            if (a.getReviewTime() == null) return 1;
            if (b.getReviewTime() == null) return -1;
            return b.getReviewTime().compareTo(a.getReviewTime());
        });

        return reviews;
    }

    /**
     * 获取用户的私人评价（从收藏表中）
     */
    public List<MovieCollection> getPrivateReviews(Integer userId) {
        List<MovieCollection> collections = movieMapper.findCollectionsByUserId(userId);
        return collections.stream()
                .filter(c -> c.getPrivateReview() != null && !c.getPrivateReview().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 获取评价统计
     */
    public Map<String, Object> getReviewStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();

        // 私人评价统计
        List<MovieCollection> collections = movieMapper.findCollectionsByUserId(userId);
        long privateCount = collections.stream()
                .filter(c -> c.getPrivateReview() != null && !c.getPrivateReview().isEmpty())
                .count();

        // 公开评价统计
        List<Comment> comments = commentMapper.findCommentsByUserId(userId);
        long publicCount = comments.size();

        // 同时有公开和私人评价的数量
        Set<Integer> moviesWithPrivate = collections.stream()
                .filter(c -> c.getPrivateReview() != null && !c.getPrivateReview().isEmpty())
                .map(MovieCollection::getMovieId)
                .collect(Collectors.toSet());

        long bothCount = comments.stream()
                .filter(c -> moviesWithPrivate.contains(c.getMovieId()))
                .count();

        stats.put("totalPrivateCount", privateCount);
        stats.put("totalPublicCount", publicCount);
        stats.put("bothCount", bothCount);
        stats.put("onlyPrivateCount", privateCount - bothCount);
        stats.put("onlyPublicCount", publicCount - bothCount);

        return stats;
    }
}