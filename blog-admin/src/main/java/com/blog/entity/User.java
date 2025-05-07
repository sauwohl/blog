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
    private String email;         // 对应数据库字段 email
    private String passwordHash;  // 对应数据库字段 password_hash
    private String phone;         // 对应数据库字段 phone
    private String status;        // 对应数据库字段 status
    private Date lastLoginTime;   // 对应数据库字段 last_login_time
    private Date createdAt;       // 对应数据库字段 created_at
    private Date updatedAt;       // 对应数据库字段 updated_at
}
