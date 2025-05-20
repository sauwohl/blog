package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.entity.AccountAbnormalRecord;
import com.blog.entity.UserIpRecord;
import com.blog.mapper.AccountAbnormalRecordMapper;
import com.blog.mapper.UserIpRecordMapper;
import com.blog.service.IpDetectionService;
import com.blog.utils.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class IpDetectionServiceImpl implements IpDetectionService {

    @Autowired
    private UserIpRecordMapper userIpRecordMapper;
    
    @Autowired
    private AccountAbnormalRecordMapper abnormalRecordMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    private static final int LOGIN_COUNT_THRESHOLD = 5; // 登录次数阈值，超过此次数认为是常用IP

    @Override
    public boolean checkIpAbnormal(Long userId, String account, String ipAddress) {
        // 检查是否为内网IP
        if (IpUtil.isInternalIp(ipAddress)) {
            return false;
        }

        // 查询该用户的IP记录
        QueryWrapper<UserIpRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("ip_address", ipAddress);
        
        UserIpRecord ipRecord = userIpRecordMapper.selectOne(queryWrapper);
        
        if (ipRecord == null) {
            // 新IP，记录并标记为异常
            ipRecord = new UserIpRecord();
            ipRecord.setUserId(userId);
            ipRecord.setIpAddress(ipAddress);
            ipRecord.setLoginCount(1);
            ipRecord.setFirstLoginTime(LocalDateTime.now());
            ipRecord.setLastLoginTime(LocalDateTime.now());
            ipRecord.setIsCommon(false);
            userIpRecordMapper.insert(ipRecord);
            
            // 记录异常
            recordIpAbnormal(userId, account, ipAddress, true);
            return true;
        } else {
            // 更新登录次数和时间
            ipRecord.setLoginCount(ipRecord.getLoginCount() + 1);
            ipRecord.setLastLoginTime(LocalDateTime.now());
            
            // 如果登录次数达到阈值，标记为常用IP
            if (ipRecord.getLoginCount() >= LOGIN_COUNT_THRESHOLD) {
                ipRecord.setIsCommon(true);
            }
            
            userIpRecordMapper.updateById(ipRecord);
            
            // 如果不是常用IP，记录异常
            if (!ipRecord.getIsCommon()) {
                recordIpAbnormal(userId, account, ipAddress, false);
                return true;
            }
        }
        
        return false;
    }

    private void recordIpAbnormal(Long userId, String account, String ipAddress, boolean isFirstTime) {
        try {
            // 构建异常详情
            Map<String, Object> details = new HashMap<>();
            details.put("ipAddress", ipAddress);
            details.put("isFirstTimeIp", isFirstTime);
            
            // 创建异常记录
            AccountAbnormalRecord record = new AccountAbnormalRecord();
            record.setUserId(userId);
            record.setAccount(account);
            record.setAbnormalType("IP_ABNORMAL");
            record.setAbnormalDetail(objectMapper.writeValueAsString(details));
            record.setIsResolved(false);
            record.setCreateTime(LocalDateTime.now());
            abnormalRecordMapper.insert(record);
        } catch (Exception e) {
            // 如果JSON序列化失败，使用简单的异常详情
            AccountAbnormalRecord record = new AccountAbnormalRecord();
            record.setUserId(userId);
            record.setAccount(account);
            record.setAbnormalType("IP_ABNORMAL");
            record.setAbnormalDetail("非常用IP登录: " + ipAddress);
            record.setIsResolved(false);
            record.setCreateTime(LocalDateTime.now());
            abnormalRecordMapper.insert(record);
        }
    }

    @Override
    public void markIpAsCommon(Long userId, String ipAddress) {
        QueryWrapper<UserIpRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("ip_address", ipAddress);
        
        UserIpRecord ipRecord = userIpRecordMapper.selectOne(queryWrapper);
        if (ipRecord != null) {
            ipRecord.setIsCommon(true);
            userIpRecordMapper.updateById(ipRecord);
        }
    }
} 