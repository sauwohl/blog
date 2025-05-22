package com.blog.dto;

import lombok.Data;

@Data
public class CreateUserDTO {
    private String account;    // 用户账号
    private String email;      // 用于接收账号信息的邮箱
} 