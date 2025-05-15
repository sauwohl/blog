package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.AbnormalRecordDTO;
import com.blog.dto.PageResult;
import com.blog.entity.AccountAbnormalRecord;
import com.blog.entity.User;
import com.blog.mapper.AccountAbnormalRecordMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.AbnormalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class AbnormalRecordServiceImpl implements AbnormalRecordService {

    @Autowired
    private AccountAbnormalRecordMapper abnormalRecordMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<AbnormalRecordDTO> listAbnormalRecords(
            int page, 
            int perPage, 
            Integer category, 
            Integer status,
            LocalDateTime beginDate,
            LocalDateTime endDate) {
        // 构建查询条件
        QueryWrapper<AccountAbnormalRecord> queryWrapper = new QueryWrapper<>();
        
        // 添加时间范围条件
        if (beginDate != null) {
            queryWrapper.ge("create_time", beginDate);
        }
        if (endDate != null) {
            queryWrapper.le("create_time", endDate);
        }
        
        // 添加异常类型条件
        if (category != null) {
            if (category == 0) {
                queryWrapper.eq("abnormal_type", "IP_ABNORMAL");
            } else if (category == 1) {
                queryWrapper.eq("abnormal_type", "PASSWORD_RETRY");
            } else if (category == 2) {
                queryWrapper.eq("abnormal_type", "SUSPICIOUS_ACTIVITY");
            }
        }
        
        // 添加处理状态条件
        if (status != null) {
            queryWrapper.eq("is_resolved", status == 1);
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc("create_time");

        // 执行分页查询
        Page<AccountAbnormalRecord> pageResult = abnormalRecordMapper.selectPage(
            new Page<>(page, perPage),
            queryWrapper
        );

        // 转换为DTO
        List<AbnormalRecordDTO> records = pageResult.getRecords().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        // 构建分页结果
        return PageResult.of(records, pageResult.getTotal(), page, perPage);
    }

    @Override
    public AbnormalRecordDTO getAbnormalRecord(String id) {
        AccountAbnormalRecord record = abnormalRecordMapper.selectById(id);
        return record != null ? convertToDTO(record) : null;
    }

    @Override
    public void resolveAbnormalRecord(String id) {
        AccountAbnormalRecord record = abnormalRecordMapper.selectById(id);
        if (record != null && !record.getIsResolved()) {
            record.setIsResolved(true);
            record.setUpdateTime(LocalDateTime.now());
            abnormalRecordMapper.updateById(record);
        }
    }

    private AbnormalRecordDTO convertToDTO(AccountAbnormalRecord record) {
        AbnormalRecordDTO dto = new AbnormalRecordDTO();
        dto.setId(String.valueOf(record.getId()));
        
        // 获取用户账号
        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            dto.setAccount(user.getAccount());
        } else {
            // 如果通过ID找不到用户，尝试从异常记录中获取账号
            String account = record.getAccount();
            dto.setAccount(account != null ? account : "未知账号");
        }
        
        // 设置异常描述
        String description;
        String abnormalType = record.getAbnormalType();
        if ("IP_ABNORMAL".equals(abnormalType)) {
            description = "IP异常登录";
        } else if ("PASSWORD_RETRY".equals(abnormalType)) {
            description = "密码重试次数过多";
        } else if ("SUSPICIOUS_ACTIVITY".equals(abnormalType)) {
            description = "可疑操作";
        } else {
            description = "未知异常";
        }
        
        if (record.getIpAddress() != null) {
            description += String.format("（IP: %s, 位置: %s）", 
                record.getIpAddress(), 
                record.getLocation());
        }
        dto.setDescription(description);
        
        // 设置时间
        dto.setPublishTime(record.getCreateTime().format(DATE_FORMATTER));
        
        // 设置状态
        dto.setStatus(record.getIsResolved() ? 1 : 0);
        
        // 设置分类
        if ("IP_ABNORMAL".equals(abnormalType)) {
            dto.setCategory(0);
        } else if ("PASSWORD_RETRY".equals(abnormalType)) {
            dto.setCategory(1);
        } else if ("SUSPICIOUS_ACTIVITY".equals(abnormalType)) {
            dto.setCategory(2);
        } else {
            dto.setCategory(-1);
        }
        
        return dto;
    }
} 