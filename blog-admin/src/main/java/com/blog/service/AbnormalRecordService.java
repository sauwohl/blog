package com.blog.service;

import com.blog.dto.AbnormalRecordDTO;
import com.blog.dto.PageResult;

public interface AbnormalRecordService {
    
    /**
     * 分页查询异常记录
     */
    PageResult<AbnormalRecordDTO> listAbnormalRecords(int page, int perPage, Integer category, Integer status);
    
    /**
     * 获取单条异常记录
     */
    AbnormalRecordDTO getAbnormalRecord(String id);
    
    /**
     * 标记异常记录为已处理
     */
    void resolveAbnormalRecord(String id);
} 