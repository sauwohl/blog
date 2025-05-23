package com.blog.service;

import com.blog.dto.AbnormalRecordDTO;
import com.blog.dto.PageResult;
import com.blog.dto.SuspiciousContentDTO;
import com.blog.entity.AccountAbnormalRecord;

import java.time.LocalDateTime;
import java.util.Map;

public interface AbnormalRecordService {
    
    /**
     * 分页查询异常记录
     * @param page 页码
     * @param perPage 每页数量
     * @param category 异常类别
     * @param status 处理状态
     * @param beginDate 起始时间（可选）
     * @param endDate 截止时间（可选）
     * @return 分页结果
     */
    PageResult<AbnormalRecordDTO> listAbnormalRecords(
            int page, 
            int perPage, 
            Integer category, 
            Integer status,
            LocalDateTime beginDate,
            LocalDateTime endDate);
    
    /**
     * 获取单条异常记录
     */
    AbnormalRecordDTO getAbnormalRecord(String id);
    
    /**
     * 标记异常记录为已处理
     */
    void resolveAbnormalRecord(String id);

    /**
     * 分页查询可疑内容
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<SuspiciousContentDTO> listSuspiciousContents(int page, int size);

    AccountAbnormalRecord getById(Long id);

    /**
     * 获取异常记录详情
     * @param id 异常记录ID
     * @return 异常记录详情，包含不同类型的异常信息
     */
    Map<String, Object> getAbnormalRecordDetail(String id);
} 