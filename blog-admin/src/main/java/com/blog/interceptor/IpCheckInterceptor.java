package com.blog.interceptor;

import com.blog.service.AbnormalRecordService;
import com.blog.utils.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class IpCheckInterceptor implements HandlerInterceptor {

    @Autowired
    private AbnormalRecordService abnormalRecordService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取请求路径
        String uri = request.getRequestURI();
        
        // 跳过静态资源和swagger等请求
        if (isStaticResource(uri) || isSwaggerRequest(uri)) {
            return true;
        }
        
        // 获取当前请求的IP
        String ipAddress = IpUtil.getIpAddr(request);
        
        // 获取当前用户账号（从token中）
        String account = (String) request.getAttribute("account");
        
        // 只记录已登录用户的关键操作
        if (account != null && !ipAddress.isEmpty() && isKeyOperation(uri)) {
            abnormalRecordService.checkAndRecordIpAbnormal(account, ipAddress);
        }
        
        return true;
    }
    
    /**
     * 判断是否为静态资源
     */
    private boolean isStaticResource(String uri) {
        return uri.contains("/static/") 
            || uri.endsWith(".js")
            || uri.endsWith(".css")
            || uri.endsWith(".jpg")
            || uri.endsWith(".png")
            || uri.endsWith(".ico");
    }
    
    /**
     * 判断是否为Swagger请求
     */
    private boolean isSwaggerRequest(String uri) {
        return uri.contains("/swagger-")
            || uri.contains("/v2/api-docs")
            || uri.contains("/webjars/");
    }
    
    /**
     * 判断是否为需要记录IP的关键操作
     */
    private boolean isKeyOperation(String uri) {
        return uri.contains("/user/login")  // 登录
            || uri.contains("/user/register")  // 注册
            || uri.contains("/article/publish")  // 发布文章
            || uri.contains("/article/edit")  // 编辑文章
            || uri.contains("/admin/")  // 管理员操作
            || uri.contains("/comment/add");  // 评论
    }
} 