-- ================================================================
-- RPA 管理平台 — 完整数据库部署脚本
-- 适用于：MySQL 8.0+
-- 用法：mysql -u root -p < deploy.sql
--       或直接在 MySQL Workbench / Navicat 中执行
--
-- 默认账号：
--   管理员：admin     密码：admin123
--   普通用户：user01  密码：user123
--   普通用户：user02  密码：user123
--
-- ⚠️  本脚本使用 IF NOT EXISTS / INSERT IGNORE，可安全重复执行，不会删除已有数据。
-- ================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ================================================================
-- 创建数据库
-- ================================================================
CREATE DATABASE IF NOT EXISTS management_system
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS spider_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON management_system.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON spider_db.* TO 'root'@'%';
FLUSH PRIVILEGES;

USE management_system;

-- ================================================================
-- 1. 用户表 sys_user
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`              BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username`        VARCHAR(50)  NOT NULL UNIQUE             COMMENT '用户名',
    `password`        VARCHAR(255) NOT NULL                    COMMENT '密码（BCrypt加密）',
    `real_name`       VARCHAR(50)                              COMMENT '真实姓名',
    `email`           VARCHAR(100)                             COMMENT '邮箱',
    `phone`           VARCHAR(20)                              COMMENT '手机号',
    `role`            VARCHAR(20)  NOT NULL                    COMMENT '角色：ADMIN / USER',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'active'   COMMENT '状态：active / inactive',
    `avatar`          VARCHAR(255)                             COMMENT '头像URL',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    `last_login_time` DATETIME                                  COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(50)                              COMMENT '最后登录IP',
    INDEX `idx_username` (`username`),
    INDEX `idx_role`     (`role`),
    INDEX `idx_status`   (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ================================================================
-- 2. 角色表 sys_role
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT      AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    `name`        VARCHAR(50) NOT NULL                    COMMENT '角色名称',
    `code`        VARCHAR(50) NOT NULL UNIQUE             COMMENT '角色编码',
    `description` VARCHAR(200)                            COMMENT '角色描述',
    `status`      VARCHAR(20) NOT NULL DEFAULT 'active'   COMMENT '状态',
    `sort_order`  INT                  DEFAULT 0          COMMENT '排序',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- ================================================================
-- 3. 权限表 sys_permission
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id`          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    `name`        VARCHAR(50)  NOT NULL                    COMMENT '权限名称',
    `code`        VARCHAR(100) NOT NULL UNIQUE             COMMENT '权限编码',
    `type`        VARCHAR(20)  NOT NULL                    COMMENT '类型：menu / button / api',
    `parent_id`   BIGINT               DEFAULT 0           COMMENT '父级ID，0=顶级',
    `path`        VARCHAR(255)                             COMMENT '路由路径',
    `icon`        VARCHAR(50)                              COMMENT '图标',
    `sort`        INT                  DEFAULT 0           COMMENT '排序',
    `status`      VARCHAR(20)  NOT NULL DEFAULT 'active'   COMMENT '状态',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    INDEX `idx_code`      (`code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_type`      (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限资源表';

-- ================================================================
-- 4. 角色权限关联表 sys_role_permission（Hibernate ManyToMany 连接表）
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `role_id`       BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    INDEX `idx_role_id`       (`role_id`),
    INDEX `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ================================================================
-- 5. 机器人表 sys_robot
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_robot` (
    `id`               BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '机器人ID',
    `robot_code`       VARCHAR(50)  NOT NULL UNIQUE             COMMENT '机器人编号',
    `name`             VARCHAR(100) NOT NULL                    COMMENT '机器人名称',
    `type`             VARCHAR(50)  NOT NULL                    COMMENT '机器人类型',
    `description`      VARCHAR(500)                             COMMENT '描述',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'offline'  COMMENT '状态：online / offline / running',
    `total_tasks`      BIGINT       NOT NULL DEFAULT 0          COMMENT '执行任务总数',
    `success_tasks`    BIGINT       NOT NULL DEFAULT 0          COMMENT '成功任务数',
    `failed_tasks`     BIGINT       NOT NULL DEFAULT 0          COMMENT '失败任务数',
    `success_rate`     DOUBLE       NOT NULL DEFAULT 0.0        COMMENT '成功率',
    `last_execute_time` DATETIME                                COMMENT '最后执行时间',
    `current_task_id`  VARCHAR(100)                             COMMENT '当前任务ID',
    `last_heartbeat`   DATETIME                                 COMMENT '最后心跳时间',
    `create_by`        BIGINT                                   COMMENT '创建者ID',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    INDEX `idx_robot_code` (`robot_code`),
    INDEX `idx_status`     (`status`),
    INDEX `idx_type`       (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机器人表';

-- ================================================================
-- 6. 任务表 sys_task
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_task` (
    `id`               BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_id`          VARCHAR(50)  UNIQUE                      COMMENT '任务编号（业务ID）',
    `name`             VARCHAR(100) NOT NULL                    COMMENT '任务名称',
    `type`             VARCHAR(50)                              COMMENT '任务类型：crawl / ai_workflow / ...',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'pending'  COMMENT '状态：pending / running / completed / failed',
    `progress`         INT          NOT NULL DEFAULT 0          COMMENT '进度（0-100）',
    `robot_id`         BIGINT                                   COMMENT '执行机器人ID',
    `robot_name`       VARCHAR(50)                              COMMENT '执行机器人名称',
    `params`           LONGTEXT                                 COMMENT '任务参数（JSON）',
    `priority`         VARCHAR(20)           DEFAULT 'medium'   COMMENT '优先级：high / medium / low',
    `execute_type`     VARCHAR(20)           DEFAULT 'immediate' COMMENT '执行方式：immediate / scheduled',
    `scheduled_time`   DATETIME                                 COMMENT '计划执行时间',
    `start_time`       DATETIME                                 COMMENT '实际开始时间',
    `end_time`         DATETIME                                 COMMENT '实际结束时间',
    `duration`         INT                   DEFAULT 0          COMMENT '耗时（秒）',
    `user_id`          BIGINT                                   COMMENT '创建用户ID',
    `user_name`        VARCHAR(50)                              COMMENT '创建用户名',
    `description`      LONGTEXT                                 COMMENT '任务描述',
    `result`           LONGTEXT                                 COMMENT '执行结果',
    `error_message`    LONGTEXT                                 COMMENT '错误信息',
    `tax_id`           VARCHAR(50)                              COMMENT '税号（税务采集专用）',
    `enterprise_name`  VARCHAR(200)                             COMMENT '企业名称',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP
                                   ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    INDEX `idx_task_id`  (`task_id`),
    INDEX `idx_status`   (`status`),
    INDEX `idx_user_id`  (`user_id`),
    INDEX `idx_robot_id` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- ================================================================
-- 7. 执行日志表 sys_execution_log
-- ⚠️  包含实体类所有字段（历史 schema.sql 中该表缺少 5 个字段）
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_execution_log` (
    `id`          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `task_id`     BIGINT                                  COMMENT '任务主键ID',
    `task_code`   VARCHAR(50)                             COMMENT '任务编号',
    `task_name`   VARCHAR(100)                            COMMENT '任务名称',
    `robot_id`    BIGINT                                  COMMENT '机器人ID',
    `robot_name`  VARCHAR(50)                             COMMENT '机器人名称',
    `level`       VARCHAR(20)  NOT NULL DEFAULT 'INFO'    COMMENT '日志级别：INFO / WARN / ERROR',
    `message`     TEXT         NOT NULL                   COMMENT '日志内容',
    `stage`       VARCHAR(50)                             COMMENT '执行阶段：start / process / end / error',
    `extra_data`  LONGTEXT                                COMMENT '额外数据（JSON格式）',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    INDEX `idx_task_id`    (`task_id`),
    INDEX `idx_task_code`  (`task_code`),
    INDEX `idx_robot_id`   (`robot_id`),
    INDEX `idx_level`      (`level`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行日志表';

-- ================================================================
-- 8. 操作日志表 sys_operation_log
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id`          BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    `user_id`     BIGINT                                   COMMENT '用户ID',
    `username`    VARCHAR(50)                              COMMENT '用户名',
    `operation`   VARCHAR(100)                             COMMENT '操作描述',
    `method`      VARCHAR(255)                             COMMENT '请求方法',
    `params`      TEXT                                     COMMENT '请求参数',
    `ip`          VARCHAR(50)                              COMMENT 'IP地址',
    `status`      VARCHAR(20)                              COMMENT '状态：success / fail',
    `error_msg`   TEXT                                     COMMENT '错误信息',
    `duration`    BIGINT                                   COMMENT '耗时（毫秒）',
    `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    INDEX `idx_user_id`    (`user_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ================================================================
-- 9. 爬虫结果表 sys_crawl_result
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_crawl_result` (
    `id`              BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_record_id`  BIGINT                                  COMMENT '任务主表ID',
    `task_id`         VARCHAR(50)  NOT NULL UNIQUE            COMMENT '任务编号',
    `task_name`       VARCHAR(100)                            COMMENT '任务名称',
    `final_url`       VARCHAR(1000)                           COMMENT '最终URL',
    `title`           VARCHAR(500)                            COMMENT '页面标题',
    `summary_text`    LONGTEXT                                COMMENT '正文摘要',
    `raw_html`        LONGTEXT                                COMMENT '原始HTML',
    `structured_data` LONGTEXT                                COMMENT '结构化结果（JSON）',
    `total_count`     INT          DEFAULT 0                  COMMENT '结果条数',
    `crawled_pages`   INT          DEFAULT 0                  COMMENT '抓取页数',
    `status`          VARCHAR(20)  DEFAULT 'pending'          COMMENT '状态',
    `error_message`   LONGTEXT                                COMMENT '错误信息',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_task_record_id` (`task_record_id`),
    INDEX `idx_status`         (`status`),
    INDEX `idx_create_time`    (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='爬虫抓取结果表';

-- ================================================================
-- 10. 数据采集配置表 sys_collect_config
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_collect_config` (
    `id`                  BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name`                VARCHAR(100) NOT NULL                    COMMENT '配置名称',
    `task_id`             BIGINT                                   COMMENT '关联任务ID',
    `robot_id`            BIGINT                                   COMMENT '关联机器人ID',
    `collect_type`        VARCHAR(20)  NOT NULL DEFAULT 'web'      COMMENT '采集类型：web / api / database',
    `target_url`          VARCHAR(500)                             COMMENT '目标URL',
    `request_method`      VARCHAR(10)           DEFAULT 'GET'      COMMENT '请求方法',
    `request_headers`     TEXT                                     COMMENT '请求头（JSON）',
    `request_params`      TEXT                                     COMMENT '请求参数（JSON）',
    `request_body`        TEXT                                     COMMENT '请求体',
    `collect_rules`       TEXT                                     COMMENT '采集规则（JSON）',
    `field_mapping`       TEXT                                     COMMENT '字段映射（JSON）',
    `data_clean_rules`    TEXT                                     COMMENT '数据清洗规则（JSON）',
    `page_config`         TEXT                                     COMMENT '分页配置（JSON）',
    `cron_expression`     VARCHAR(100)                             COMMENT '定时表达式（Cron）',
    `is_enabled`          TINYINT(1)            DEFAULT 1          COMMENT '是否启用',
    `timeout`             INT                   DEFAULT 30000      COMMENT '超时（毫秒）',
    `retry_count`         INT                   DEFAULT 3          COMMENT '重试次数',
    `proxy_config`        TEXT                                     COMMENT '代理配置（JSON）',
    `output_config`       TEXT                                     COMMENT '输出配置（JSON）',
    `spider_config`       TEXT                                     COMMENT '税务 Spider 配置',
    `last_execute_time`   DATETIME                                 COMMENT '最后执行时间',
    `last_execute_status` VARCHAR(20)                              COMMENT '最后执行状态',
    `total_count`         BIGINT                DEFAULT 0          COMMENT '执行总次数',
    `success_count`       BIGINT                DEFAULT 0          COMMENT '成功次数',
    `fail_count`          BIGINT                DEFAULT 0          COMMENT '失败次数',
    `create_by`           BIGINT                                   COMMENT '创建者ID',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
    INDEX `idx_collect_type` (`collect_type`),
    INDEX `idx_is_enabled`   (`is_enabled`),
    INDEX `idx_robot_id`     (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集配置表';

-- ================================================================
-- 11. 采集数据表 sys_collect_data
-- ================================================================
CREATE TABLE IF NOT EXISTS `sys_collect_data` (
    `id`           BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `config_id`    BIGINT       NOT NULL                   COMMENT '采集配置ID',
    `task_id`      BIGINT                                  COMMENT '任务ID',
    `robot_id`     BIGINT                                  COMMENT '机器人ID',
    `source_url`   VARCHAR(500)                            COMMENT '数据来源URL',
    `data_hash`    VARCHAR(64)                             COMMENT '数据Hash（去重用）',
    `data_content` TEXT                                    COMMENT '采集数据内容（JSON）',
    `raw_html`     MEDIUMTEXT                              COMMENT '原始HTML',
    `collect_time` DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '采集时间',
    `status`       VARCHAR(20)  DEFAULT 'valid'            COMMENT '状态：valid / invalid / duplicate',
    `remark`       VARCHAR(500)                            COMMENT '备注',
    INDEX `idx_config_id`    (`config_id`),
    INDEX `idx_task_id`      (`task_id`),
    INDEX `idx_data_hash`    (`data_hash`),
    INDEX `idx_collect_time` (`collect_time`),
    INDEX `idx_status`       (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集数据表';

-- ================================================================
-- 12. 节点类型表 node_type（流程设计器使用）
-- ================================================================
CREATE TABLE IF NOT EXISTS `node_type` (
    `id`             BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `type`           VARCHAR(50)  NOT NULL UNIQUE             COMMENT '节点类型代码',
    `name`           VARCHAR(100) NOT NULL                    COMMENT '节点类型名称',
    `icon`           VARCHAR(100)                             COMMENT '图标名称',
    `color`          VARCHAR(20)                              COMMENT '图标颜色',
    `category`       VARCHAR(50)                              COMMENT '分类',
    `sort_order`     INT          DEFAULT 0                   COMMENT '排序',
    `enabled`        TINYINT(1)   DEFAULT 1                   COMMENT '是否启用',
    `default_config` LONGTEXT                                 COMMENT '默认配置（JSON）',
    `description`    VARCHAR(500)                             COMMENT '描述',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点类型表';

-- ================================================================
-- 13. 流程定义表 workflow
-- ================================================================
CREATE TABLE IF NOT EXISTS `workflow` (
    `id`            BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `workflow_code` VARCHAR(100) NOT NULL UNIQUE             COMMENT '流程编号',
    `name`          VARCHAR(200) NOT NULL                    COMMENT '流程名称',
    `description`   VARCHAR(1000)                            COMMENT '流程描述',
    `status`        VARCHAR(20)  DEFAULT 'draft'             COMMENT '状态：draft / published / archived',
    `version`       INT          DEFAULT 1                   COMMENT '版本号',
    `user_id`       BIGINT                                   COMMENT '创建用户ID',
    `user_name`     VARCHAR(100)                             COMMENT '创建用户名',
    `publish_time`  DATETIME                                 COMMENT '发布时间',
    `config`        LONGTEXT                                 COMMENT '流程配置（JSON，含节点和连线）',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    INDEX `idx_status`  (`status`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';

-- ================================================================
-- 14. 流程节点表 workflow_node
-- ================================================================
CREATE TABLE IF NOT EXISTS `workflow_node` (
    `id`           BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `workflow_id`  BIGINT       NOT NULL                   COMMENT '所属流程ID',
    `node_type_id` BIGINT                                  COMMENT '节点类型ID',
    `node_type`    VARCHAR(50)                             COMMENT '节点类型代码',
    `name`         VARCHAR(200) NOT NULL                   COMMENT '节点名称',
    `description`  VARCHAR(500)                            COMMENT '节点描述',
    `x`            INT                                     COMMENT 'X坐标',
    `y`            INT                                     COMMENT 'Y坐标',
    `config`       TEXT                                    COMMENT '节点配置（JSON）',
    `timeout`      INT          DEFAULT 60                 COMMENT '超时（秒）',
    `retry_count`  INT          DEFAULT 3                  COMMENT '重试次数',
    `order`        INT          DEFAULT 0                  COMMENT '执行顺序',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_workflow_id` (`workflow_id`),
    FOREIGN KEY (`workflow_id`) REFERENCES `workflow`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程节点表';


-- ================================================================
-- ★  初始化数据（均使用 INSERT IGNORE，可安全重复执行）
-- ================================================================

-- ----------------------------------------------------------------
-- 角色
-- ----------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`, `name`, `code`, `description`, `status`, `sort_order`) VALUES
(1, '管理员', 'ADMIN', '系统管理员，拥有所有权限', 'active', 1),
(2, '普通用户', 'USER',  '普通用户，可管理自己的任务',  'active', 2),
(3, '访客',   'GUEST', '访客用户，只有基本查看权限',   'active', 3);

-- ----------------------------------------------------------------
-- 用户
-- BCrypt 说明：
--   admin123  → $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH
--   user123   → $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- ⚠️  如果启动后登录失败，请删除 sys_user 表数据，重启应用即可由 DataInitializer 自动重建。
-- ----------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `role`, `status`) VALUES
(1, 'admin',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@example.com',    '13800138000', 'ADMIN', 'active'),
(2, 'user01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三',     'zhangsan@example.com', '13900139001', 'USER',  'active'),
(3, 'user02', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四',     'lisi@example.com',     '13900139002', 'USER',  'active');

-- ----------------------------------------------------------------
-- 权限菜单
-- ----------------------------------------------------------------
INSERT IGNORE INTO `sys_permission` (`id`, `name`, `code`, `type`, `parent_id`, `path`, `icon`, `sort`, `status`) VALUES
-- 系统管理
( 1, '系统管理',  'system',              'menu',   0, '/system',             'Setting',      1, 'active'),
( 2, '用户管理',  'system:user',         'menu',   1, '/system/user',        'User',         1, 'active'),
( 3, '查看用户',  'system:user:view',    'button', 2, NULL,                  NULL,           1, 'active'),
( 4, '新增用户',  'system:user:add',     'button', 2, NULL,                  NULL,           2, 'active'),
( 5, '编辑用户',  'system:user:edit',    'button', 2, NULL,                  NULL,           3, 'active'),
( 6, '删除用户',  'system:user:delete',  'button', 2, NULL,                  NULL,           4, 'active'),
( 7, '角色管理',  'system:role',         'menu',   1, '/system/role',        'UserFilled',   2, 'active'),
( 8, '新增角色',  'system:role:add',     'button', 7, NULL,                  NULL,           1, 'active'),
( 9, '编辑角色',  'system:role:edit',    'button', 7, NULL,                  NULL,           2, 'active'),
(10, '删除角色',  'system:role:delete',  'button', 7, NULL,                  NULL,           3, 'active'),
(11, '分配权限',  'system:role:perm',    'button', 7, NULL,                  NULL,           4, 'active'),
(12, '权限管理',  'system:permission',   'menu',   1, '/system/permission',  'Key',          3, 'active'),
-- 任务管理
(13, '任务管理',  'task',                'menu',   0, '/task',               'List',         2, 'active'),
(14, '任务列表',  'task:list',           'menu',  13, '/task/list',          'Document',     1, 'active'),
(15, '创建任务',  'task:create',         'menu',  13, '/task/create',        'Plus',         2, 'active'),
(16, '任务历史',  'task:history',        'menu',  13, '/task/history',       'Clock',        3, 'active'),
(17, '启动任务',  'task:start',          'button',14, NULL,                  NULL,           1, 'active'),
(18, '停止任务',  'task:stop',           'button',14, NULL,                  NULL,           2, 'active'),
-- 流程管理
(19, '流程管理',  'workflow',            'menu',   0, '/workflow',           'Share',        3, 'active'),
(20, '流程列表',  'workflow:list',       'menu',  19, '/workflow/list',      'Document',     1, 'active'),
(21, '流程设计',  'workflow:design',     'menu',  19, '/workflow/design',    'Edit',         2, 'active'),
-- 机器人管理
(22, '机器人管理','robot',               'menu',   0, '/robot',              'Cpu',          4, 'active'),
(23, '机器人列表','robot:list',          'menu',  22, '/robot/list',         'Monitor',      1, 'active'),
(24, '机器人配置','robot:config',        'menu',  22, '/robot/config',       'Setting',      2, 'active'),
(25, '启动机器人','robot:start',         'button',23, NULL,                  NULL,           1, 'active'),
(26, '停止机器人','robot:stop',          'button',23, NULL,                  NULL,           2, 'active'),
-- 执行监控
(27, '执行监控',  'monitor',             'menu',   0, '/monitor',            'View',         5, 'active'),
(28, '实时监控',  'monitor:realtime',    'menu',  27, '/monitor/realtime',   'DataLine',     1, 'active'),
(29, '执行日志',  'monitor:logs',        'menu',  27, '/monitor/logs',       'Tickets',      2, 'active'),
-- 数据统计
(30, '数据统计',  'statistics',          'menu',   0, '/statistics',         'DataAnalysis', 6, 'active'),
(31, '数据查询',  'statistics:query',    'menu',  30, '/statistics/query',   'Search',       1, 'active'),
(32, '统计报表',  'statistics:report',   'menu',  30, '/statistics/report',  'DataBoard',    2, 'active'),
-- 系统设置
(33, '系统设置',  'settings',            'menu',   0, '/settings',           'Tools',        7, 'active'),
(34, '基础设置',  'settings:basic',      'menu',  33, '/settings/basic',     'Setting',      1, 'active'),
(35, '通知设置',  'settings:notification','menu', 33, '/settings/notification','Bell',       2, 'active');

-- 管理员拥有所有权限
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `sys_permission`;

-- 普通用户权限（任务管理、流程查看、机器人查看、监控查看）
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2,13),(2,14),(2,15),(2,16),(2,17),(2,18),  -- 任务管理
(2,19),(2,20),(2,21),                        -- 流程查看
(2,22),(2,23),                               -- 机器人查看
(2,27),(2,28),(2,29),                        -- 监控查看
(2,30),(2,31),(2,32);                        -- 数据统计

-- ----------------------------------------------------------------
-- 初始机器人
-- ----------------------------------------------------------------
INSERT IGNORE INTO `sys_robot` (`robot_code`, `name`, `type`, `description`, `status`, `total_tasks`, `success_tasks`, `failed_tasks`, `success_rate`) VALUES
('DC-001', '数据采集机器人-1', 'data_collector',   '自动采集网站数据',     'online',  128, 125, 3, 97.66),
('RG-001', '报表生成机器人-1', 'report_generator', '自动生成业务报表',     'online',   85,  85, 0, 100.00),
('TS-001', '任务调度机器人-1', 'task_scheduler',   '定时任务调度管理',     'offline',  42,  40, 2, 95.24),
('NT-001', '消息通知机器人-1', 'notification',     '发送消息通知',         'offline',   0,   0, 0,  0.00),
('FP-001', '文件处理机器人-1', 'file_processor',   '处理文件格式转换',     'online',   56,  54, 2, 96.43);

-- ----------------------------------------------------------------
-- 节点类型
-- ----------------------------------------------------------------
INSERT IGNORE INTO `node_type` (`type`, `name`, `icon`, `color`, `category`, `sort_order`, `enabled`, `description`) VALUES
('start',        '开始节点',   'VideoPlay',  '#52c41a', '基础节点',   1, 1, '流程起始节点'),
('end',          '结束节点',   'VideoPause', '#ff4d4f', '基础节点',   2, 1, '流程结束节点'),
('http',         'HTTP请求',   'Connection', '#1890ff', '机器人节点', 3, 1, '发送HTTP请求'),
('data_process', '数据处理',   'DataAnalysis','#E6A23C','机器人节点', 4, 1, '数据清洗和转换'),
('condition',    '条件判断',   'Share',      '#faad14', '逻辑节点',   5, 1, '根据条件分支执行'),
('parallel',     '并行执行',   'Grid',       '#722ed1', '逻辑节点',   6, 1, '多分支并行执行'),
('loop',         '循环节点',   'Refresh',    '#eb2f96', '逻辑节点',   7, 1, '循环执行某段流程'),
('delay',        '延时等待',   'Timer',      '#9ACD32', '工具节点',   8, 1, '延时等待指定时间'),
('email',        '邮件发送',   'Message',    '#FF69B4', '工具节点',   9, 1, '发送邮件通知'),
('webhook',      'Webhook',    'Link',       '#2f54eb', '工具节点',  10, 1, '调用外部API');

-- ----------------------------------------------------------------
-- 示例流程
-- ----------------------------------------------------------------
INSERT IGNORE INTO `workflow` (`id`, `workflow_code`, `name`, `description`, `status`, `version`, `user_id`, `user_name`, `config`) VALUES
(1, 'WF-DATA-001',     '数据采集自动处理流程', '自动采集网页数据并进行清洗处理，发送邮件通知', 'published', 1, 1, 'admin', '{"nodes":[],"edges":[]}'),
(2, 'WF-REPORT-001',   '报表自动生成流程',     '定时生成业务报表，发送给相关人员',             'published', 1, 1, 'admin', '{"nodes":[],"edges":[]}'),
(3, 'WF-APPROVAL-001', '任务审批流程',         '自动化任务审批流程（草稿）',                   'draft',     1, 1, 'admin', '{"nodes":[],"edges":[]}'),
(4, 'WF-ALERT-001',    '异常告警处理流程',     '监控系统异常并自动告警处理',                   'published', 2, 1, 'admin', '{"nodes":[],"edges":[]}');

SET FOREIGN_KEY_CHECKS = 1;

-- ================================================================
-- 验证查询（可选，确认初始化结果）
-- ================================================================
SELECT CONCAT('✅ 用户数：',    COUNT(*)) AS info FROM `sys_user`;
SELECT CONCAT('✅ 角色数：',    COUNT(*)) AS info FROM `sys_role`;
SELECT CONCAT('✅ 权限数：',    COUNT(*)) AS info FROM `sys_permission`;
SELECT CONCAT('✅ 机器人数：',  COUNT(*)) AS info FROM `sys_robot`;
SELECT CONCAT('✅ 节点类型数：',COUNT(*)) AS info FROM `node_type`;
SELECT CONCAT('✅ 流程数：',    COUNT(*)) AS info FROM `workflow`;

SELECT '================================================================' AS '';
SELECT '数据库初始化完成！' AS '';
SELECT '管理员账号: admin / admin123' AS '';
SELECT '普通用户账号: user01 / user123,  user02 / user123' AS '';
SELECT '================================================================' AS '';
