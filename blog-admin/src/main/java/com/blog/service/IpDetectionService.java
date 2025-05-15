package com.blog.service;

import com.blog.entity.AccountAbnormalRecord;

public interface IpDetectionService {
    
    /**
     * 检查IP是否异常
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @return 如果IP异常，返回异常记录；否则返回null
     */
    AccountAbnormalRecord checkIpAbnormal(Long userId, String ipAddress);
    
    /**
     * 记录用户IP
     * @param userId 用户ID
     * @param ipAddress IP地址
     */
    void recordUserIp(Long userId, String ipAddress);
    
    /**
     * 将IP标记为常用地址
     * @param userId 用户ID
     * @param ipAddress IP地址
     */
    void markIpAsCommon(Long userId, String ipAddress);
    
    /**
     * 获取IP地理位置
     * @param ipAddress IP地址
     * @return 地理位置描述
     */
    String getIpLocation(String ipAddress);
} 