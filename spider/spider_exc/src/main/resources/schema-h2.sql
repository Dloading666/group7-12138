-- spider_exc H2 开发环境初始化脚本

CREATE TABLE IF NOT EXISTS rpa_spider_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64),
    tax_no VARCHAR(64),
    usc_code VARCHAR(64),
    app_date DATE,
    status VARCHAR(32),
    error_message TEXT,
    collection_id BIGINT,
    parsing_id BIGINT,
    processing_id BIGINT,
    query_id BIGINT,
    callback_url VARCHAR(512),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rpa_indicator_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tax_no VARCHAR(64),
    usc_code VARCHAR(64),
    app_date DATE,
    collected_payload TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rpa_indicator_parsing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT,
    tax_no VARCHAR(64),
    usc_code VARCHAR(64),
    app_date DATE,
    parsed_data TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rpa_indicator_processing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parsing_id BIGINT,
    tax_no VARCHAR(64),
    usc_code VARCHAR(64),
    app_date DATE,
    processed_result TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS rpa_indicator_query (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT,
    parsing_id BIGINT,
    processing_id BIGINT,
    tax_no VARCHAR(64),
    usc_code VARCHAR(64),
    app_date DATE,
    business_json TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
