-- 创建机器人表
CREATE TABLE IF NOT EXISTS sys_robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    robot_code VARCHAR(50) NOT NULL UNIQUE COMMENT '机器人编号',
    name VARCHAR(100) NOT NULL COMMENT '机器人名称',
    type VARCHAR(50) NOT NULL COMMENT '机器人类型',
    description VARCHAR(500) COMMENT '机器人描述',
    status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态：online-在线, offline-离线, running-运行中',
    total_tasks BIGINT NOT NULL DEFAULT 0 COMMENT '执行任务总数',
    success_tasks BIGINT NOT NULL DEFAULT 0 COMMENT '成功任务数',
    failed_tasks BIGINT NOT NULL DEFAULT 0 COMMENT '失败任务数',
    success_rate DOUBLE NOT NULL DEFAULT 0.0 COMMENT '成功率',
    last_execute_time DATETIME COMMENT '最后执行时间',
    create_by BIGINT COMMENT '创建者ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_robot_code (robot_code),
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机器人表';

-- 插入测试数据
INSERT INTO sys_robot (robot_code, name, type, description, status, total_tasks, success_tasks, failed_tasks, success_rate) VALUES
('DC-001', '数据采集机器人-1', 'data_collector', '自动采集网站数据', 'online', 128, 125, 3, 97.66),
('RG-001', '报表生成机器人-1', 'report_generator', '自动生成业务报表', 'online', 85, 85, 0, 100.00),
('TS-001', '任务调度机器人-1', 'task_scheduler', '定时任务调度管理', 'offline', 42, 40, 2, 95.24),
('NT-001', '消息通知机器人-1', 'notification', '发送消息通知', 'offline', 0, 0, 0, 0.00);
