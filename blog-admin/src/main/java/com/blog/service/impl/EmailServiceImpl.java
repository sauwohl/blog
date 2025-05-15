package com.blog.service.impl;

import com.blog.service.EmailService;
import com.blog.service.OneTimeLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private OneTimeLinkService oneTimeLinkService;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendAccountInfo(String toEmail, String account, String password) {
        // 创建一次性链接，30分钟有效
        String content = String.format("账号：%s\n密码：%s", account, password);
        String token = oneTimeLinkService.createLink(content, 30);
        String link = "http://localhost:8081/link/" + token;
        
        // 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("账号信息");
        message.setText(String.format(
            "您好，\n\n" +
            "您的账号信息已准备就绪，请点击以下链接查看（链接仅能访问一次且30分钟内有效）：\n\n" +
            "%s\n\n" +
            "请妥善保管您的账号信息，建议首次登录后修改密码。\n\n" +
            "此邮件为系统自动发送，请勿回复。",
            link
        ));
        
        mailSender.send(message);
    }
} 