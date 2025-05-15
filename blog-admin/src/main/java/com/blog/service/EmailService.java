package com.blog.service;

public interface EmailService {
    /**
     * 发送账号信息邮件
     *
     * @param toEmail 收件人邮箱
     * @param account 账号
     * @param password 密码
     */
    void sendAccountInfo(String toEmail, String account, String password);
} 