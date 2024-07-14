package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.UserDTO;
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
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        // 校验验证码
        Object cacheCode = session.getAttribute("code"); // 后端的
        String code = loginForm.getCode(); // 前端的
        //System.out.println(code + " " + cacheCode.toString());
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            // 不一致，报错
            return Result.fail("验证码错误");
        }

        // 一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 不存在，创建新用户
        if (user == null) {
            createUsrWithphone(phone);
        }
        // 保存用户到session
        // 为了隐藏用户敏感信息，转为userDTO存session
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        // System.out.println(session.getAttribute("user").toString());
        return Result.ok();
    }

    private  User createUsrWithphone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
    
}
