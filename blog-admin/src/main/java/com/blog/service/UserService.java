package com.blog.service;

import com.blog.dto.CreateUserDTO;
import com.blog.dto.QResult;
import com.blog.entity.User;
import com.blog.dto.OperVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface UserService {

    /**
     * 分页查询用户列表
     * @param page 当前页码
     * @param size 每页大小
     * @param account 账号（可选，用于模糊搜索）
     * @return 分页查询结果
     */
    Map<String, Object> listUsersWithPagination(int page, int size, String account);

    /**
     * 创建新用户
     *
     * @param createUserDTO 创建用户的请求参数
     * @return 包含创建结果的Map，包括：
     *         success: 是否成功
     *         message: 提示信息
     *         password: 加密后的密码（仅在成功时返回）
     */
    Map<String, Object> createUser(CreateUserDTO createUserDTO);

    User getUserById(Long id);

    /**
     * 根据账号查询用户
     * @param account 用户账号
     * @return 用户信息
     */
    User getUserByAccount(String account);

    String resetPassword(Long id);

    // 账号操作相关方法
    OperVO kickOutUser(String account);
    OperVO banUser(String account);
    OperVO unbanUser(String account);
    boolean isUserBanned(String account);
}
