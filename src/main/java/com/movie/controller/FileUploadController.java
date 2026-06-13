// FileUploadController.java
package com.movie.controller;

import com.movie.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    // 使用 user.dir 获取工作目录
    private static final String UPLOAD_BASE_PATH = System.getProperty("user.dir") + File.separator + "uploads";
    private static final String UPLOAD_URL = "/uploads/";

    /**
     * 上传图片
     */
    @PostMapping("/image")
    public ApiResponse<?> uploadImage(@RequestParam("file") MultipartFile file,
                                      HttpServletRequest request) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "请选择要上传的文件");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(400, "只能上传图片文件");
        }

        // 检查文件大小（限制5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return ApiResponse.error(400, "图片大小不能超过5MB");
        }

        try {
            // 按日期分目录存储
            String dateDir = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".jpg";

            // 构建完整路径
            Path uploadDir = Paths.get(UPLOAD_BASE_PATH, dateDir);

            // 确保目录存在
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                System.out.println("创建上传目录: " + uploadDir.toString());
            }

            Path filePath = uploadDir.resolve(fileName);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 返回访问URL
            String fileUrl = UPLOAD_URL + dateDir + "/" + fileName;

            System.out.println("文件保存成功: " + filePath.toString());
            System.out.println("访问URL: " + fileUrl);

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", fileName);

            return ApiResponse.success("上传成功", result);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.error(500, "上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/image")
    public ApiResponse<?> deleteImage(@RequestParam String url) {
        try {
            // 从URL中提取相对路径
            String relativePath = url.replace(UPLOAD_URL, "");
            Path filePath = Paths.get(UPLOAD_BASE_PATH, relativePath);
            File file = filePath.toFile();
            if (file.exists()) {
                file.delete();
                System.out.println("删除文件: " + filePath.toString());
            }
            return ApiResponse.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "删除失败：" + e.getMessage());
        }
    }
}