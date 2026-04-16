-- 指标采集四步流程数据库表结构

-- 1. 采集表（Collect）
CREATE TABLE IF NOT EXISTS `rpa_indicator_collection` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '纳税人识别号',
    `usc_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `app_date` DATE DEFAULT NULL COMMENT '申请日期',
    `collected_payload` JSON DEFAULT NULL COMMENT '采集的原始数据（JSON格式）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tax_no` (`tax_no`),
    KEY `idx_usc_code` (`usc_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标采集表';

-- 2. 解析表（Parse）
CREATE TABLE IF NOT EXISTS `rpa_indicator_parsing` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `collection_id` BIGINT DEFAULT NULL COMMENT '关联采集表ID',
    `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '纳税人识别号',
    `usc_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `app_date` DATE DEFAULT NULL COMMENT '申请日期',
    `parsed_data` JSON DEFAULT NULL COMMENT '解析后的结构化数据（JSON格式，包含发票明细和月份字段）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_collection_id` (`collection_id`),
    KEY `idx_tax_no` (`tax_no`),
    KEY `idx_usc_code` (`usc_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标解析表';

-- 3. 处理表（Process）
CREATE TABLE IF NOT EXISTS `rpa_indicator_processing` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parsing_id` BIGINT DEFAULT NULL COMMENT '关联解析表ID',
    `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '纳税人识别号',
    `usc_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `app_date` DATE DEFAULT NULL COMMENT '申请日期',
    `processed_result` JSON DEFAULT NULL COMMENT '处理结果（JSON格式，包含按月汇总、均值、标准差、波动系数等）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parsing_id` (`parsing_id`),
    KEY `idx_tax_no` (`tax_no`),
    KEY `idx_usc_code` (`usc_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标处理表';

-- 4. 查询表（Persist/Query）
CREATE TABLE IF NOT EXISTS `rpa_indicator_query` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `collection_id` BIGINT DEFAULT NULL COMMENT '关联采集表ID',
    `parsing_id` BIGINT DEFAULT NULL COMMENT '关联解析表ID',
    `processing_id` BIGINT DEFAULT NULL COMMENT '关联处理表ID',
    `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '纳税人识别号',
    `usc_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `app_date` DATE DEFAULT NULL COMMENT '申请日期',
    `business_json` JSON DEFAULT NULL COMMENT '业务JSON（包含所有步骤的结果和关联ID）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_collection_id` (`collection_id`),
    KEY `idx_parsing_id` (`parsing_id`),
    KEY `idx_processing_id` (`processing_id`),
    KEY `idx_tax_no` (`tax_no`),
    KEY `idx_usc_code` (`usc_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标查询表';
