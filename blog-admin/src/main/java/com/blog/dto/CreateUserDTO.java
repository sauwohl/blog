package com.blog.dto;

import lombok.Data;

@Data
public class CreateUserDTO {
    private String account;
    private String email;  // 用于接收账号信息的邮箱
} 