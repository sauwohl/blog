package com.blog.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String roleName;
    private String status;
    private Date lastLoginTime;
}

