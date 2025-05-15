package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.entity.OneTimeLink;
import com.blog.mapper.OneTimeLinkMapper;
import com.blog.service.OneTimeLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OneTimeLinkServiceImpl implements OneTimeLinkService {

    @Autowired
    private OneTimeLinkMapper oneTimeLinkMapper;

    @Override
    public String createLink(String content, int expiryMinutes) {
        String token = UUID.randomUUID().toString().replace("-", "");
        
        OneTimeLink link = new OneTimeLink();
        link.setToken(token);
        link.setContent(content);
        link.setExpiryTime(LocalDateTime.now().plusMinutes(expiryMinutes));
        link.setUsed(false);
        link.setCreateTime(LocalDateTime.now());
        link.setUpdateTime(LocalDateTime.now());
        
        oneTimeLinkMapper.insert(link);
        
        return token;
    }

    @Override
    @Transactional
    public String useLink(String token) {
        QueryWrapper<OneTimeLink> wrapper = new QueryWrapper<>();
        wrapper.eq("token", token)
               .eq("used", false)
               .gt("expiry_time", LocalDateTime.now());
        
        OneTimeLink link = oneTimeLinkMapper.selectOne(wrapper);
        
        if (link == null) {
            return null;
        }
        
        // 标记为已使用
        link.setUsed(true);
        link.setUpdateTime(LocalDateTime.now());
        oneTimeLinkMapper.updateById(link);
        
        return link.getContent();
    }
} 