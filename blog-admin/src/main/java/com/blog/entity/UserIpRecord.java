package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_ip_record")
public class UserIpRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String account;
    
    private String ip;
    
    private Integer count;
    
    private Boolean isCommon;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
} 