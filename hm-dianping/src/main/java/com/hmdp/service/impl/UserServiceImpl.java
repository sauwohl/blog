package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.utils.RegexUtils;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;

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
        session.setAttribute("code",code);       

        // 发送验证码(模拟)
        log.debug("验证码:{}",code);
        // 返回
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        // 校验验证码
        // 不一致，报错
        // 一致，根据手机号查询用户
        // 不存在，创建新用户
        // 保存用户到session
        throw new UnsupportedOperationException("Unimplemented method 'login'");
    }
    
}
