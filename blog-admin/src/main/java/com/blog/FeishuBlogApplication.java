package com.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.blog.mapper")
@SpringBootApplication
public class FeishuBlogApplication {

    public static void main(String[] args) {

        SpringApplication.run(FeishuBlogApplication.class, args);
    }

}
