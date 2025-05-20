package com.blog.dto;

import lombok.Data;

@Data
public class SuspiciousContentDTO {
    private String account;        // 发布者账号
    private String content;        // 发布内容
    private String publishTime;    // 发布时间
} 