package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP = 1704067200;
    private static final long COUNT_BITS = 32; // 序列号位数

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){
        //实现全局唯一id
        // 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowsecond = now.toEpochSecond(UTC);
        long timestamp = nowsecond - BEGIN_TIMESTAMP;
        // 生成序列号(拼接当天日期)
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        // 拼接
        return timestamp << COUNT_BITS | count;
    }

    public static void main(String[] args){
        LocalDateTime time = LocalDateTime.of(2024, 1, 1, 0, 0);
        long second = time.toEpochSecond(UTC); // 转成具体的秒
        System.out.println(second);
    }
}
