package com.blog.interceptor;

import com.blog.service.AbnormalRecordService;
import com.blog.service.UserService;
import com.blog.utils.IpUtil;
import com.blog.dto.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Slf4j
@Component
public class IpCheckInterceptor implements HandlerInterceptor {

    @Autowired
    private AbnormalRecordService abnormalRecordService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
        
        // 检查管理员权限
        if (uri.startsWith("/admin")) {
            if (account == null) {
                response.setStatus(401);
                writeErrorResponse(response, 401, "请先登录");
                return false;
            }
            // 检查是否是管理员
            if (!userService.isAdmin(account)) {
                response.setStatus(403);
                writeErrorResponse(response, 403, "权限不足，需要管理员权限");
                return false;
            }
            // 管理员操作必须记录IP
            abnormalRecordService.checkAndRecordIpAbnormal(account, ipAddress);
            return true;
        }
        
        // 其他关键操作的IP记录
        if (account != null && !ipAddress.isEmpty() && isKeyOperation(uri)) {
            abnormalRecordService.checkAndRecordIpAbnormal(account, ipAddress);
        }
        
        return true;
    }
    
    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        Result result = Result.fail(message);
        result.setCode(status);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(objectMapper.writeValueAsString(result));
        }
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
            || uri.contains("/comment/add");  // 评论
    }
} 