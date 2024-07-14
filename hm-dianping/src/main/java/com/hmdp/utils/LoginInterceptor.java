package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// 拦截器
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    // preHandle 即 原始方法调用前执行的内容
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取session，获取其中用户
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        // 用户不存在，拦截
        if(user==null){
            response.setStatus(401);
            return false;
        }
        // 存在，保存用户信息到ThreadLocal
        UserHolder.saveUser((UserDTO)user);
        // 放行
        return true;
    }

    @Override
    //afterCompletion 即 原始方法调用完成后执行的内容
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户 避免内存泄漏
        UserHolder.removeUser();
    }

    //postHandle 即 原始方法调用后执行的内容
    //public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception{}
}
