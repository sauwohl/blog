package com.blog.dto;

import lombok.Data;

@Data
public class AbnormalRecordDTO {
    private String id;
    private String account;        // 用户账号
    private String description;    // 异常描述
    private String publishTime;    // 发生时间
    private Integer status;        // 状态：0-未处理，1-已处理
    private Integer category;      // 异常类别：0-IP异常，1-密码错误，2-可疑操作等
    private String abnormalDetail;
} 