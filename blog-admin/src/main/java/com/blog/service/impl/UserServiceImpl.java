package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.QResult;
import com.blog.dto.UserInfoVO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public QResult listUsersWithPagination(int page, int size) {
        Page<UserInfoVO> pageObj = new Page<>(page, size);
        IPage<UserInfoVO> resultPage = userMapper.selectUsersWithRolesPage(pageObj);
        return new QResult(resultPage.getRecords(), resultPage.getTotal());
    }

//    public QResult listUsersWithPagination(int page, int size) {
//        // 创建分页对象
//        Page<User2> pageObj = new Page<>(page, size);
//
//        // 创建查询条件
//        QueryWrapper<User2> queryWrapper = new QueryWrapper<>();
//        queryWrapper.ne("status", "banned");  // 只查询不是封禁状态的账号
//        queryWrapper.orderByDesc("last_login_time");  // 排序：active 排在前，已登录排在前
//
//        // 选择需要的字段：id, username, status
//        queryWrapper.select("user_id", "username", "status");
//
//        // 查询分页数据
//        IPage<User2> resultPage = user2Mapper.selectPage(pageObj, queryWrapper);
//        // 封装返回结果
//        return new QResult(resultPage.getRecords(), resultPage.getTotal());
//    }

    @Override
    public String createUser(User user) {
        // 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        User existingUser2 = userMapper.selectOne(queryWrapper);
        if (existingUser2 != null) {
            return "用户名已存在";
        }

        //TODO:加密密码
        user.setPasswordHash(generateRandomPassword(10));
        user.setStatus("active");
        userMapper.insert(user);
        return "新增成功";
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public String resetPassword(Long userId) {
        // 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 随机密码
        String newPassword = generateRandomPassword(10);

        // TODO:加密新密码
        //String encryptedPassword = passwordEncoder.encode(newPassword);

        // 更新数据库中的密码（加密后）
        user.setPasswordHash(newPassword);
        userMapper.updateById(user);

        // 返回明文新密码一次
        return newPassword;
    }

    /**
     * 随机生成密码的方法
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // 判断是否已登录
    public boolean isLoggedIn(User user) {
        if (user.getLastLoginTime() != null) {
            long currentTime = System.currentTimeMillis();
            long lastLoginTime = user.getLastLoginTime().getTime();
            // 判断是否在过去 30 分钟内登录过
            return (currentTime - lastLoginTime) <= 30 * 60 * 1000;
        }
        return false;
    }

}
