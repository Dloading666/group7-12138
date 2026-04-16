-- Docker 启动时自动执行：创建数据库并初始化表结构
-- management_system: Spring Boot 管理后端使用
-- spider_db: Spider Java 执行器使用

CREATE DATABASE IF NOT EXISTS management_system
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS spider_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 授权（root 已有全部权限，这里为了明确性保留）
GRANT ALL PRIVILEGES ON management_system.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON spider_db.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- =============================================
-- 初始化 management_system 表结构
-- 使用 CREATE TABLE IF NOT EXISTS，不破坏已有数据
-- Hibernate ddl-auto:update 会在此基础上补全列
-- =============================================
USE management_system;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    avatar VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    last_login_ip VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(255),
    icon VARCHAR(50),
    sort INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress INT DEFAULT 0,
    robot_id BIGINT,
    robot_name VARCHAR(50),
    params LONGTEXT,
    description LONGTEXT,
    result LONGTEXT,
    error_message LONGTEXT,
    priority VARCHAR(20) DEFAULT 'medium',
    execute_type VARCHAR(20) DEFAULT 'immediate',
    scheduled_time DATETIME,
    user_id BIGINT,
    user_name VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    start_time DATETIME,
    end_time DATETIME,
    duration INT,
    tax_id VARCHAR(50),
    enterprise_name VARCHAR(200),
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_code VARCHAR(50),
    name VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'offline',
    description TEXT,
    ip_address VARCHAR(50),
    port INT,
    config TEXT,
    last_heartbeat DATETIME,
    task_count INT DEFAULT 0,
    total_tasks BIGINT DEFAULT 0,
    success_tasks BIGINT DEFAULT 0,
    failed_tasks BIGINT DEFAULT 0,
    success_rate DOUBLE DEFAULT 0.0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- sys_execution_log: 包含实体类中所有字段，避免 Hibernate ddl-auto:update 漏建列
CREATE TABLE IF NOT EXISTS sys_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    task_code VARCHAR(50),
    task_name VARCHAR(100),
    robot_id BIGINT,
    robot_name VARCHAR(50),
    level VARCHAR(20) NOT NULL DEFAULT 'INFO',
    message TEXT NOT NULL,
    stage VARCHAR(50),
    extra_data LONGTEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_level (level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(100),
    method VARCHAR(255),
    params TEXT,
    ip VARCHAR(50),
    status VARCHAR(20),
    error_msg TEXT,
    duration BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_crawl_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_record_id BIGINT,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    task_name VARCHAR(100),
    final_url VARCHAR(1000),
    title VARCHAR(500),
    summary_text LONGTEXT,
    raw_html LONGTEXT,
    structured_data LONGTEXT,
    total_count INT DEFAULT 0,
    crawled_pages INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending',
    error_message LONGTEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_ai_analysis_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    analysis_task_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_analysis_task_id (analysis_task_id),
    INDEX idx_ai_analysis_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_collect_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    task_id BIGINT,
    robot_id BIGINT,
    collect_type VARCHAR(20) NOT NULL DEFAULT 'web',
    target_url VARCHAR(500),
    request_method VARCHAR(10) DEFAULT 'GET',
    request_headers TEXT,
    request_params TEXT,
    request_body TEXT,
    collect_rules TEXT,
    field_mapping TEXT,
    data_clean_rules TEXT,
    page_config TEXT,
    cron_expression VARCHAR(100),
    is_enabled TINYINT(1) DEFAULT 1,
    timeout INT DEFAULT 30000,
    retry_count INT DEFAULT 3,
    proxy_config TEXT,
    output_config TEXT,
    spider_config TEXT,
    last_execute_time DATETIME,
    last_execute_status VARCHAR(20),
    total_count BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    fail_count BIGINT DEFAULT 0,
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_collect_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    task_id BIGINT,
    robot_id BIGINT,
    source_url VARCHAR(500),
    data_hash VARCHAR(64),
    data_content TEXT,
    raw_html MEDIUMTEXT,
    collect_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'valid',
    remark VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 节点类型表（流程设计器）
CREATE TABLE IF NOT EXISTS node_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(100),
    color VARCHAR(20),
    category VARCHAR(50),
    sort_order INT DEFAULT 0,
    enabled TINYINT(1) DEFAULT 1,
    default_config LONGTEXT,
    description VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 流程定义表
CREATE TABLE IF NOT EXISTS workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'draft',
    version INT DEFAULT 1,
    user_id BIGINT,
    user_name VARCHAR(100),
    publish_time DATETIME,
    config LONGTEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 流程节点表
CREATE TABLE IF NOT EXISTS workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    node_type_id BIGINT,
    node_type VARCHAR(50),
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    x INT,
    y INT,
    config TEXT,
    timeout INT DEFAULT 60,
    retry_count INT DEFAULT 3,
    `order` INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow_id (workflow_id),
    FOREIGN KEY (workflow_id) REFERENCES workflow(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
