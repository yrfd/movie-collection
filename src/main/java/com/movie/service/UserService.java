package com.movie.service;

import com.movie.dto.*;
import com.movie.entity.MovieCategory;
import com.movie.entity.User;
import com.movie.mapper.MovieMapper;
import com.movie.mapper.UserMapper;
import com.movie.util.FileUploadUtil;
import com.movie.util.JwtUtil;
import com.movie.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MovieMapper movieMapper;

    @Autowired
    private VerificationCodeService codeService;

    /**
     * 用户注册 - 同时创建默认分类
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (userMapper.findByUsername(request.getUsername()) != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        User existingUserByEmail = userMapper.findByEmail(request.getEmail());
        if (existingUserByEmail != null) {
            result.put("success", false);
            result.put("message", "该邮箱已被注册，用户名为：" + existingUserByEmail.getUsername());
            result.put("existingUsername", existingUserByEmail.getUsername());
            return result;
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(MD5Util.md5(request.getPassword()));
        user.setEmail(request.getEmail());

        userMapper.insert(user);

        createDefaultCategories(user.getUserId());

        result.put("success", true);
        result.put("message", "注册成功");
        result.put("userId", user.getUserId());
        return result;
    }

    private void createDefaultCategories(Integer userId) {
        MovieCategory defaultCategory = new MovieCategory();
        defaultCategory.setUserId(userId);
        defaultCategory.setCategoryName("默认收藏");
        movieMapper.insertCategory(defaultCategory);
    }

    /**
     * 登录 - 支持用户名或邮箱
     */
    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> result = new HashMap<>();

        // ✅ 使用用户名或邮箱查找
        User user = userMapper.findByUsernameOrEmail(request.getUsername());

        if (user == null) {
            result.put("success", false);
            result.put("message", "用户名/邮箱不存在");
            return result;
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            result.put("success", false);
            result.put("message", "账号已被禁用，请联系管理员");
            return result;
        }

        if (!user.getPassword().equals(MD5Util.md5(request.getPassword()))) {
            result.put("success", false);
            result.put("message", "密码错误");
            return result;
        }

        String token = JwtUtil.generateToken(user.getUserId(), user.getUsername());

        Map<String, Object> userInfo = Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : "/images/default-avatar.png",
                "createTime", user.getCreateTime()
        );

        Map<String, Object> data = Map.of(
                "token", token,
                "user", userInfo
        );

        result.put("success", true);
        result.put("message", "登录成功");
        result.put("data", data);
        return result;
    }

    public User getUserById(Integer userId) {
        return userMapper.findByUserId(userId);
    }

    /**
     * 获取用户信息（包含头像）
     */
    public Map<String, Object> getUserProfile(Integer userId) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findByUserId(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        result.put("success", true);
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("avatar", user.getAvatar() != null ? user.getAvatar() : "/images/default-avatar.png");
        result.put("createTime", user.getCreateTime());

        return result;
    }

    /**
     * 上传头像
     */
    @Transactional
    public Map<String, Object> uploadAvatar(Integer userId, MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        String avatarUrl = FileUploadUtil.uploadAvatar(file, userId);
        if (avatarUrl == null) {
            result.put("success", false);
            result.put("message", "头像上传失败，请检查文件格式（支持图片，大小不超过2MB）");
            return result;
        }

        User user = userMapper.findByUserId(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            FileUploadUtil.deleteOldAvatar(user.getAvatar());
        }

        userMapper.updateAvatar(userId, avatarUrl);

        result.put("success", true);
        result.put("message", "头像上传成功");
        result.put("avatarUrl", avatarUrl);
        return result;
    }

    public Map<String, Object> updateProfile(Integer userId, ProfileUpdateRequest request) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findByUserId(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            User existingUser = userMapper.findByEmail(request.getEmail());
            if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "邮箱已被其他用户使用");
                return result;
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(MD5Util.md5(request.getPassword()));
        }

        userMapper.updateUser(user);

        result.put("success", true);
        result.put("message", "资料更新成功");
        return result;
    }

    public boolean isEmailRegistered(String email) {
        return userMapper.findByEmail(email) != null;
    }

    public boolean updatePasswordByEmail(String email, String newPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return false;
        }
        user.setPassword(MD5Util.md5(newPassword));
        int result = userMapper.updatePassword(user.getUserId(), user.getPassword());
        return result > 0;
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (!isEmailRegistered(request.getEmail())) {
            result.put("success", false);
            result.put("message", "该邮箱未注册");
            return result;
        }

        if (!codeService.verifyCode(request.getEmail(), request.getCode())) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        boolean success = updatePasswordByEmail(request.getEmail(), request.getNewPassword());

        if (success) {
            codeService.deleteCode(request.getEmail());
            result.put("success", true);
            result.put("message", "密码重置成功");
        } else {
            result.put("success", false);
            result.put("message", "密码重置失败，请稍后重试");
        }

        return result;
    }

    public Map<String, Object> updateEmail(Integer userId, UpdateEmailRequest request) {
        Map<String, Object> result = new HashMap<>();

        if (!isValidEmail(request.getNewEmail())) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return result;
        }

        User existingUser = userMapper.findByEmail(request.getNewEmail());
        if (existingUser != null && !existingUser.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "该邮箱已被其他用户注册");
            return result;
        }

        if (!codeService.verifyCode(request.getNewEmail(), request.getCode())) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        int updated = userMapper.updateEmail(userId, request.getNewEmail());
        if (updated > 0) {
            codeService.deleteCode(request.getNewEmail());
            result.put("success", true);
            result.put("message", "邮箱修改成功");
        } else {
            result.put("success", false);
            result.put("message", "邮箱修改失败");
        }

        return result;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(regex);
    }

    public boolean isUsernameExists(String username) {
        return userMapper.findByUsername(username) != null;
    }

    public boolean updateUsername(Integer userId, String newUsername) {
        return userMapper.updateUsername(userId, newUsername) > 0;
    }

    public User getUserByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    public Map<String, Object> updatePassword(Integer userId, PasswordUpdateRequest request) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findByUserId(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        if (!user.getPassword().equals(MD5Util.md5(request.getOldPassword()))) {
            result.put("success", false);
            result.put("message", "当前密码错误");
            return result;
        }

        String newPasswordMd5 = MD5Util.md5(request.getNewPassword());
        if (user.getPassword().equals(newPasswordMd5)) {
            result.put("success", false);
            result.put("message", "新密码不能与旧密码相同");
            return result;
        }

        int updated = userMapper.updatePassword(userId, MD5Util.md5(request.getNewPassword()));
        if (updated > 0) {
            result.put("success", true);
            result.put("message", "密码修改成功");
        } else {
            result.put("success", false);
            result.put("message", "密码修改失败");
        }

        return result;
    }
}