package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.AbnormalRecordDTO;
import com.blog.dto.PageResult;
import com.blog.dto.SuspiciousContentDTO;
import com.blog.entity.AccountAbnormalRecord;
import com.blog.entity.User;
import com.blog.entity.Article;
import com.blog.entity.UserIpRecord;
import com.blog.mapper.AccountAbnormalRecordMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserIpRecordMapper;
import com.blog.service.AbnormalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.blog.utils.IpUtil;

@Slf4j
@Service
public class AbnormalRecordServiceImpl implements AbnormalRecordService {

    @Autowired
    private AccountAbnormalRecordMapper abnormalRecordMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ArticleMapper articleMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserIpRecordMapper userIpRecordMapper;
    
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
            log.info("添加开始时间条件: {}", beginDate.format(DATE_FORMATTER));
            queryWrapper.ge("create_time", beginDate);
        }
        if (endDate != null) {
            log.info("添加结束时间条件: {}", endDate.format(DATE_FORMATTER));
            queryWrapper.lt("create_time", endDate);
        }
        
        // 添加异常类型条件
        if (category != null) {
            log.info("添加异常类型条件: {}", category);
            if (category == 0) {
                queryWrapper.eq("abnormal_type", "IP_ABNORMAL-IP异常");
            } else if (category == 1) {
                queryWrapper.eq("abnormal_type", "PASSWORD_RETRY-异常登录");
            } else if (category == 2) {
                queryWrapper.eq("abnormal_type", "SUSPICIOUS_CONTENT-内容异常");
            }
        }
        
        // 添加处理状态条件
        if (status != null) {
            log.info("添加处理状态条件: {}", status);
            queryWrapper.eq("is_resolved", status == 1);
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc("create_time");
        
        // 打印最终的SQL语句
        log.info("执行查询的SQL条件: {}", queryWrapper.getSqlSegment());

        // 执行分页查询
        Page<AccountAbnormalRecord> pageResult = abnormalRecordMapper.selectPage(
            new Page<>(page, perPage),
            queryWrapper
        );
        
        // 打印查询结果数量
        log.info("查询结果总数: {}", pageResult.getTotal());

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
            abnormalRecordMapper.updateById(record);
        }
    }

    @Override
    public PageResult<SuspiciousContentDTO> listSuspiciousContents(int page, int size) {
        // 1. 先查询标记为可疑活动的异常记录
        QueryWrapper<AccountAbnormalRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("abnormal_type", "SUSPICIOUS_CONTENT-内容异常")
                   .orderByDesc("create_time");
        
        Page<AccountAbnormalRecord> pageResult = abnormalRecordMapper.selectPage(
            new Page<>(page, size),
            queryWrapper
        );
        
        // 2. 获取相关的文章内容
        List<SuspiciousContentDTO> records = pageResult.getRecords().stream()
            .map(record -> {
                SuspiciousContentDTO dto = new SuspiciousContentDTO();
                dto.setAccount(record.getAccount());
                dto.setPublishTime(record.getCreateTime().format(DATE_FORMATTER));
                
                try {
                    // 从abnormal_detail中解析文章ID
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode detailNode = mapper.readTree(record.getAbnormalDetail());
                    Long articleId = detailNode.get("articleId").asLong();
                    
                    // 查询文章信息
                    Article article = articleMapper.selectById(articleId);
                    if (article != null) {
                        dto.setContent(article.getContent());
                    } else {
                        dto.setContent("文章已删除");
                    }
                } catch (Exception e) {
                    log.error("解析内容详情失败", e);
                    dto.setContent("内容解析失败");
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        return PageResult.of(records, pageResult.getTotal(), page, size);
    }

    @Override
    public AccountAbnormalRecord getById(Long id) {
        return abnormalRecordMapper.selectById(id);
    }

    @Override
    public Map<String, Object> getAbnormalRecordDetail(String id) {
        AccountAbnormalRecord record = abnormalRecordMapper.selectById(id);
        if (record == null) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("account", record.getAccount());
        data.put("publish_time", record.getCreateTime().format(DATE_FORMATTER));

        // 设置异常描述
        String description;
        String abnormalType = record.getAbnormalType();
        if ("IP_ABNORMAL-IP异常".equals(abnormalType)) {
            description = "IP异常登录";
        } else if ("PASSWORD_RETRY-异常登录".equals(abnormalType)) {
            description = "密码重试次数过多";
        } else if ("SUSPICIOUS_CONTENT-内容异常".equals(abnormalType)) {
            description = "发布异常内容";
        } else {
            description = "未知异常";
        }
        data.put("description", description);
        data.put("abnormal_detail", record.getAbnormalDetail());

        // 如果是可疑内容，需要额外包含博客信息
        if ("SUSPICIOUS_CONTENT-内容异常".equals(abnormalType)) {
            try {
                JsonNode detailNode = objectMapper.readTree(record.getAbnormalDetail());
                Long articleId = detailNode.get("articleId").asLong();
                // 查询文章信息
                Article article = articleMapper.selectById(articleId);
                if (article != null) {
                    data.put("title", article.getTitle());
                    data.put("content", article.getContent());
                } else {
                    data.put("title", "文章已删除");
                    data.put("content", "文章内容不可用");
                }
            } catch (Exception e) {
                log.error("解析异常记录详情失败", e);
                data.put("title", "解析失败");
                data.put("content", "内容解析失败");
            }
        }

        return data;
    }

    @Override
    public void checkAndRecordIpAbnormal(String account, String ipAddress) {
        // 1. 查询该IP是否有记录
        UserIpRecord record = userIpRecordMapper.getRecord(account, ipAddress);
        
        if (record != null) {
            // 2. 更新访问次数
            record.setCount(record.getCount() + 1);
            // 如果访问次数超过10次，标记为常用IP
            if (record.getCount() >= 10) {
                record.setIsCommon(true);
            }
            userIpRecordMapper.updateById(record);
            return;
        }
        
        // 3. 如果是新IP，先查询用户的常用IP列表
        List<UserIpRecord> commonIps = userIpRecordMapper.listCommonIps(account);
        
        // 4. 如果没有常用IP记录，或者这是第一次访问，直接记录
        if (commonIps.isEmpty()) {
            saveNewIpRecord(account, ipAddress, false);
            return;
        }
        
        // 5. 如果有常用IP但当前IP不在其中，记录异常
        saveNewIpRecord(account, ipAddress, false);
        saveAbnormalRecord(account, ipAddress);
    }

    private void saveNewIpRecord(String account, String ipAddress, boolean isCommon) {
        UserIpRecord newRecord = new UserIpRecord();
        newRecord.setAccount(account);
        newRecord.setIp(ipAddress);
        newRecord.setCount(1);
        newRecord.setIsCommon(isCommon);
        userIpRecordMapper.insert(newRecord);
    }

    private void saveAbnormalRecord(String account, String ipAddress) {
        AccountAbnormalRecord abnormalRecord = new AccountAbnormalRecord();
        abnormalRecord.setAccount(account);
        abnormalRecord.setAbnormalType("IP_ABNORMAL");
        
        Map<String, Object> detail = new HashMap<>();
        detail.put("ip", ipAddress);
        detail.put("timestamp", LocalDateTime.now().toString());
        detail.put("description", "检测到非常用IP地址访问");
        
        try {
            abnormalRecord.setAbnormalDetail(objectMapper.writeValueAsString(detail));
        } catch (JsonProcessingException e) {
            log.error("JSON序列化异常: {}", e.getMessage());
            return;
        }
        
        abnormalRecord.setIsResolved(false);
        abnormalRecord.setCreateTime(LocalDateTime.now());
        abnormalRecordMapper.insert(abnormalRecord);
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
        if ("IP_ABNORMAL-IP异常".equals(abnormalType)) {
            description = "IP异常登录";
        } else if ("PASSWORD_RETRY-异常登录".equals(abnormalType)) {
            description = "密码重试次数过多";
        } else if ("SUSPICIOUS_CONTENT-内容异常".equals(abnormalType)) {
            description = "发布异常内容";
        } else {
            description = "未知异常";
        }
        
//        if (record.getIpAddress() != null) {
//            description += String.format("（IP: %s, 位置: %s）",
//                record.getIpAddress(),
//                record.getLocation());
//        }
        dto.setDescription(description);
        
        // 设置时间
        dto.setPublishTime(record.getCreateTime().format(DATE_FORMATTER));
        
        // 设置状态
        dto.setStatus(record.getIsResolved() ? 1 : 0);
        
        // 设置分类
        if ("IP_ABNORMAL-IP异常".equals(abnormalType)) {
            dto.setCategory(0);
        } else if ("PASSWORD_RETRY-登陆异常".equals(abnormalType)) {
            dto.setCategory(1);
        } else if ("SUSPICIOUS_CONTENT-内容异常".equals(abnormalType)) {
            dto.setCategory(2);
        } else {
            dto.setCategory(-1);
        }
        
        return dto;
    }
} 