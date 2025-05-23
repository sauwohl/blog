package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tokens")
public class Token {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String account;
    
    private String token;
    
    private LocalDateTime expireTime;    // token过期时间
    
    private LocalDateTime lastUsedTime;  // 最后使用时间
    
    private Integer status;              // 状态：0-无效，1-有效
    
    private LocalDateTime createTime;    // 创建时间
} 