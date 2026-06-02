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
    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest request) {
        // 1. 非空校验
        if (request.getUsername() == null || request.getUsername().isEmpty() ||
                request.getPassword() == null || request.getPassword().isEmpty() ||
                request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getCode() == null || request.getCode().isEmpty()) {
            return ApiResponse.error(400, "所有字段都不能为空");
        }

        // 2. 密码格式校验：不允许空格，只允许数字、字母、符号
        String passwordRegex = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!request.getPassword().matches(passwordRegex)) {
            return ApiResponse.error(400, "密码只能包含数字、字母和符号，不能包含空格");
        }

        // 3. 密码长度校验
        if (request.getPassword().length() < 6) {
            return ApiResponse.error(400, "密码长度至少6位");
        }

        // 4. 验证邮箱格式
        if (!isValidEmail(request.getEmail())) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        // 5. 验证验证码
        if (!codeService.verifyCode(request.getEmail(), request.getCode())) {
            return ApiResponse.error(400, "验证码错误或已过期");
        }

        // 6. 调用原有注册逻辑
        Map<String, Object> result = userService.register(request);
        boolean success = (Boolean) result.get("success");

        if (success) {
            // 注册成功后删除验证码
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
        // 1. 验证邮箱格式
        if (!isValidEmail(email)) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }


        // 3. 防止重复发送（60秒内只能发送一次）
        if (!codeService.canSendCode(email)) {
            return ApiResponse.error(429, "验证码已发送，请稍后再试");
        }

        // 4. 生成验证码
        String code = VerificationCodeUtil.generateCode();

        // 5. 保存验证码到Redis
        codeService.saveCode(email, code);
        codeService.markCodeSent(email);

        // 6. 发送邮件
        String subject = "银幕记忆 - 注册验证码";
        String content = "【银幕记忆】您的注册验证码是：" + code + "，请在5分钟内使用。如非本人操作，请忽略。";

        try {
            emailService.sendSimpleMail(email, subject, content);
            return ApiResponse.success("验证码已发送至 " + email);
        } catch (Exception e) {
            e.printStackTrace();
            // 发送失败时删除已保存的验证码
            codeService.deleteCode(email);
            return ApiResponse.error(500, "邮件发送失败，请稍后重试");
        }
    }

    @PostMapping("/send-reset-code")
    public ApiResponse<?> sendResetCode(@RequestParam String email) {
        // 1. 验证邮箱格式
        if (!isValidEmail(email)) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        // 2. 检查邮箱是否存在
        if (!userService.isEmailRegistered(email)) {
            return ApiResponse.error(400, "该邮箱未注册");
        }

        // 3. 防止重复发送（60秒内只能发送一次）
        if (!codeService.canSendCode(email)) {
            return ApiResponse.error(429, "验证码已发送，请稍后再试");
        }

        // 4. 生成验证码
        String code = VerificationCodeUtil.generateCode();

        // 5. 保存验证码到Redis
        codeService.saveCode(email, code);
        codeService.markCodeSent(email);

        // 6. 发送邮件
        String subject = "银幕记忆 - 找回密码验证码";
        String content = "【银幕记忆】您正在找回密码，验证码是：" + code + "，请在5分钟内使用。如非本人操作，请忽略。";

        try {
            emailService.sendSimpleMail(email, subject, content);
            return ApiResponse.success("验证码已发送至 " + email);
        } catch (Exception e) {
            e.printStackTrace();
            codeService.deleteCode(email);

            // 判断是否是邮箱不存在导致的发送失败
            String errorMsg = "邮件发送失败";
            if (e.getMessage().contains("invalid address") ||
                    e.getMessage().contains("Recipient address rejected") ||
                    e.getMessage().contains("550")) {
                errorMsg = "邮箱地址不存在，请检查邮箱是否正确";
            }
            return ApiResponse.error(500, errorMsg);
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        // 非空校验
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getCode() == null || request.getCode().isEmpty() ||
                request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ApiResponse.error(400, "所有字段都不能为空");
        }

        //密码格式校验
        String passwordRegex = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!request.getNewPassword().matches(passwordRegex)) {
            return ApiResponse.error(400, "密码只能包含数字、字母和符号，不能包含空格");
        }

        // 密码长度校验
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

    /**
     * 验证邮箱格式
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(regex);
    }


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

    /**
     * 发送修改邮箱的验证码
     */
    @PostMapping("/send-email-code")
    public ApiResponse<?> sendEmailCode(@RequestParam String newEmail, HttpServletRequest request) {
        Integer userId = getUserIdFromToken(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 1. 验证邮箱格式
        if (!isValidEmail(newEmail)) {
            return ApiResponse.error(400, "邮箱格式不正确");
        }

        // 2. 检查新邮箱是否已被其他用户注册
        if (userService.isEmailRegistered(newEmail)) {
            User existingUser = userService.getUserByEmail(newEmail);
            if (!existingUser.getUserId().equals(userId)) {
                return ApiResponse.error(400, "该邮箱已被其他用户注册");
            }
        }

        // 3. 防止重复发送
        if (!codeService.canSendCode(newEmail)) {
            return ApiResponse.error(429, "验证码已发送，请稍后再试");
        }

        // 4. 生成验证码
        String code = VerificationCodeUtil.generateCode();

        // 5. 保存验证码
        codeService.saveCode(newEmail, code);
        codeService.markCodeSent(newEmail);

        // 6. 发送邮件
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

    /**
     * 修改用户邮箱
     */
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

    /**
     * 修改用户名
     */
    @PutMapping("/update-username")
    public ApiResponse<?> updateUsername(@RequestParam String newUsername, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 1. 验证用户名格式
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ApiResponse.error(400, "用户名不能为空");
        }
        String usernameRegex = "^[a-zA-Z0-9_]{4,20}$";
        if (!newUsername.matches(usernameRegex)) {
            return ApiResponse.error(400, "用户名必须为4-20位字母、数字或下划线");
        }

        // 2. 检查用户名是否已被使用
        if (userService.isUsernameExists(newUsername)) {
            return ApiResponse.error(400, "用户名已存在");
        }

        // 3. 更新用户名
        boolean success = userService.updateUsername(userId, newUsername);
        if (success) {
            return ApiResponse.success("用户名修改成功");
        }
        return ApiResponse.error(400, "用户名修改失败");
    }

    /**
     * 修改密码
     */
    @PutMapping("/update-password")
    public ApiResponse<?> updatePassword(@RequestBody PasswordUpdateRequest request, HttpServletRequest req) {
        Integer userId = getUserIdFromToken(req);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }

        // 1. 非空校验
        if (request.getOldPassword() == null || request.getOldPassword().isEmpty() ||
                request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ApiResponse.error(400, "原密码和新密码都不能为空");
        }

        // 2. 新密码长度校验（至少6位）
        if (request.getNewPassword().length() < 6) {
            return ApiResponse.error(400, "新密码长度至少6位");
        }

        // 3. 新密码格式校验：不允许空格，只允许数字、字母、符号
        String passwordRegex = "^[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$";
        if (!request.getNewPassword().matches(passwordRegex)) {
            return ApiResponse.error(400, "新密码只能包含数字、字母和符号，不能包含空格");
        }

        // 4. 新旧密码不能相同
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

}