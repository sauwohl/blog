package com.blog.service;

import com.blog.dto.QResult;
import com.blog.entity.User;
import com.blog.dto.OperVO;
import com.github.pagehelper.PageInfo;

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

    // 账号操作相关方法
    OperVO kickOutUser(String account);
    OperVO banUser(String account);
    OperVO unbanUser(String account);
    boolean isUserBanned(String account);
}
