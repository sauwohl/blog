package com.blog.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.blog.utils.RedisConstants.LOCK_SHOP_KEY;

@Component
@Slf4j
public class CacheClient {
    // 缓存工具封装(主要针对任意object做序列化/反序列化)
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, Long time, TimeUnit unit){
        //函数式编程：Function<参数,返回值>可以作为参数传逻辑，在此处传递对应对象的数据库操作

        // 针对缓存穿透问题
        String key = keyPrefix + id;
        // 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 存在，直接返回
        if(StrUtil.isNotBlank(json)){ // isNotBlank只有对String返回真
            return JSONUtil.toBean(json, type);
        }
        // 判断命中是否空值
        if(json != null){
            return null;
        }

        // 不存在且shopJson为null ，查数据库
        R r = dbFallback.apply(id); // apply返回function的返回值
        // 数据库中不存在
        if(r == null){
            // 用 缓存空字符串 解决 缓存穿透
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 数据库中存在，写入redis;设置有效时间，以保证缓存一致性
        this.set(key, r, time, unit);
        return r;
    }

    private static final ExecutorService CACHE_REBUID_EXECUTOR = Executors.newFixedThreadPool(10);

    public <R,ID> R queryWithLogicalExpire(
            String KeyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, Long time, TimeUnit unit) {
        // 逻辑过期解决缓存穿透问题
        String key = KeyPrefix + id;
        // 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 未命中(说明不是热点key)，直接返回
        if (StrUtil.isBlank(json)) { // isNotBlank只有对String返回真
            return null;
        }
        // 命中，判断过期时间
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // 取出的类型是JsonObject
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，返回店铺信息
            return r;
        }

        // 过期，缓存重建
        // 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 获取成功，开启独立线程重建
        if (isLock) {
            CACHE_REBUID_EXECUTOR.submit(() -> {
                try {
                    R r1 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);    // unlock要在finally里
                }
            });
        }
        // 获取失败返回过期信息(r1重建好等下一次查询返回就行了，不用在此返回）
        return r;
    }
    private boolean tryLock(String key){
        // 获取锁
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10, TimeUnit.SECONDS); // 即SETNX
        return BooleanUtil.isTrue(flag); // Boolean是对象，编译器拆箱可能有空指针异常
    }

    private void unLock(String key) {
        // 释放锁
        stringRedisTemplate.delete(key);
    }
}
