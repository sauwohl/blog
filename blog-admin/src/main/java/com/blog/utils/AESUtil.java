package com.blog.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AESUtil {
    
    @Value("${aes.key:your-secret-key-16}")  // 16位密钥，可以在配置文件中配置
    private String key;

    /**
     * AES解密
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的数据
     */
    public String decrypt(String encryptedData) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * AES加密（用于测试）
     * @param data 要加密的数据
     * @return Base64编码的加密数据
     */
    public String encrypt(String data) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }
} 