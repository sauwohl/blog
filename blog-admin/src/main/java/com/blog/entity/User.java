package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("users")  // 对应数据库表 "users"
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;          // 对应数据库字段 user_id
    private String username;      // 对应数据库字段 username
    private String password;  // 对应数据库字段 password
    private String phone;         // 对应数据库字段 phone
    private String status;        // 对应数据库字段 status，用于标记账号状态：active/banned
    private String identity;      // 对应数据库字段 user_type，0：普通用户，1：管理员
    private Date lastLoginTime;   // 对应数据库字段 last_login_time
    private Date lastLogoutTime;  // 对应数据库字段 last_logout_time
    private Date createdAt;       // 对应数据库字段 created_at
    private Date updatedAt;       // 对应数据库字段 updated_at

}
