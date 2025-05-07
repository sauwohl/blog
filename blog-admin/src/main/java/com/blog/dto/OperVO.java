package com.blog.dto;

import lombok.Data;

@Data
public class OperVO {
    private String account; // 用户账号
    private int operate;    // 0：解封，1：封禁，2：踢蹬
}
