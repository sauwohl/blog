package com.blog.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.blog.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 拦截器
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    // 此处不是spring控制，所以不能用注解@Resource注入，需要手动注入
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    // preHandle 即 原始方法调用前执行的内容
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的token，获取redis中用户
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            return true;
        }

        String key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object,Object> userMap = stringRedisTemplate.opsForHash().entries(key);
//        HttpSession session = request.getSession();
//        Object user = session.getAttribute("user");
        // 用户不存在，拦截
        if(userMap.isEmpty()){
            return true;
        }

        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);

        // 存在，保存用户信息到ThreadLocal
        UserHolder.saveUser(userDTO);

        // 刷新token有效期（因为toke逻辑应该是30min未访问就删除，正适合在拦截器处理）
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
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
