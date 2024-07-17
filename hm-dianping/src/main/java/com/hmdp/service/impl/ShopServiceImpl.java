package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
        String s = RedisConstants.CACHE_SHOP_KEY + id;
        // 从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(s);
        // 存在，直接返回
        if(StrUtil.isNotBlank(shopJson)){ // isNotBlank只有对String返回真
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        // 判断命中是否空值
        if(shopJson != null){
            return Result.fail("店铺不存在");
        }

        // 不存在且shopJson为null ，查数据库
        Shop shop = getById(id);
        // 数据库中不存在
        if(shop == null){
            // 用 缓存空值 解决 缓存穿透
            stringRedisTemplate.opsForValue().set(s,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        // 数据库中存在，写入redis;设置有效时间，以保证缓存一致性
        stringRedisTemplate.opsForValue().set(s,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
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
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
