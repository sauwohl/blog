package com.blog.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 拦截器
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    // preHandle 即 原始方法调用前执行的内容
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断是否需要拦截（通过ThreadLocal）
        if(UserHolder.getUser() == null){
            response.setStatus(401);
            return false;
        }
        // 放行
        return true;
    }
}
