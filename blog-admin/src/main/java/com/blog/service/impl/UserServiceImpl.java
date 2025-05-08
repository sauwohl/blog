package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.QResult;
import com.blog.entity.User;
import com.blog.dto.OperVO;
import com.blog.enums.AccountOperationType;
import com.blog.mapper.UserMapper;
import com.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询用户列表
     * 按用户身份和登录时间排序
     *
     * @param page 当前页码
     * @param size 每页大小
     * @return 分页查询结果
     */
    @Override
    public QResult listUsersWithPagination(int page, int size) {
        Page<User> pageObj = new Page<>(page, size);
        IPage<User> resultPage = userMapper.selectUsersPage(pageObj);
        return new QResult(resultPage.getRecords(), resultPage.getTotal());
    }

    /**
     * 创建新用户
     * 
     * @param user 用户信息
     * @return 创建结果提示信息
     */
    @Override
    @Transactional
    public String createUser(User user) {
        // 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        User existingUser = userMapper.selectOne(queryWrapper);
        if (existingUser != null) {
            return "用户名已存在";
        }

        // 设置默认值
        user.setPasswordHash(generateRandomPassword(10));
        user.setStatus("active");
        user.setIdentity("0"); // 默认为普通用户
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        
        userMapper.insert(user);
        return "新增成功";
    }

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    /**
     * 重置用户密码
     *
     * @param userId 用户ID
     * @return 新生成的随机密码
     * @throws RuntimeException 用户不存在时抛出异常
     */
    @Override
    @Transactional
    public String resetPassword(Long userId) {
        // 查询用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 生成随机密码
        String newPassword = generateRandomPassword(10);

        // TODO:加密新密码
        //String encryptedPassword = passwordEncoder.encode(newPassword);

        // 更新数据库中的密码
        user.setPasswordHash(newPassword);
        userMapper.updateById(user);

        return newPassword;
    }

    /**
     * 踢出用户
     * 更新用户最后登出时间，并通知认证模块清除用户token
     *
     * @param account 用户账号
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperVO kickOutUser(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", account);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            // 更新最后登出时间
            user.setLastLogoutTime(new Date());
            userMapper.updateById(user);
            
            // TODO: 通知认证模块清除用户token
        }
        
        return new OperVO(account, AccountOperationType.KICK_OUT);
    }

    /**
     * 封禁用户账号
     * 将用户状态改为banned，更新登出时间，并通知认证模块清除用户token
     *
     * @param account 用户账号
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperVO banUser(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", account);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            user.setStatus("banned");
            user.setLastLogoutTime(new Date());
            userMapper.updateById(user);
            
            // TODO: 通知认证模块清除用户token
            // authService.invalidateUserToken(account);
        }
        
        return new OperVO(account, AccountOperationType.BAN);
    }

    /**
     * 解封用户账号
     * 将用户状态改为active
     *
     * @param account 用户账号
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperVO unbanUser(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", account);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            user.setStatus("active");
            userMapper.updateById(user);
        }
        
        return new OperVO(account, AccountOperationType.UNBAN);
    }

    /**
     * 检查用户是否被封禁
     *
     * @param account 用户账号
     * @return true表示已封禁，false表示未封禁
     */
    @Override
    public boolean isUserBanned(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", account);
        User user = userMapper.selectOne(queryWrapper);
        
        return user != null && "banned".equals(user.getStatus());
    }

    /**
     * 生成指定长度的随机密码
     * 包含大小写字母和数字
     *
     * @param length 密码长度
     * @return 随机生成的密码
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
}
