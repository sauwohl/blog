package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = queryWithLogicalExpire(id);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

    private static final ExecutorService CACHE_REBUID_EXECUTOR = Executors.newFixedThreadPool(10);

    public Shop queryWithLogicalExpire(Long id){
        // 逻辑过期解决缓存穿透问题
        String s = CACHE_SHOP_KEY + id;
        // 从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(s);
        // 未命中(说明不是热点key)，直接返回
        if(StrUtil.isBlank(shopJson)){ // isNotBlank只有对String返回真
            return null;
        }
        // 命中，判断过期时间
        RedisData redisData = JSONUtil.toBean(shopJson,RedisData.class);
        // 取出的类型是JsonObject
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(),Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        if(expireTime.isAfter(LocalDateTime.now())){
            // 未过期，返回店铺信息
            return shop;
        }

        // 过期，缓存重建
        // 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 获取成功，开启独立线程重建
        if(isLock){
            CACHE_REBUID_EXECUTOR.submit(()->{
                try {
                    this.saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                finally {
                    unLock(lockKey);    // unlock要在finally里
                }
            });
        }
        // 返回商铺信息（获取失败返回过期信息；成功返回重建信息）
        return shop;
    }

    public Shop queryWithMutex(Long id){
        // 用互斥锁解决缓存击穿问题
        String s = CACHE_SHOP_KEY + id;
        // 从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(s);
        // 存在，直接返回
        if(StrUtil.isNotBlank(shopJson)){ // isNotBlank只有对String返回真
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中是否空值
        if(shopJson != null){
            return null;
        }

        // 未命中，开始缓存重建
        // 获取互斥锁
        String lockkey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockkey);
            // 获取失败，则休眠并重试
            if(!isLock){
                Thread.sleep(50);
                queryWithPassThrough(id);
            }
            // 获取成功
            shop = getById(id);
            // 数据库中不存在
            if(shop == null){
                // 用 缓存空值 解决 缓存穿透
                stringRedisTemplate.opsForValue().set(s,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 数据库中存在，写入redis;设置有效时间，以保证缓存一致性
            stringRedisTemplate.opsForValue().set(s,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 释放互斥锁
            unLock(lockkey);
        }
        return shop;
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

    public void saveShop2Redis(Long id, Long expireSeconds){
        // 查询店铺
        Shop shop = getById(id);
        // 封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(redisData));
    }


    @Override
    @Transactional // 由于这里不是分布式系统，用事务限制原子性就够了
    public Result update(Shop shop) {
        if(shop.getId() == null){
            return Result.fail("店铺id不为空");
        }
        // ①更新数据库; ①②顺序有意义，因为缓存读写速度快，这个顺序发生错误概率更小
        updateById(shop);
        // ②删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    public Shop queryWithPassThrough(Long id){
        // 针对缓存穿透问题
        String s = CACHE_SHOP_KEY + id;
        // 从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(s);
        // 存在，直接返回
        if(StrUtil.isNotBlank(shopJson)){ // isNotBlank只有对String返回真
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中是否空值
        if(shopJson != null){
            return null;
        }

        // 不存在且shopJson为null ，查数据库
        Shop shop = getById(id);
        // 数据库中不存在
        if(shop == null){
            // 用 缓存空值 解决 缓存穿透
            stringRedisTemplate.opsForValue().set(s,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 数据库中存在，写入redis;设置有效时间，以保证缓存一致性
        stringRedisTemplate.opsForValue().set(s,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }
}
