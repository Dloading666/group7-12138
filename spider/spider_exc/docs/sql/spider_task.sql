-- 外部任务调度表（用于 project-gl 任务调度 spider_exc）
CREATE TABLE IF NOT EXISTS `rpa_spider_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` VARCHAR(64) NOT NULL UNIQUE COMMENT 'project-gl 侧任务ID',
    `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '纳税人识别号',
    `usc_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `app_date` DATE DEFAULT NULL COMMENT '申请日期',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '任务状态: pending/running/completed/failed',
    `error_message` TEXT COMMENT '失败原因',
    `collection_id` BIGINT COMMENT '关联采集记录ID',
    `parsing_id` BIGINT COMMENT '关联解析记录ID',
    `processing_id` BIGINT COMMENT '关联处理记录ID',
    `query_id` BIGINT COMMENT '关联查询记录ID',
    `callback_url` VARCHAR(500) DEFAULT NULL COMMENT '回调地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部任务调度表';
