package com.blog.service;

import com.blog.entity.OneTimeLink;

public interface OneTimeLinkService {
    
    /**
     * 创建一次性链接
     * @param content 需要加密的内容
     * @param expiryMinutes 过期时间（分钟）
     * @return 生成的token
     */
    String createLink(String content, int expiryMinutes);
    
    /**
     * 获取并使用一次性链接
     * @param token 链接token
     * @return 链接内容，如果链接无效则返回null
     */
    String useLink(String token);
} 