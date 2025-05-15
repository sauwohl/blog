package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.entity.UserIpRecord;
import com.blog.entity.AccountAbnormalRecord;
import com.blog.entity.User;
import com.blog.mapper.UserIpRecordMapper;
import com.blog.mapper.AccountAbnormalRecordMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.IpDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class IpDetectionServiceImpl implements IpDetectionService {

    @Autowired
    private UserIpRecordMapper userIpRecordMapper;
    
    @Autowired
    private AccountAbnormalRecordMapper accountAbnormalRecordMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public AccountAbnormalRecord checkIpAbnormal(Long userId, String ipAddress) {
        // 1. 查询该IP是否是用户的常用IP
        UserIpRecord ipRecord = userIpRecordMapper.findByUserIdAndIp(userId, ipAddress);
        
        // 2. 如果是首次使用的IP或非常用IP，创建异常记录
        if (ipRecord == null || !ipRecord.getIsCommon()) {
            AccountAbnormalRecord abnormalRecord = new AccountAbnormalRecord();
            abnormalRecord.setUserId(userId);
            
            // 获取并设置用户账号
            User user = userMapper.selectById(userId);
            if (user != null) {
                abnormalRecord.setAccount(user.getAccount());
            }
            
            abnormalRecord.setAbnormalType("IP_ABNORMAL");
            abnormalRecord.setIpAddress(ipAddress);
            abnormalRecord.setLocation(getIpLocation(ipAddress));
            abnormalRecord.setIsResolved(false);
            abnormalRecord.setCreateTime(LocalDateTime.now());
            abnormalRecord.setUpdateTime(LocalDateTime.now());
            
            try {
                // 记录详细信息
                Map<String, Object> details = new HashMap<>();
                details.put("isFirstTimeIp", ipRecord == null);
                if (ipRecord != null) {
                    details.put("loginCount", ipRecord.getLoginCount());
                    details.put("firstLoginTime", ipRecord.getFirstLoginTime());
                }
                List<UserIpRecord> commonIps = userIpRecordMapper.findCommonIpsByUserId(userId);
                details.put("commonIpAddresses", commonIps.stream()
                    .map(UserIpRecord::getIpAddress)
                    .toList());
                
                abnormalRecord.setAbnormalDetail(objectMapper.writeValueAsString(details));
            } catch (Exception e) {
                abnormalRecord.setAbnormalDetail("详细信息序列化失败");
            }
            
            accountAbnormalRecordMapper.insert(abnormalRecord);
            return abnormalRecord;
        }
        
        return null;
    }

    @Override
    public void recordUserIp(Long userId, String ipAddress) {
        UserIpRecord ipRecord = userIpRecordMapper.findByUserIdAndIp(userId, ipAddress);
        
        if (ipRecord == null) {
            // 新IP记录
            ipRecord = new UserIpRecord();
            ipRecord.setUserId(userId);
            ipRecord.setIpAddress(ipAddress);
            ipRecord.setLocation(getIpLocation(ipAddress));
            ipRecord.setLoginCount(1);
            ipRecord.setFirstLoginTime(LocalDateTime.now());
            ipRecord.setLastLoginTime(LocalDateTime.now());
            ipRecord.setIsCommon(false);
            userIpRecordMapper.insert(ipRecord);
        } else {
            // 更新现有记录
            ipRecord.setLoginCount(ipRecord.getLoginCount() + 1);
            ipRecord.setLastLoginTime(LocalDateTime.now());
            
            // 如果登录次数达到阈值，自动标记为常用IP
            if (ipRecord.getLoginCount() >= 5 && !ipRecord.getIsCommon()) {
                ipRecord.setIsCommon(true);
                // 解决相关的异常记录
                resolveIpAbnormalRecord(userId, ipAddress);
            }
            
            userIpRecordMapper.updateById(ipRecord);
        }
    }

    private void resolveIpAbnormalRecord(Long userId, String ipAddress) {
        QueryWrapper<AccountAbnormalRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("ip_address", ipAddress)
               .eq("abnormal_type", "IP_ABNORMAL")
               .eq("is_resolved", false);
               
        AccountAbnormalRecord record = accountAbnormalRecordMapper.selectOne(wrapper);
        if (record != null) {
            record.setIsResolved(true);
            record.setUpdateTime(LocalDateTime.now());
            accountAbnormalRecordMapper.updateById(record);
        }
    }

    @Override
    public void markIpAsCommon(Long userId, String ipAddress) {
        UserIpRecord ipRecord = userIpRecordMapper.findByUserIdAndIp(userId, ipAddress);
        if (ipRecord != null) {
            ipRecord.setIsCommon(true);
            userIpRecordMapper.updateById(ipRecord);
            // 解决相关的异常记录
            resolveIpAbnormalRecord(userId, ipAddress);
        }
    }

    @Override
    public String getIpLocation(String ipAddress) {
        // TODO: 集成实际的IP地理位置服务
        return "未知位置";
    }
} 