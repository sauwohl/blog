package com.blog.service.impl;

import com.blog.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendAccountInfo(String to, String account, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("账号信息");
        message.setText(String.format(
            "您好，\n\n" +
            "您的账号信息如下：\n" +
            "账号：%s\n" +
            "密码：%s\n\n" +
            "请妥善保管您的账号信息，建议首次登录后修改密码。\n\n" +
            "此邮件为系统自动发送，请勿回复。",
            account, password
        ));
        
        mailSender.send(message);
    }
} 