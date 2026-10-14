package com.course.sims.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码加密工具类。
 * <p>课程设计为降低部署复杂度，采用 MD5 摘要算法对密码做不可逆加密存储，
 * 用户表中只保存 32 位十六进制密文，避免明文密码泄露。</p>
 *
 * @author 课程设计第 2 小组 - 成员 A（系统架构 / 公共模块）
 * @version 1.0.0
 */
public final class MD5Util {

    private MD5Util() {
    }

    /**
     * 计算字符串的 MD5 摘要（小写十六进制）。
     *
     * @param text 明文
     * @return 32 位小写十六进制密文；入参为 null 时返回 null
     */
    public static String encrypt(String text) {
        if (text == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 必然支持 MD5，此处仅为满足编译要求
            throw new IllegalStateException("当前 JVM 不支持 MD5 算法", e);
        }
    }

    /**
     * 校验明文密码与密文是否匹配。
     *
     * @param plainText  明文密码
     * @param cipherText 数据库中保存的密文
     * @return 匹配返回 true
     */
    public static boolean matches(String plainText, String cipherText) {
        if (plainText == null || cipherText == null) {
            return false;
        }
        return cipherText.equalsIgnoreCase(encrypt(plainText));
    }
}
