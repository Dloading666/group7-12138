package com.rpa.management.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

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
        addColumnIfMissing("sys_task", "workflow_id", "BIGINT");
        addColumnIfMissing("sys_task", "workflow_version_id", "BIGINT");
        addColumnIfMissing("sys_task", "workflow_version", "INT");
        addColumnIfMissing("sys_task", "workflow_name", "VARCHAR(200)");
        addColumnIfMissing("sys_task", "workflow_category", "VARCHAR(100)");
        addColumnIfMissing("sys_task", "input_config", "LONGTEXT");
        addColumnIfMissing("sys_task", "schedule_config", "LONGTEXT");
        addColumnIfMissing("sys_task", "latest_run_id", "BIGINT");
        addColumnIfMissing("sys_task", "latest_run_status", "VARCHAR(20)");
        addColumnIfMissing("sys_task", "last_run_time", "DATETIME");
        addColumnIfMissing("sys_task", "next_run_time", "DATETIME");

        jdbcTemplate.execute("""
                ALTER TABLE sys_crawl_result
                    MODIFY COLUMN summary_text LONGTEXT,
                    MODIFY COLUMN raw_html LONGTEXT,
                    MODIFY COLUMN structured_data LONGTEXT,
                    MODIFY COLUMN error_message LONGTEXT
                """);
        addColumnIfMissing("sys_crawl_result", "task_run_id", "BIGINT");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_workflow_version (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    workflow_id BIGINT NOT NULL,
                    version_number INT NOT NULL,
                    workflow_code VARCHAR(100) NOT NULL,
                    name VARCHAR(200) NOT NULL,
                    description VARCHAR(1000),
                    category VARCHAR(100),
                    publish_status VARCHAR(20) DEFAULT 'published',
                    user_id BIGINT,
                    user_name VARCHAR(100),
                    publish_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    input_schema LONGTEXT,
                    graph LONGTEXT,
                    INDEX idx_workflow_version_workflow_id (workflow_id),
                    INDEX idx_workflow_version_status (publish_status)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_task_run (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    run_id VARCHAR(50) NOT NULL UNIQUE,
                    task_id BIGINT NOT NULL,
                    task_code VARCHAR(50),
                    task_name VARCHAR(100),
                    workflow_version_id BIGINT,
                    workflow_name VARCHAR(200),
                    workflow_category VARCHAR(100),
                    status VARCHAR(20) DEFAULT 'pending',
                    progress INT DEFAULT 0,
                    trigger_type VARCHAR(20) DEFAULT 'manual',
                    engine_run_id VARCHAR(100),
                    input_config LONGTEXT,
                    workflow_snapshot LONGTEXT,
                    result LONGTEXT,
                    error_message LONGTEXT,
                    start_time DATETIME,
                    end_time DATETIME,
                    duration INT DEFAULT 0,
                    user_id BIGINT,
                    user_name VARCHAR(50),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_task_run_task_id (task_id),
                    INDEX idx_task_run_status (status),
                    INDEX idx_task_run_engine_run_id (engine_run_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_workflow_debug_run (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    run_id VARCHAR(50) NOT NULL UNIQUE,
                    workflow_id BIGINT NOT NULL,
                    workflow_code VARCHAR(100),
                    workflow_name VARCHAR(200),
                    status VARCHAR(20) DEFAULT 'pending',
                    progress INT DEFAULT 0,
                    input_config LONGTEXT,
                    graph_snapshot LONGTEXT,
                    result LONGTEXT,
                    error_message LONGTEXT,
                    start_time DATETIME,
                    end_time DATETIME,
                    duration INT DEFAULT 0,
                    user_id BIGINT,
                    user_name VARCHAR(50),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_workflow_debug_run_workflow_id (workflow_id),
                    INDEX idx_workflow_debug_run_status (status)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_workflow_step_run (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    step_run_id VARCHAR(50) NOT NULL UNIQUE,
                    task_run_id BIGINT,
                    debug_run_id BIGINT,
                    node_id VARCHAR(100) NOT NULL,
                    node_type VARCHAR(100) NOT NULL,
                    node_label VARCHAR(200),
                    branch_key VARCHAR(100),
                    engine_task_id VARCHAR(100),
                    status VARCHAR(20) DEFAULT 'pending',
                    input_snapshot LONGTEXT,
                    output_snapshot LONGTEXT,
                    error_message LONGTEXT,
                    start_time DATETIME,
                    end_time DATETIME,
                    duration INT DEFAULT 0,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_workflow_step_run_task_run_id (task_run_id),
                    INDEX idx_workflow_step_run_debug_run_id (debug_run_id),
                    INDEX idx_workflow_step_run_status (status)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_ai_analysis_message (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    analysis_task_id BIGINT NULL,
                    task_run_id BIGINT NULL,
                    role VARCHAR(20) NOT NULL,
                    content LONGTEXT NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_ai_analysis_task_id (analysis_task_id),
                    INDEX idx_ai_analysis_task_run_id (task_run_id),
                    INDEX idx_ai_analysis_create_time (create_time)
                )
                """);
        addColumnIfMissing("sys_execution_log", "task_run_id", "BIGINT");

        jdbcTemplate.execute("""
                ALTER TABLE workflow
                    MODIFY COLUMN config LONGTEXT
                """);
        addColumnIfMissing("workflow", "category", "VARCHAR(100)");
        addColumnIfMissing("workflow", "latest_version_id", "BIGINT");
        addColumnIfMissing("workflow", "input_schema", "LONGTEXT");
        addColumnIfMissing("workflow", "graph", "LONGTEXT");

        log.info("Database schema migration completed");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean isMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("mysql");
        }
    }
}
