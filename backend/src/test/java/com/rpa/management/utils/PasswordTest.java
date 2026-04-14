package com.rpa.management.utils;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码测试类
 */
public class PasswordTest {
    
    @Test
    public void testGeneratePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String rawPassword = "admin123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("原始密码: " + rawPassword);
        System.out.println("加密密码: " + encodedPassword);
        System.out.println("验证结果: " + encoder.matches(rawPassword, encodedPassword));
    }
    
    @Test
    public void testGenerateMultiple() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String[] passwords = {"admin123", "user123", "123456"};
        
        for (String password : passwords) {
            System.out.println("密码: " + password);
            System.out.println("加密: " + encoder.encode(password));
            System.out.println();
        }
    }
}
