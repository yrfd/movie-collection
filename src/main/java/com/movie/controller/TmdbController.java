package com.movie.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/tmdb")
public class TmdbController {

    private static final String TMDB_API_KEY = "b8bd41516966d743a7cbcb3d81dc02c2";
    private static final String TMDB_BASE_URL = "https://api.tmdb.org/3";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/movie/popular", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> getPopularMovies(@RequestParam(defaultValue = "1") int page) {
        String url = TMDB_BASE_URL + "/movie/popular?api_key=" + TMDB_API_KEY + "&language=zh-CN&page=" + page;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            String fullJson = new String(response.getBody(), StandardCharsets.UTF_8);

            // 解析并精简数据
            Map<String, Object> originalData = objectMapper.readValue(fullJson, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) originalData.get("results");

            // 精简每部电影的数据，只保留必要字段，不包含评分
            List<Map<String, Object>> simplifiedResults = new ArrayList<>();
            for (Map<String, Object> movie : results) {
                Map<String, Object> simplified = new HashMap<>();
                simplified.put("id", movie.get("id"));
                simplified.put("title", movie.get("title"));
                simplified.put("poster_path", movie.get("poster_path"));
                simplified.put("release_date", movie.get("release_date"));
                simplifiedResults.add(simplified);
            }

            Map<String, Object> simplifiedData = new HashMap<>();
            simplifiedData.put("page", originalData.get("page"));
            simplifiedData.put("total_pages", originalData.get("total_pages"));
            simplifiedData.put("total_results", originalData.get("total_results"));
            simplifiedData.put("results", simplifiedResults);

