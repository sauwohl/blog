package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class RedissonConfig {
    @Bean
    @Primary
    public RedissonClient redissonClient(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379"); // 有密码也可以在此处设置
        return Redisson.create(config);
    }
//    @Bean
//    public RedissonClient redissonClient2(){
//        // 配置
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://127.0.0.1:6380"); // 有密码也可以在此处设置
//        return Redisson.create(config);
//    }
//    @Bean
//    public RedissonClient redissonClien3(){
//        // 配置
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://127.0.0.1:6381"); // 有密码也可以在此处设置
//        return Redisson.create(config);
//    }
}
