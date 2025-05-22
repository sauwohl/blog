package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.CreateUserDTO;
import com.blog.dto.QResult;
import com.blog.entity.User;
import com.blog.dto.OperVO;
import com.blog.enums.AccountOperationType;
import com.blog.mapper.UserMapper;
import com.blog.service.EmailService;
import com.blog.service.UserService;
import com.blog.utils.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表
     * 按用户身份和登录时间排序
     *
     * @param page 当前页码
     * @param size 每页大小
     * @param account 账号模糊搜索条件
     * @return 分页查询结果
     */
    @Override
    public Map<String, Object> listUsersWithPagination(int page, int size, String account) {
        Page<User> pageObj = new Page<>(page, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        
        // 如果提供了account参数，添加模糊搜索条件
        if (account != null && !account.trim().isEmpty()) {
            queryWrapper.like("account", account.trim());
        }
        
        // 按状态降序排序
        queryWrapper.orderByDesc("status");
        
        IPage<User> resultPage = userMapper.selectPage(pageObj, queryWrapper);
        
        List<Map<String, Object>> results = resultPage.getRecords().stream()
            .map(user -> {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("userId", user.getId());
                userMap.put("account", user.getAccount());
                userMap.put("username", user.getUsername());
                userMap.put("status", user.getStatus());
                userMap.put("password", user.getPassword());
                userMap.put("phone", user.getPhone());
                return userMap;
            })
            .collect(Collectors.toList());
            
        Map<String, Object> data = new HashMap<>();
        data.put("results", results);
        data.put("total", resultPage.getTotal());
        data.put("page", page);
        data.put("per_page", size);
        
        return data;
    }

    /**
     * 创建新用户
     * 
     *
     * @return 创建结果提示信息
     */

    @Override
    @Transactional
    public String createUser(CreateUserDTO createUserDTO) {
        try {
            // 检查账号是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("account", createUserDTO.getAccount());
            User existingUser = userMapper.selectOne(queryWrapper);
            if (existingUser != null) {
                return "账号已存在";
            }

            // 使用解密后的密码
            String password = createUserDTO.getDecryptedPassword();
            // 对解密后的密码进行加密存储
            String encodedPassword = passwordEncoder.encode(password);
            
            // 创建新用户对象并设置默认值
            User user = new User();
            user.setAccount(createUserDTO.getAccount());
            user.setUsername(createUserDTO.getAccount());  // 默认用账号作为用户名
            user.setPassword(encodedPassword);
            user.setPhone(null);
            user.setImage("");  // 默认头像
            user.setIdentity(User.NORMAL_USER);          // 默认为普通用户
            user.setStatus(User.STATUS_OFFLINE);         // 默认为离线状态
            user.setCreateTime(new Date());
            
            // 保存用户
            userMapper.insert(user);
            
            // 发送账号信息邮件
            emailService.sendAccountInfo(createUserDTO.getEmail(), createUserDTO.getAccount(), password);
            
            return "用户创建成功，账号信息已发送至邮箱";
        } catch (Exception e) {
            throw new RuntimeException("创建用户失败：" + e.getMessage());
        }
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
     * 根据账号查询用户
     *
     * @param account 用户账号
     * @return 用户信息，如果用户不存在则返回null
     */
    @Override
    public User getUserByAccount(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", account);
        return userMapper.selectOne(queryWrapper);
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
        
        // 对密码进行加密
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        userMapper.updateById(user);

        return newPassword;  // 返回明文密码给管理员
    }

    /**
     * 踢出用户
     * 将用户状态改为离线
     *
     * @param account 用户账号
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperVO kickOutUser(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", account);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            user.setStatus(User.STATUS_OFFLINE);
            userMapper.updateById(user);
        }
        
        return new OperVO(account, AccountOperationType.KICK_OUT);
    }

    /**
     * 封禁用户账号
     * 将用户状态改为禁封
     *
     * @param account 用户账号
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperVO banUser(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", account);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            user.setStatus(User.STATUS_BANNED);
            userMapper.updateById(user);
        }
        
        return new OperVO(account, AccountOperationType.BAN);
    }

    /**
     * 解封用户账号
     * 将用户状态改为离线
     *
     * @param account 用户账号
     * @return 操作结果
     */
    @Override
    @Transactional
    public OperVO unbanUser(String account) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account", account);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user != null) {
            user.setStatus(User.STATUS_OFFLINE);
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
        queryWrapper.eq("account", account);
        User user = userMapper.selectOne(queryWrapper);
        
        return user != null && user.getStatus() == User.STATUS_BANNED;
    }

    /**
     * 生成指定长度的随机密码
     * 包含大小写字母和数字
     *
     * @param length 密码长度
     * @return 随机生成的密码
     */
    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder password = new StringBuilder();
        SecureRandom random = new SecureRandom();
        
        // 确保密码包含至少一个数字、一个小写字母、一个大写字母和一个特殊字符
        password.append(chars.charAt(random.nextInt(10))); // 数字
        password.append(chars.charAt(10 + random.nextInt(26))); // 大写字母
        password.append(chars.charAt(36 + random.nextInt(26))); // 小写字母
        password.append(chars.charAt(62 + random.nextInt(10))); // 特殊字符
        
        // 填充剩余长度
        for (int i = 4; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // 打乱密码字符顺序
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
}
