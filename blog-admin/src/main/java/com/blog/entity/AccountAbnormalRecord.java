package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("account_abnormal_record")
public class AccountAbnormalRecord {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String account;  // 用户账号
    
    private String abnormalType;  // 异常类型：IP_ABNORMAL, PASSWORD_RETRY, SUSPICIOUS_ACTIVITY 等
    
    private String abnormalDetail;  // 异常详细信息
    
    private Boolean isResolved;  // 是否已解决
    
    private LocalDateTime createTime;
} 