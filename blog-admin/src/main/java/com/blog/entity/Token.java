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
    
} 