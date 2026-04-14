package com.rpa.management.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时打印加密密码
 */
@Component
public class PasswordPrinter implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("\n========== 加密密码列表 ==========");
        
        // 常用密码加密
        String[][] passwords = {
            {"admin123", "管理员默认密码"},
            {"user123", "普通用户默认密码"},
            {"123456", "简单密码"},
            {"password", "测试密码"}
        };
        
        for (String[] item : passwords) {
            String rawPassword = item[0];
            String desc = item[1];
            String encodedPassword = encoder.encode(rawPassword);
            
            System.out.println("\n" + desc + ": " + rawPassword);
            System.out.println("加密密码: " + encodedPassword);
        }
        
        System.out.println("\n===================================\n");
    }
}
