package com.blog.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.PostConstruct;

@Component
public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int KEY_SIZE = 128;
    private static final Cipher cipher;

    static {
        try {
            cipher = Cipher.getInstance(TRANSFORMATION);
        } catch (Exception e) {
            throw new RuntimeException("初始化AES Cipher失败", e);
        }
    }

    @Value("${aes.key}")
    private String key;

    /**
     * AES加密
     * @param data 要加密的数据
     * @return Base64编码的加密数据
     */
    public String encrypt(String data) {
        try {
            if (data == null) {
                throw new IllegalArgumentException("待加密数据不能为null");
            }

            // 初始化密钥
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 加密
            synchronized (cipher) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(encryptedBytes);
            }
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES解密
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的数据
     */
    public String decrypt(String encryptedData) {
        try {
            if (encryptedData == null) {
                throw new IllegalArgumentException("加密数据不能为null");
            }

            byte[] encrypted = Base64.getDecoder().decode(encryptedData);

            // 初始化密钥
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            // 解密
            synchronized (cipher) {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] decryptedBytes = cipher.doFinal(encrypted);
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }
}