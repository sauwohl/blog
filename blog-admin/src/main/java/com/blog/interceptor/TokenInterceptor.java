package com.blog.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.entity.Token;
import com.blog.mapper.TokenMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenMapper tokenMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果是OPTIONS请求，放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "未提供token");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }
        
        // 查询token是否有效
        QueryWrapper<Token> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("token", token);
        Token tokenEntity = tokenMapper.selectOne(queryWrapper);
        
        if (tokenEntity == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "token无效");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }
        
        // 将用户账号添加到请求属性中，供后续使用
        request.setAttribute("account", tokenEntity.getAccount());
        return true;
    }
} 