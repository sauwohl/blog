package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("one_time_links")
public class OneTimeLink {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String token;
    
    private String content;
    
    private LocalDateTime expiryTime;
    
    private Boolean used;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
} 