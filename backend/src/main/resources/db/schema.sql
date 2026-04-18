-- =============================================
-- 管理系统数据库表结构
-- 数据库：MySQL 8.0+ / H2
-- 字符集：utf8mb4
-- =============================================

-- 创建数据库（仅MySQL）
-- CREATE DATABASE IF NOT EXISTS management_system 
-- DEFAULT CHARACTER SET utf8mb4 
-- COLLATE utf8mb4_unicode_ci;
-- 
-- USE management_system;

-- =============================================
-- 1. 用户表
-- =============================================
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN-管理员，USER-普通用户',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
    avatar VARCHAR(255) COMMENT '头像URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 2. 角色表
-- =============================================
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 3. 权限表
-- =============================================
DROP TABLE IF EXISTS sys_permission;

CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) NOT NULL COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    path VARCHAR(255) COMMENT '路径',
    icon VARCHAR(50) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_code (code),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- =============================================
-- 4. 角色权限关联表
-- =============================================
DROP TABLE IF EXISTS sys_role_permission;

CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =============================================
-- 5. 任务表
-- =============================================
DROP TABLE IF EXISTS sys_task;

CREATE TABLE sys_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    type VARCHAR(50) NOT NULL COMMENT '任务类型',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-等待，running-执行中，completed-完成，failed-失败',
    progress INT DEFAULT 0 COMMENT '进度(0-100)',
    robot_id BIGINT COMMENT '执行机器人ID',
    robot_name VARCHAR(50) COMMENT '执行机器人名称',
    params LONGTEXT COMMENT '任务参数(JSON)',
    description LONGTEXT COMMENT '任务描述',
    result LONGTEXT COMMENT '执行结果',
    error_message LONGTEXT COMMENT '错误信息',
    priority VARCHAR(20) DEFAULT 'medium' COMMENT '优先级：high-高，medium-中，low-低',
    execute_type VARCHAR(20) DEFAULT 'immediate' COMMENT '执行方式：immediate-立即，scheduled-定时',
    scheduled_time DATETIME COMMENT '定时执行时间',
    user_id BIGINT COMMENT '创建用户ID',
    user_name VARCHAR(50) COMMENT '创建用户名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration INT COMMENT '耗时(秒)',
    tax_id VARCHAR(50) COMMENT '税号',
    enterprise_name VARCHAR(200) COMMENT '企业名称',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- =============================================
-- 6. 抓取结果表
-- =============================================
DROP TABLE IF EXISTS sys_crawl_result;

CREATE TABLE sys_crawl_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '抓取结果ID',
    task_record_id BIGINT COMMENT '任务主表ID',
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    task_name VARCHAR(100) COMMENT '任务名称',
    final_url VARCHAR(1000) COMMENT '最终URL',
    title VARCHAR(500) COMMENT '页面标题',
    summary_text LONGTEXT COMMENT '正文摘要',
    raw_html LONGTEXT COMMENT '原始HTML',
    structured_data LONGTEXT COMMENT '结构化结果',
    total_count INT DEFAULT 0 COMMENT '结果条数',
    crawled_pages INT DEFAULT 0 COMMENT '抓取页数',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    error_message LONGTEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_crawl_result_task_record_id (task_record_id),
    INDEX idx_crawl_result_status (status),
    INDEX idx_crawl_result_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='真实网站抓取结果表';

-- =============================================
-- 7. 机器人表
-- =============================================
DROP TABLE IF EXISTS sys_robot;

CREATE TABLE sys_robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '机器人ID',
    name VARCHAR(50) NOT NULL COMMENT '机器人名称',
    type VARCHAR(50) NOT NULL COMMENT '机器人类型',
    status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态：online-在线，offline-离线',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    port INT COMMENT '端口',
    config TEXT COMMENT '配置信息(JSON)',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    task_count INT DEFAULT 0 COMMENT '执行任务数',
    success_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '成功率',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机器人表';

-- =============================================
-- 8. 执行日志表
-- =============================================
-- =============================================
-- 7.1 AI 分析问答消息表
-- =============================================
DROP TABLE IF EXISTS sys_ai_analysis_message;

CREATE TABLE sys_ai_analysis_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    analysis_task_id BIGINT NOT NULL COMMENT 'AI分析任务ID',
    role VARCHAR(20) NOT NULL COMMENT '角色:user/assistant',
    content LONGTEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_ai_analysis_task_id (analysis_task_id),
    INDEX idx_ai_analysis_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI分析问答消息表';

DROP TABLE IF EXISTS sys_execution_log;

CREATE TABLE sys_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    task_id BIGINT COMMENT '任务ID',
    task_code VARCHAR(50) COMMENT '任务编号',
    task_name VARCHAR(100) COMMENT '任务名称',
    robot_id BIGINT COMMENT '机器人ID',
    robot_name VARCHAR(50) COMMENT '机器人名称',
    level VARCHAR(20) NOT NULL DEFAULT 'INFO' COMMENT '日志级别：INFO, WARN, ERROR',
    message TEXT NOT NULL COMMENT '日志内容',
    stage VARCHAR(50) COMMENT '执行阶段: start, process, end, error',
    extra_data LONGTEXT COMMENT '额外数据（JSON格式）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_task_id (task_id),
    INDEX idx_level (level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行日志表';

-- =============================================
-- 9. 操作日志表
-- =============================================
DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(255) COMMENT '方法名称',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(50) COMMENT 'IP地址',
    status VARCHAR(20) COMMENT '状态：success-成功，fail-失败',
    error_msg TEXT COMMENT '错误信息',
    duration BIGINT COMMENT '耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
-- =============================================
-- 10. 流程草稿与发布版本
-- =============================================
CREATE TABLE IF NOT EXISTS workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(100),
    status VARCHAR(20) DEFAULT 'draft',
    version INT DEFAULT 1,
    user_id BIGINT,
    user_name VARCHAR(100),
    publish_time DATETIME,
    latest_version_id BIGINT,
    config LONGTEXT,
    input_schema LONGTEXT,
    graph LONGTEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow_code (workflow_code),
    INDEX idx_workflow_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程草稿表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程发布版本表';

-- =============================================
-- 11. 任务运行
-- =============================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务运行表';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='workflow debug run';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='workflow step run';

ALTER TABLE sys_task
    ADD COLUMN IF NOT EXISTS workflow_id BIGINT,
    ADD COLUMN IF NOT EXISTS workflow_version_id BIGINT,
    ADD COLUMN IF NOT EXISTS workflow_version INT,
    ADD COLUMN IF NOT EXISTS workflow_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS workflow_category VARCHAR(100),
    ADD COLUMN IF NOT EXISTS input_config LONGTEXT,
    ADD COLUMN IF NOT EXISTS schedule_config LONGTEXT,
    ADD COLUMN IF NOT EXISTS latest_run_id BIGINT,
    ADD COLUMN IF NOT EXISTS latest_run_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS last_run_time DATETIME,
    ADD COLUMN IF NOT EXISTS next_run_time DATETIME;

ALTER TABLE sys_crawl_result
    ADD COLUMN IF NOT EXISTS task_run_id BIGINT;

ALTER TABLE sys_ai_analysis_message
    ADD COLUMN IF NOT EXISTS task_run_id BIGINT;

ALTER TABLE sys_execution_log
    ADD COLUMN IF NOT EXISTS task_run_id BIGINT;
