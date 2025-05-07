package com.blog.service;

import com.blog.dto.QResult;
import com.blog.entity.User;

import java.util.List;

public interface UserService {

    /**
     * 查询所有用户
     */
    QResult listUsersWithPagination(int page, int size);

    /**
     * 创建新用户
     */
    String createUser(User user);

    User getUserById(Long id);

    String resetPassword(Long id);
}
