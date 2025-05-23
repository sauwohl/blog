//package com.blog;
//
//import com.blog.utils.AESUtil;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.beans.factory.annotation.Value;
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//public class code {
//
//    @Autowired
//    private AESUtil aesUtil;
//
//    @Value("${aes.key}")
//    private String aesKey;
//
//    @Test
//    public void testEncryptAndDecrypt() {
//        // 测试普通字符串
//        String originalText = "G&P!E!DsYd";
//        String encrypted = aesUtil.encrypt(originalText);
//        String decrypted = aesUtil.decrypt(encrypted);
//
//        System.out.println("密钥: " + aesKey);
//        System.out.println("原文: " + originalText);
//        System.out.println("加密后: " + encrypted);
//        System.out.println("解密后: " + decrypted);
//
//        assertEquals(originalText, decrypted, "加密解密后的文本应该与原文相同");
//
//        // 测试中文字符串
//        String chineseText = "你好，世界！123";
//        String encryptedChinese = aesUtil.encrypt(chineseText);
//        String decryptedChinese = aesUtil.decrypt(encryptedChinese);
//
//        System.out.println("\n原文: " + chineseText);
//        System.out.println("加密后: " + encryptedChinese);
//        System.out.println("解密后: " + decryptedChinese);
//
//        assertEquals(chineseText, decryptedChinese, "中文加密解密后的文本应该与原文相同");
//    }
//
//    @Test
//    public void testEmptyAndNull() {
//        // 测试空字符串
//        String emptyString = "";
//        String encryptedEmpty = aesUtil.encrypt(emptyString);
//        String decryptedEmpty = aesUtil.decrypt(encryptedEmpty);
//
//        assertEquals(emptyString, decryptedEmpty, "空字符串加密解密后应该仍然为空");
//
//        // 测试 null
//        assertThrows(IllegalArgumentException.class, () -> {
//            aesUtil.encrypt(null);
//        }, "加密 null 应该抛出异常");
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            aesUtil.decrypt(null);
//        }, "解密 null 应该抛出异常");
//    }
//
//    @Test
//    public void testLongText() {
//        // 测试长文本
//        StringBuilder longText = new StringBuilder();
//        for (int i = 0; i < 1000; i++) {
//            longText.append("测试长文本-").append(i).append(" ");
//        }
//
//        String originalLongText = longText.toString();
//        String encryptedLong = aesUtil.encrypt(originalLongText);
//        String decryptedLong = aesUtil.decrypt(encryptedLong);
//
//        System.out.println("\n长文本加密后长度: " + encryptedLong.length());
//        assertEquals(originalLongText, decryptedLong, "长文本加密解密后应该与原文相同");
//    }
//
//    @Test
//    public void testInvalidInput() {
//        // 测试无效的Base64编码
//        assertThrows(RuntimeException.class, () -> {
//            aesUtil.decrypt("invalid-base64-data");
//        }, "解密非Base64编码的数据应该抛出异常");
//    }
//
//    @Test
//    public void testKeySize() {
//        // 验证密钥长度
//        assertEquals(16, aesKey.length(), "AES密钥长度应该是16字节（128位）");
//
//        // 测试加密结果是否为16字节的倍数
//        String testText = "test123";
//        String encrypted = aesUtil.encrypt(testText);
//        byte[] encryptedBytes = java.util.Base64.getDecoder().decode(encrypted);
//        assertEquals(0, encryptedBytes.length % 16, "加密结果长度应该是16字节的倍数");
//    }
//}