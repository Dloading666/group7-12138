-- =============================================
-- 绠＄悊绯荤粺鏁版嵁搴撹〃缁撴瀯
-- 鏁版嵁搴擄細MySQL 8.0+ / H2
-- 瀛楃闆嗭細utf8mb4
-- =============================================

-- 鍒涘缓鏁版嵁搴擄紙浠匨ySQL锛?
-- CREATE DATABASE IF NOT EXISTS management_system 
-- DEFAULT CHARACTER SET utf8mb4 
-- COLLATE utf8mb4_unicode_ci;
-- 
-- USE management_system;

-- =============================================
-- 1. 鐢ㄦ埛琛?
-- =============================================
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鐢ㄦ埛ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '鐢ㄦ埛鍚?,
    password VARCHAR(255) NOT NULL COMMENT '瀵嗙爜锛圔Crypt鍔犲瘑锛?,
    real_name VARCHAR(50) COMMENT '鐪熷疄濮撳悕',
    email VARCHAR(100) COMMENT '閭',
    phone VARCHAR(20) COMMENT '鎵嬫満鍙?,
    role VARCHAR(20) NOT NULL COMMENT '瑙掕壊锛欰DMIN-绠＄悊鍛橈紝USER-鏅€氱敤鎴?,
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '鐘舵€侊細active-鍚敤锛宨nactive-绂佺敤',
    avatar VARCHAR(255) COMMENT '澶村儚URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    last_login_time DATETIME COMMENT '鏈€鍚庣櫥褰曟椂闂?,
    last_login_ip VARCHAR(50) COMMENT '鏈€鍚庣櫥褰旾P',
    
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐢ㄦ埛琛?;

-- =============================================
-- 2. 瑙掕壊琛?
-- =============================================
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '瑙掕壊ID',
    name VARCHAR(50) NOT NULL COMMENT '瑙掕壊鍚嶇О',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '瑙掕壊缂栫爜',
    description VARCHAR(255) COMMENT '瑙掕壊鎻忚堪',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '鐘舵€?,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='瑙掕壊琛?;

-- =============================================
-- 3. 鏉冮檺琛?
-- =============================================
DROP TABLE IF EXISTS sys_permission;

CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鏉冮檺ID',
    name VARCHAR(50) NOT NULL COMMENT '鏉冮檺鍚嶇О',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '鏉冮檺缂栫爜',
    type VARCHAR(20) NOT NULL COMMENT '鏉冮檺绫诲瀷锛歮enu-鑿滃崟锛宐utton-鎸夐挳锛宎pi-鎺ュ彛',
    parent_id BIGINT DEFAULT 0 COMMENT '鐖剁骇ID',
    path VARCHAR(255) COMMENT '璺緞',
    icon VARCHAR(50) COMMENT '鍥炬爣',
    sort INT DEFAULT 0 COMMENT '鎺掑簭',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '鐘舵€?,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    
    INDEX idx_code (code),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏉冮檺琛?;

-- =============================================
-- 4. 瑙掕壊鏉冮檺鍏宠仈琛?
-- =============================================
DROP TABLE IF EXISTS sys_role_permission;

CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '瑙掕壊ID',
    permission_id BIGINT NOT NULL COMMENT '鏉冮檺ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='瑙掕壊鏉冮檺鍏宠仈琛?;

-- =============================================
-- 5. 浠诲姟琛?
-- =============================================
DROP TABLE IF EXISTS sys_task;

CREATE TABLE sys_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '浠诲姟ID',
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '浠诲姟缂栧彿',
    name VARCHAR(100) NOT NULL COMMENT '浠诲姟鍚嶇О',
    type VARCHAR(50) NOT NULL COMMENT '浠诲姟绫诲瀷',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '鐘舵€侊細pending-绛夊緟锛宺unning-鎵ц涓紝completed-瀹屾垚锛宖ailed-澶辫触',
    progress INT DEFAULT 0 COMMENT '杩涘害(0-100)',
    robot_id BIGINT COMMENT '鎵ц鏈哄櫒浜篒D',
    robot_name VARCHAR(50) COMMENT '鎵ц鏈哄櫒浜哄悕绉?,
    params LONGTEXT COMMENT '浠诲姟鍙傛暟(JSON)',
    description LONGTEXT COMMENT '浠诲姟鎻忚堪',
    result LONGTEXT COMMENT '鎵ц缁撴灉',
    error_message LONGTEXT COMMENT '閿欒淇℃伅',
    priority VARCHAR(20) DEFAULT 'medium' COMMENT '浼樺厛绾э細high-楂橈紝medium-涓紝low-浣?,
    execute_type VARCHAR(20) DEFAULT 'immediate' COMMENT '鎵ц鏂瑰紡锛歩mmediate-绔嬪嵆锛宻cheduled-瀹氭椂',
    scheduled_time DATETIME COMMENT '瀹氭椂鎵ц鏃堕棿',
    user_id BIGINT COMMENT '鍒涘缓鐢ㄦ埛ID',
    user_name VARCHAR(50) COMMENT '鍒涘缓鐢ㄦ埛鍚?,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    start_time DATETIME COMMENT '寮€濮嬫椂闂?,
    end_time DATETIME COMMENT '缁撴潫鏃堕棿',
    duration INT COMMENT '鑰楁椂(绉?',
    tax_id VARCHAR(50) COMMENT '绋庡彿',
    enterprise_name VARCHAR(200) COMMENT '浼佷笟鍚嶇О',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浠诲姟琛?;

-- =============================================
-- 6. 鎶撳彇缁撴灉琛?
-- =============================================
DROP TABLE IF EXISTS sys_crawl_result;

