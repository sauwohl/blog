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
     * @return 分页查询结果
     */
    Map<String, Object> listUsersWithPagination(int page, int size);

    /**
     * 创建新用户
     */
    @Transactional
    String createUser(CreateUserDTO createUserDTO);

    User getUserById(Long id);

    String resetPassword(Long id);

    // 账号操作相关方法
    OperVO kickOutUser(String account);
    OperVO banUser(String account);
    OperVO unbanUser(String account);
    boolean isUserBanned(String account);
}
