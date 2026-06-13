package com.movie.mapper;

import com.movie.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findByEmail(@Param("email") String email);
    User findByUserId(@Param("userId") Integer userId);

    // ✅ 新增：通过用户名或邮箱查找（用于登录）
    User findByUsernameOrEmail(@Param("account") String account);

    int insert(User user);
    int updatePassword(@Param("userId") Integer userId, @Param("password") String password);
    int updateUser(User user);
    int updateEmail(@Param("userId") Integer userId, @Param("email") String email);
    int updateUsername(@Param("userId") Integer userId, @Param("username") String username);

    // ✅ 新增：更新头像
    int updateAvatar(@Param("userId") Integer userId, @Param("avatarUrl") String avatarUrl);
}