package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        // 查询type缓存
        List<String> typeJson = stringRedisTemplate.opsForList().range(key,0,-1);
        // redis存在
        if(!typeJson.isEmpty()){
            List<ShopType> typeList = new ArrayList<>();
            for(String type : typeJson){
                typeList.add(JSONUtil.toBean(type,ShopType.class));
            }
            return Result.ok(typeList);
        }
        // redis不存在
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //数据库不存在
        if (typeList.isEmpty())
            return Result.fail("店铺类型不存在");
        // 数据库存在
        for(ShopType type : typeList){
            typeJson.add(JSONUtil.toJsonStr(type));
        }
        // leftPushAll插入的类型可以是List，leftPush插入String这样的单个元素
        stringRedisTemplate.opsForList().leftPushAll(key, typeJson);
        return Result.ok(typeList);
    }
}
