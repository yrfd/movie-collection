package com.movie.service;

import com.movie.dto.*;
import com.movie.entity.MovieCategory;
import com.movie.entity.User;
import com.movie.mapper.MovieMapper;
import com.movie.mapper.UserMapper;
import com.movie.util.JwtUtil;
import com.movie.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MovieMapper movieMapper;  // 新增：注入 MovieMapper 用于创建分类

    @Autowired
    private VerificationCodeService codeService;

    /**
     * 用户注册 - 同时创建默认分类
     */
    @Transactional  // 添加事务注解，确保用户创建和分类创建要么都成功，要么都失败
    public Map<String, Object> register(RegisterRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 检查用户名是否已存在
        if (userMapper.findByUsername(request.getUsername()) != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        // 检查邮箱是否已被注册
        User existingUserByEmail = userMapper.findByEmail(request.getEmail());
        if (userMapper.findByEmail(request.getEmail()) != null) {
            result.put("success", false);
            result.put("message", "该邮箱已被注册，用户名为：" + existingUserByEmail.getUsername() + "，\n请使用其他邮箱注册或者使用该账号进行登录");
            result.put("existingUsername", existingUserByEmail.getUsername());
            return result;
        }

        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(MD5Util.md5(request.getPassword()));
        user.setEmail(request.getEmail());

        userMapper.insert(user);

        // ========== 新增：为用户创建默认分类 ==========
        createDefaultCategories(user.getUserId());

        result.put("success", true);
        result.put("message", "注册成功");
        result.put("userId", user.getUserId());
        return result;
    }

    /**
     * 为用户创建默认分类
     * @param userId 用户ID
     */
    private void createDefaultCategories(Integer userId) {
        // 方式一：创建一个默认分类
        MovieCategory defaultCategory = new MovieCategory();
        defaultCategory.setUserId(userId);
        defaultCategory.setCategoryName("默认收藏");
        movieMapper.insertCategory(defaultCategory);

        // 方式二：创建多个默认分类（可选，根据需要决定是否启用）
        // 如果想要多个默认分类，取消下面的注释
        /*
        String[] defaultCategories = {"想看清单", "已看过", "我的最爱"};
        for (String categoryName : defaultCategories) {
            MovieCategory category = new MovieCategory();
            category.setUserId(userId);
            category.setCategoryName(categoryName);
            movieMapper.insertCategory(category);
        }
        */
    }

    // 其他方法保持不变...
    public Map<String, Object> login(LoginRequest request) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户名不存在");
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

    public Map<String, Object> updateProfile(Integer userId, ProfileUpdateRequest request) {
        Map<String, Object> result = new HashMap<>();

        User user = userMapper.findByUserId(userId);
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        // 更新邮箱
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // 检查邮箱是否已被其他用户使用
            User existingUser = userMapper.findByEmail(request.getEmail());
            if (existingUser != null && !existingUser.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "邮箱已被其他用户使用");
                return result;
            }
            user.setEmail(request.getEmail());
        }

        // 更新密码
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

    /**
     * 通过邮箱更新密码（找回密码用）
     */
    public boolean updatePasswordByEmail(String email, String newPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return false;
        }
        user.setPassword(MD5Util.md5(newPassword));
        int result = userMapper.updatePassword(user.getUserId(), user.getPassword());
        return result > 0;
    }

    /**
     * 重置密码（带验证码验证）
     */
    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 1. 验证邮箱是否存在
        if (!isEmailRegistered(request.getEmail())) {
            result.put("success", false);
            result.put("message", "该邮箱未注册");
            return result;
        }

        // 2. 验证验证码
        if (!codeService.verifyCode(request.getEmail(), request.getCode())) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        // 3. 更新密码
        boolean success = updatePasswordByEmail(request.getEmail(), request.getNewPassword());

        if (success) {
            // 验证码使用后立即删除
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

        // 1. 验证新邮箱格式
        if (!isValidEmail(request.getNewEmail())) {
            result.put("success", false);
            result.put("message", "邮箱格式不正确");
            return result;
        }

        // 2. 检查新邮箱是否已被其他用户注册
        User existingUser = userMapper.findByEmail(request.getNewEmail());
        if (existingUser != null && !existingUser.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "该邮箱已被其他用户注册");
            return result;
        }

        // 3. 验证验证码
        if (!codeService.verifyCode(request.getNewEmail(), request.getCode())) {
            result.put("success", false);
            result.put("message", "验证码错误或已过期");
            return result;
        }

        // 4. 更新邮箱
        int updated = userMapper.updateEmail(userId, request.getNewEmail());
        if (updated > 0) {
            // 验证码使用后删除
            codeService.deleteCode(request.getNewEmail());
            result.put("success", true);
            result.put("message", "邮箱修改成功");
        } else {
            result.put("success", false);
            result.put("message", "邮箱修改失败");
        }

        return result;
    }

    /**
     * 验证邮箱格式
     */
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

        // 验证旧密码
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

        // 更新密码
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