package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.utils.RegexUtils;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        // 符合，生成验证码 
        String code = RandomUtil.randomNumbers(6);

        // 保存验证码到session
        //session.setAttribute("code",code);

        // 保存验证码到redis + 设置业务前缀以区分 + 设置验证码有效期
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 发送验证码(模拟)
        log.debug("验证码:{}",code);
        // 返回
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        //  从redis获取验证码 并 校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone); // 后端的
        String code = loginForm.getCode(); // 前端的
        //System.out.println(code + " " + cacheCode.toString());
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            return Result.fail("验证码错误");
        }

        // 一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 不存在，创建新用户
        if (user == null) {
            createUsrWithphone(phone);
        }
        // 保存用户到redis
        // 生成随机token作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 将User转为Hash存储,为了隐藏用户敏感信息，用userDTO存
        // 此处需要存string类型，所以利用了BeaUtil的方法，也可以手动自定义Map将各个字段转成String存
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,filedValue)->filedValue.toString()));

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
        //设置有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);

        // 保存用户到session
        //session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        // System.out.println(session.getAttribute("user").toString());

        // 返回token
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        // 签到
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 写入redis SETBIT key offset 1
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        // bitMap由String实现
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth-1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        // 获取本月签到记录
        // 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();
        // 写入redis SETBIT key offset 1
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        // 获取记录并遍历（此时返回的是十进制数字 BITFIELD
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if(result == null || result.isEmpty()){
            // 无签到记录
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == 0 || num == null){
            return Result.ok(0);
        }
        int count = 0;
        while (true){
            if((num & 1) == 0){
                // =0 说明未签到
                break;
            }else{
                // !=0 说明这一位签到
                count++;
            }
            // >>>是无符号右移
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private  User createUsrWithphone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
    
}
