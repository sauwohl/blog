package com.blog.config;

import com.blog.interceptor.TokenInterceptor;
import com.blog.interceptor.IpCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;
    
    @Autowired
    private IpCheckInterceptor ipCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Token拦截器
        registry.addInterceptor(tokenInterceptor)
               .addPathPatterns("/**")
               .excludePathPatterns(
                   "/user/login",
                   "/user/register",
                       "/link"
               );
               
        // IP检查拦截器
        registry.addInterceptor(ipCheckInterceptor)
               .addPathPatterns("/**");
    }
} 