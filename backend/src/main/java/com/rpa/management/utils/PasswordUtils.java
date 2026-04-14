package com.rpa.management.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类
 * 用于生成BCrypt加密密码
 */
public class PasswordUtils {
    
    /**
     * 生成BCrypt加密密码
     * 使用方法：运行此类的main方法
     */
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("========== BCrypt 密码生成工具 ==========\n");
        
        // 生成常用密码
        String[] passwords = {"admin123", "user123", "123456", "password"};
        
        for (String password : passwords) {
            String encodedPassword = encoder.encode(password);
            System.out.println("原始密码: " + password);
            System.out.println("加密密码: " + encodedPassword);
            System.out.println("验证结果: " + encoder.matches(password, encodedPassword));
            System.out.println();
        }
        
        System.out.println("==========================================");
        System.out.println("提示：将加密密码复制到数据库中使用");
    }
    
    /**
     * 加密密码
     */
    public static String encode(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }
    
    /**
     * 验证密码
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(rawPassword, encodedPassword);
    }
}
