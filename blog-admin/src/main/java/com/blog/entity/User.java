package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Getter
@Setter
@TableName("user")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String account;
    
    private String username;
    
    private String password;
    
    private String image;
    
    private String phone;
    
    private Integer identity;
    
    private Integer status;
    
    private Date createTime;
    
    private String info;
    
    // 用户身份常量
    public static final int NORMAL_USER = 0;
    public static final int ADMIN_USER = 1;
    
    // 用户状态常量
    public static final int STATUS_OFFLINE = 0;
    public static final int STATUS_ONLINE = 1;
    public static final int STATUS_BANNED = 2;
    
    // 判断是否是管理员
    public boolean isAdmin() {
        return ADMIN_USER == this.identity;
    }
    
    // 判断是否被封禁
    public boolean isBanned() {
        return STATUS_BANNED == this.status;
    }
    
    // 判断是否在线
    public boolean isOnline() {
        return STATUS_ONLINE == this.status;
    }
}
