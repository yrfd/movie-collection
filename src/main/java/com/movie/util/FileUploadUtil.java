package com.movie.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUploadUtil {

    private static final String UPLOAD_DIR = "uploads/avatars/";

    /**
     * 上传头像
     * @param file 上传的文件
     * @param userId 用户ID
     * @return 访问URL，失败返回null
     */
    public static String uploadAvatar(MultipartFile file, Integer userId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return null;
        }

        // 检查文件大小（2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            return null;
        }

        try {
            // 创建目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            return "/uploads/avatars/" + newFilename;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除旧头像
     */
    public static void deleteOldAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }

        try {
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}