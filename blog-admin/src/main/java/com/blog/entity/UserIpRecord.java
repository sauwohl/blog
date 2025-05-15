package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_ip_records")
public class UserIpRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String ipAddress;
    
    private String location;  // IP地理位置
    
    private Boolean isCommon;  // 是否为常用地址
    
    private Integer loginCount;  // 该IP的登录次数
    
    private LocalDateTime firstLoginTime;
    
    private LocalDateTime lastLoginTime;
    
    private Boolean isAbnormal;  // 是否标记为异常
    
    private String abnormalReason;  // 异常原因
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
} 