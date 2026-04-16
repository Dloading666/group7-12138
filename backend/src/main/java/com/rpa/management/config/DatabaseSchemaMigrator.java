package com.rpa.management.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Keeps deployed MySQL schemas aligned with fields that can store large crawl payloads.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DatabaseSchemaMigrator implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        if (!isMySql()) {
            log.info("Skipping MySQL schema migration for non-MySQL datasource");
            return;
        }

        jdbcTemplate.execute("""
                ALTER TABLE sys_user
                    MODIFY COLUMN role VARCHAR(20) NOT NULL
                """);
        jdbcTemplate.execute("""
                ALTER TABLE sys_task
                    MODIFY COLUMN params LONGTEXT,
                    MODIFY COLUMN description LONGTEXT,
                    MODIFY COLUMN result LONGTEXT,
                    MODIFY COLUMN error_message LONGTEXT
                """);
        jdbcTemplate.execute("""
                ALTER TABLE sys_crawl_result
                    MODIFY COLUMN summary_text LONGTEXT,
                    MODIFY COLUMN raw_html LONGTEXT,
                    MODIFY COLUMN structured_data LONGTEXT,
                    MODIFY COLUMN error_message LONGTEXT
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_ai_analysis_message (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    analysis_task_id BIGINT NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    content LONGTEXT NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_ai_analysis_task_id (analysis_task_id),
                    INDEX idx_ai_analysis_create_time (create_time)
                )
                """);
        log.info("Ensured sys_user.role supports custom roles and large text columns use LONGTEXT");
    }

    private boolean isMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("mysql");
        }
    }
}
