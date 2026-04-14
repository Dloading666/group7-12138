package com.rpa.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 管理系统启动类
 */
@SpringBootApplication
public class ManagementSystemApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ManagementSystemApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("管理系统启动成功！");
        System.out.println("API文档地址: http://localhost:8080/api/swagger-ui/index.html");
        System.out.println("========================================\n");
    }
}