            String simplifiedJson = objectMapper.writeValueAsString(simplifiedData);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(simplifiedJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/search/movie", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> searchMovie(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String withGenres) {

        StringBuilder url = new StringBuilder(TMDB_BASE_URL + "/search/movie?api_key=" + TMDB_API_KEY + "&language=zh-CN&page=" + page);

        if (query != null && !query.isEmpty()) {
            url.append("&query=").append(query);
        }
        if (year != null) {
            url.append("&year=").append(year);
        }
        if (withGenres != null && !withGenres.isEmpty()) {
            url.append("&with_genres=").append(withGenres);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url.toString(), HttpMethod.GET, entity, byte[].class);
            String fullJson = new String(response.getBody(), StandardCharsets.UTF_8);

            // ✅ 新增：解析并精简搜索结果，去除评分字段
            Map<String, Object> originalData = objectMapper.readValue(fullJson, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) originalData.get("results");

            // 精简每部电影的数据，只保留必要字段
            List<Map<String, Object>> simplifiedResults = new ArrayList<>();
            if (results != null) {
                for (Map<String, Object> movie : results) {
                    Map<String, Object> simplified = new HashMap<>();
                    simplified.put("id", movie.get("id"));
                    simplified.put("title", movie.get("title"));
                    simplified.put("poster_path", movie.get("poster_path"));
                    simplified.put("release_date", movie.get("release_date"));
                    // 不包含 vote_average 和 vote_count
                    simplifiedResults.add(simplified);
                }
            }

            Map<String, Object> simplifiedData = new HashMap<>();
            simplifiedData.put("page", originalData.get("page"));
            simplifiedData.put("total_pages", originalData.get("total_pages"));
            simplifiedData.put("total_results", originalData.get("total_results"));
            simplifiedData.put("results", simplifiedResults);

            String simplifiedJson = objectMapper.writeValueAsString(simplifiedData);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(simplifiedJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/discover/movie", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> discoverMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "primary_release_year", required = false) Integer year,
            @RequestParam(name = "with_genres", required = false) String withGenres) {

        StringBuilder url = new StringBuilder(TMDB_BASE_URL + "/discover/movie?api_key=" + TMDB_API_KEY + "&language=zh-CN&page=" + page);
        if (year != null) {
            url.append("&primary_release_year=").append(year);
        }
        if (withGenres != null && !withGenres.isEmpty()) {
            url.append("&with_genres=").append(withGenres);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url.toString(), HttpMethod.GET, entity, byte[].class);
            String fullJson = new String(response.getBody(), StandardCharsets.UTF_8);

            // ✅ 新增：解析并精简发现电影数据，去除评分字段
            Map<String, Object> originalData = objectMapper.readValue(fullJson, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) originalData.get("results");

            // 精简每部电影的数据，只保留必要字段
            List<Map<String, Object>> simplifiedResults = new ArrayList<>();
            if (results != null) {
                for (Map<String, Object> movie : results) {
                    Map<String, Object> simplified = new HashMap<>();
                    simplified.put("id", movie.get("id"));
                    simplified.put("title", movie.get("title"));
                    simplified.put("poster_path", movie.get("poster_path"));
                    simplified.put("release_date", movie.get("release_date"));
                    // 不包含 vote_average 和 vote_count
                    simplifiedResults.add(simplified);
                }
            }

            Map<String, Object> simplifiedData = new HashMap<>();
            simplifiedData.put("page", originalData.get("page"));
            simplifiedData.put("total_pages", originalData.get("total_pages"));
            simplifiedData.put("total_results", originalData.get("total_results"));
            simplifiedData.put("results", simplifiedResults);

            String simplifiedJson = objectMapper.writeValueAsString(simplifiedData);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(simplifiedJson);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/movie/{movieId}", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> getMovieDetails(@PathVariable int movieId) {
        // 构建获取详情的URL，使用 /movie/{movie_id} 端点 [citation:6]
        String url = TMDB_BASE_URL + "/movie/" + movieId + "?api_key=" + TMDB_API_KEY + "&language=zh-CN&append_to_response=credits";

        System.out.println("详情URL: " + url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String json = response.getBody();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/movie/{movieId}/full", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> getMovieFullInfo(@PathVariable int movieId) {
        // 获取电影详情（包含 credits）
        String url = TMDB_BASE_URL + "/movie/" + movieId + "?api_key=" + TMDB_API_KEY + "&language=zh-CN&append_to_response=credits";
        System.out.println("获取完整电影信息URL: " + url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String json = response.getBody();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/search/movie-by-person", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> searchMovieByPerson(
            @RequestParam String personName,
            @RequestParam(defaultValue = "1") int page) {

        try {
            // 1. 搜索人员，取第一个（按 popularity 降序）
            String searchPersonUrl = TMDB_BASE_URL + "/search/person?api_key=" + TMDB_API_KEY
                    + "&language=zh-CN&query=" + URLEncoder.encode(personName, StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json;charset=UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> personResponse = restTemplate.exchange(searchPersonUrl, HttpMethod.GET, entity, byte[].class);
            String personJson = new String(personResponse.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> personData = objectMapper.readValue(personJson, Map.class);
            List<Map<String, Object>> personResults = (List<Map<String, Object>>) personData.get("results");
            if (personResults == null || personResults.isEmpty()) {
                // 尝试英文名（简单处理：将中文转拼音？这里直接返回空）
                return ResponseEntity.ok("{\"results\":[],\"total_pages\":0,\"total_results\":0,\"page\":1}");
            }
            // 按 popularity 排序取第一个
            personResults.sort((a, b) -> Double.compare(
                    ((Number) b.getOrDefault("popularity", 0.0)).doubleValue(),
                    ((Number) a.getOrDefault("popularity", 0.0)).doubleValue()
            ));
            int personId = (int) personResults.get(0).get("id");

            // 2. 使用 with_people 参数查询电影（支持分页）
            String discoverUrl = TMDB_BASE_URL + "/discover/movie?api_key=" + TMDB_API_KEY
                    + "&language=zh-CN&page=" + page
                    + "&with_people=" + personId;
            ResponseEntity<byte[]> movieResponse = restTemplate.exchange(discoverUrl, HttpMethod.GET, entity, byte[].class);
            String fullJson = new String(movieResponse.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> originalData = objectMapper.readValue(fullJson, Map.class);

            // 3. 精简数据（与其它搜索接口保持一致）
            List<Map<String, Object>> results = (List<Map<String, Object>>) originalData.get("results");
            List<Map<String, Object>> simplifiedResults = new ArrayList<>();
            if (results != null) {
                for (Map<String, Object> movie : results) {
                    Map<String, Object> simplified = new HashMap<>();
                    simplified.put("id", movie.get("id"));
                    simplified.put("title", movie.get("title"));
                    simplified.put("poster_path", movie.get("poster_path"));
                    simplified.put("release_date", movie.get("release_date"));
                    simplifiedResults.add(simplified);
                }
            }

            Map<String, Object> simplifiedData = new HashMap<>();
            simplifiedData.put("page", originalData.get("page"));
            simplifiedData.put("total_pages", originalData.get("total_pages"));
            simplifiedData.put("total_results", originalData.get("total_results"));
            simplifiedData.put("results", simplifiedResults);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(simplifiedData));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}