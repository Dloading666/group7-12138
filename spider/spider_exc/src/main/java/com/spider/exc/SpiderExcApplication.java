package com.spider.exc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 爬虫执行应用启动类
 */
@SpringBootApplication
@MapperScan("com.spider.exc.domain.mapper")
@EnableAsync
public class SpiderExcApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpiderExcApplication.class, args);
    }
}
