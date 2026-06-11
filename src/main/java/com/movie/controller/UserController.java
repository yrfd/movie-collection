package com.movie.controller;

import com.movie.dto.*;
import com.movie.entity.User;
import com.movie.service.EmailService;
import com.movie.service.UserService;
import com.movie.service.VerificationCodeService;
import com.movie.util.JwtUtil;
import com.movie.util.VerificationCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationCodeService codeService;

    private Integer getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        return null;
    }

    // ========== 头像相关接口 ==========

    /**
     * 上传头像
     */
    @PostMapping("/avatar/upload")
    public ApiResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Map<String, Object> result = userService.uploadAvatar(userId, file);
        boolean success = (Boolean) result.get("success");
        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("avatarUrl", result.get("avatarUrl"));
            return ApiResponse.success((String) result.get("message"), data);
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    /**
     * 获取用户信息（包含头像）
     */
    @GetMapping("/profile")
    public ApiResponse<?> getUserProfile(HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Map<String, Object> result = userService.getUserProfile(userId);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success(result);
        }
        return ApiResponse.error(404, (String) result.get("message"));
    }

    // ========== 注册相关接口 ==========

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty() ||
                request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getCode() == null || request.getCode().isEmpty()) {
            return ApiResponse.error(400, "所有字段都不能为空");
        }

        String passwordRegex = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!request.getPassword().matches(passwordRegex)) {
            return ApiResponse.error(400, "密码只能包含数字、字母和符号，不能包含空格");
        }

        if (request.getPassword().length() < 6) {
            return ApiResponse.error(400, "密码长度至少6位");
        }

        if (!isValidEmail(request.getEmail())) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        if (!codeService.verifyCode(request.getEmail(), request.getCode())) {
            return ApiResponse.error(400, "验证码错误或已过期");
        }

        Map<String, Object> result = userService.register(request);
        boolean success = (Boolean) result.get("success");

        if (success) {
            codeService.deleteCode(request.getEmail());
            String message = (String) result.get("message");
            Object userId = result.get("userId");
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            return ApiResponse.success(message, data);
        }

        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PostMapping("/send-code")
    public ApiResponse<?> sendVerificationCode(@RequestParam String email) {
        if (!isValidEmail(email)) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        if (!codeService.canSendCode(email)) {
            return ApiResponse.error(429, "验证码已发送，请稍后再试");
        }

        String code = VerificationCodeUtil.generateCode();
        codeService.saveCode(email, code);
        codeService.markCodeSent(email);

        String subject = "银幕记忆 - 注册验证码";
        String content = "【银幕记忆】您的注册验证码是：" + code + "，请在5分钟内使用。如非本人操作，请忽略。";

        try {
            emailService.sendSimpleMail(email, subject, content);
            return ApiResponse.success("验证码已发送至 " + email);
        } catch (Exception e) {
            e.printStackTrace();
            codeService.deleteCode(email);
            return ApiResponse.error(500, "邮件发送失败，请稍后重试");
        }
    }

    @PostMapping("/send-reset-code")
    public ApiResponse<?> sendResetCode(@RequestParam String email) {
        if (!isValidEmail(email)) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        if (!userService.isEmailRegistered(email)) {
            return ApiResponse.error(400, "该邮箱未注册");
        }

        if (!codeService.canSendCode(email)) {
            return ApiResponse.error(429, "验证码已发送，请稍后再试");
        }

        String code = VerificationCodeUtil.generateCode();
        codeService.saveCode(email, code);
        codeService.markCodeSent(email);

        String subject = "银幕记忆 - 找回密码验证码";
        String content = "【银幕记忆】您正在找回密码，验证码是：" + code + "，请在5分钟内使用。如非本人操作，请忽略。";

        try {
            emailService.sendSimpleMail(email, subject, content);
            return ApiResponse.success("验证码已发送至 " + email);
        } catch (Exception e) {
            e.printStackTrace();
            codeService.deleteCode(email);
            String errorMsg = e.getMessage().contains("invalid address") ? "邮箱地址不存在，请检查邮箱是否正确" : "邮件发送失败";
            return ApiResponse.error(500, errorMsg);
        }
    }

    // ========== 登录接口（支持用户名/邮箱）==========

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request) {
        Map<String, Object> result = userService.login(request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            Object data = result.get("data");
            return ApiResponse.success(data);
        }
        return ApiResponse.error(401, (String) result.get("message"));
    }

    // ========== 密码重置接口 ==========

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getCode() == null || request.getCode().isEmpty() ||
                request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ApiResponse.error(400, "所有字段都不能为空");
        }

        String passwordRegex = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!request.getNewPassword().matches(passwordRegex)) {
            return ApiResponse.error(400, "密码只能包含数字、字母和符号，不能包含空格");
        }

        if (request.getNewPassword().length() < 6) {
            return ApiResponse.error(400, "密码长度至少6位");
        }

        Map<String, Object> result = userService.resetPassword(request);
        boolean success = (Boolean) result.get("success");

        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    // ========== 用户信息修改接口 ==========

    @PutMapping("/update")
    public ApiResponse<?> updateProfile(@RequestBody ProfileUpdateRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        Map<String, Object> result = userService.updateProfile(userId, request);
        boolean success = (Boolean) result.get("success");
        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PostMapping("/send-email-code")
    public ApiResponse<?> sendEmailCode(@RequestParam String newEmail, HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        if (!isValidEmail(newEmail)) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        if (userService.isEmailRegistered(newEmail)) {
            User existingUser = userService.getUserByEmail(newEmail);
            if (!existingUser.getUserId().equals(userId)) {
                return ApiResponse.error(400, "该邮箱已被其他用户注册");
            }
        }

        if (!codeService.canSendCode(newEmail)) {
            return ApiResponse.error(429, "验证码已发送，请稍后再试");
        }

        String code = VerificationCodeUtil.generateCode();
        codeService.saveCode(newEmail, code);
        codeService.markCodeSent(newEmail);

        String subject = "银幕记忆 - 修改邮箱验证码";
        String content = "您正在修改邮箱，验证码是：" + code + "，请在5分钟内使用。如非本人操作，请忽略。";

        try {
            emailService.sendSimpleMail(newEmail, subject, content);
            return ApiResponse.success("验证码已发送至 " + newEmail);
        } catch (Exception e) {
            e.printStackTrace();
            codeService.deleteCode(newEmail);
            return ApiResponse.error(500, "邮件发送失败，请稍后重试");
        }
    }

    @PutMapping("/update-email")
    public ApiResponse<?> updateEmail(@RequestBody UpdateEmailRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        Map<String, Object> result = userService.updateEmail(userId, request);
        boolean success = (Boolean) result.get("success");

        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    @PutMapping("/update-username")
    public ApiResponse<?> updateUsername(@RequestParam String newUsername, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        String usernameRegex = "^[a-zA-Z0-9_]{4,20}$";
        if (!newUsername.matches(usernameRegex)) {
            return ApiResponse.error(400, "用户名必须为4-20位字母、数字或下划线");
        }

        if (userService.isUsernameExists(newUsername)) {
            return ApiResponse.error(400, "用户名已存在");
        }

        boolean success = userService.updateUsername(userId, newUsername);
        if (success) {
            return ApiResponse.success("用户名修改成功");
        }
        return ApiResponse.error(400, "用户名修改失败");
    }

    @PutMapping("/update-password")
    public ApiResponse<?> updatePassword(@RequestBody PasswordUpdateRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        if (request.getOldPassword() == null || request.getOldPassword().isEmpty() ||
                request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ApiResponse.error(400, "原密码和新密码都不能为空");
        }

        if (request.getNewPassword().length() < 6) {
            return ApiResponse.error(400, "新密码长度至少6位");
        }

        String passwordRegex = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!request.getNewPassword().matches(passwordRegex)) {
            return ApiResponse.error(400, "新密码只能包含数字、字母和符号，不能包含空格");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            return ApiResponse.error(400, "新密码不能与旧密码相同");
        }

        Map<String, Object> result = userService.updatePassword(userId, request);
        boolean success = (Boolean) result.get("success");

        if (success) {
            return ApiResponse.success((String) result.get("message"));
        }
        return ApiResponse.error(400, (String) result.get("message"));
    }

    // ========== 工具方法 ==========

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(regex);
    }
}