CREATE TABLE sys_crawl_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鎶撳彇缁撴灉ID',
    task_record_id BIGINT COMMENT '浠诲姟涓昏〃ID',
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '浠诲姟缂栧彿',
    task_name VARCHAR(100) COMMENT '浠诲姟鍚嶇О',
    final_url VARCHAR(1000) COMMENT '鏈€缁圲RL',
    title VARCHAR(500) COMMENT '椤甸潰鏍囬',
    summary_text LONGTEXT COMMENT '姝ｆ枃鎽樿',
    raw_html LONGTEXT COMMENT '鍘熷HTML',
    structured_data LONGTEXT COMMENT '缁撴瀯鍖栫粨鏋?,
    total_count INT DEFAULT 0 COMMENT '缁撴灉鏉℃暟',
    crawled_pages INT DEFAULT 0 COMMENT '鎶撳彇椤垫暟',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '鐘舵€?,
    error_message LONGTEXT COMMENT '閿欒淇℃伅',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',

    INDEX idx_crawl_result_task_record_id (task_record_id),
    INDEX idx_crawl_result_status (status),
    INDEX idx_crawl_result_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鐪熷疄缃戠珯鎶撳彇缁撴灉琛?;

-- =============================================
-- 7. 鏈哄櫒浜鸿〃
-- =============================================
DROP TABLE IF EXISTS sys_robot;

CREATE TABLE sys_robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鏈哄櫒浜篒D',
    name VARCHAR(50) NOT NULL COMMENT '鏈哄櫒浜哄悕绉?,
    type VARCHAR(50) NOT NULL COMMENT '鏈哄櫒浜虹被鍨?,
    status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '鐘舵€侊細online-鍦ㄧ嚎锛宱ffline-绂荤嚎',
    ip_address VARCHAR(50) COMMENT 'IP鍦板潃',
    port INT COMMENT '绔彛',
    config TEXT COMMENT '閰嶇疆淇℃伅(JSON)',
    last_heartbeat DATETIME COMMENT '鏈€鍚庡績璺虫椂闂?,
    task_count INT DEFAULT 0 COMMENT '鎵ц浠诲姟鏁?,
    success_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '鎴愬姛鐜?,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鏈哄櫒浜鸿〃';

-- =============================================
-- 8. 鎵ц鏃ュ織琛?
-- =============================================
-- =============================================
-- 7.1 AI 鍒嗘瀽闂瓟娑堟伅琛?
-- =============================================
DROP TABLE IF EXISTS sys_ai_analysis_message;

CREATE TABLE sys_ai_analysis_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '娑堟伅ID',
    analysis_task_id BIGINT NULL COMMENT 'AI鍒嗘瀽浠诲姟ID',
    task_run_id BIGINT NULL COMMENT '浠诲姟杩愯ID',
    role VARCHAR(20) NOT NULL COMMENT '瑙掕壊:user/assistant',
    content LONGTEXT NOT NULL COMMENT '娑堟伅鍐呭',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',

    INDEX idx_ai_analysis_task_id (analysis_task_id),
    INDEX idx_ai_analysis_task_run_id (task_run_id),
    INDEX idx_ai_analysis_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI鍒嗘瀽闂瓟娑堟伅琛?;

DROP TABLE IF EXISTS sys_execution_log;

CREATE TABLE sys_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鏃ュ織ID',
    task_id BIGINT COMMENT '浠诲姟ID',
    task_code VARCHAR(50) COMMENT '浠诲姟缂栧彿',
    task_name VARCHAR(100) COMMENT '浠诲姟鍚嶇О',
    robot_id BIGINT COMMENT '鏈哄櫒浜篒D',
    robot_name VARCHAR(50) COMMENT '鏈哄櫒浜哄悕绉?,
    level VARCHAR(20) NOT NULL DEFAULT 'INFO' COMMENT '鏃ュ織绾у埆锛欼NFO, WARN, ERROR',
    message TEXT NOT NULL COMMENT '鏃ュ織鍐呭',
    stage VARCHAR(50) COMMENT '鎵ц闃舵: start, process, end, error',
    extra_data LONGTEXT COMMENT '棰濆鏁版嵁锛圝SON鏍煎紡锛?,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',

    INDEX idx_task_id (task_id),
    INDEX idx_level (level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鎵ц鏃ュ織琛?;

-- =============================================
-- 9. 鎿嶄綔鏃ュ織琛?
-- =============================================
DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '鏃ュ織ID',
    user_id BIGINT COMMENT '鐢ㄦ埛ID',
    username VARCHAR(50) COMMENT '鐢ㄦ埛鍚?,
    operation VARCHAR(100) COMMENT '鎿嶄綔鎻忚堪',
    method VARCHAR(255) COMMENT '鏂规硶鍚嶇О',
    params TEXT COMMENT '璇锋眰鍙傛暟',
    ip VARCHAR(50) COMMENT 'IP鍦板潃',
    status VARCHAR(20) COMMENT '鐘舵€侊細success-鎴愬姛锛宖ail-澶辫触',
    error_msg TEXT COMMENT '閿欒淇℃伅',
    duration BIGINT COMMENT '鑰楁椂(姣)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='鎿嶄綔鏃ュ織琛?;
-- =============================================
-- 10. 娴佺▼鑽夌涓庡彂甯冪増鏈?-- =============================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='娴佺▼鑽夌琛?;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='娴佺▼鍙戝竷鐗堟湰琛?;

-- =============================================
-- 11. 浠诲姟杩愯
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浠诲姟杩愯琛?;

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
    robot_id BIGINT,
    robot_name VARCHAR(100),
    robot_type VARCHAR(50),
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
ALTER TABLE sys_ai_analysis_message
    MODIFY COLUMN analysis_task_id BIGINT NULL;

ALTER TABLE sys_execution_log
    ADD COLUMN IF NOT EXISTS task_run_id BIGINT;
