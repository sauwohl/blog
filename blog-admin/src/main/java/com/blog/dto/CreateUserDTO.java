package com.blog.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
public class CreateUserDTO {
    private String account;
    private String email;  // 用于接收账号信息的邮箱
    private String encryptedPassword;  // 前端传来的加密密码
    
    @JsonIgnore  // 不会在JSON序列化中显示
    private String decryptedPassword;  // 解密后的密码
} 