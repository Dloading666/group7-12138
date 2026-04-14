-- 流程管理数据库初始化脚本

-- 1. 创建节点类型表
CREATE TABLE IF NOT EXISTS node_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) UNIQUE NOT NULL COMMENT '节点类型代码',
    name VARCHAR(100) NOT NULL COMMENT '节点类型名称',
    icon VARCHAR(100) COMMENT '图标名称',
    color VARCHAR(20) COMMENT '图标颜色',
    category VARCHAR(50) COMMENT '分类',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    default_config TEXT COMMENT '默认配置JSON',
    description VARCHAR(500) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点类型表';

-- 2. 创建流程表
CREATE TABLE IF NOT EXISTS workflow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_code VARCHAR(100) UNIQUE NOT NULL COMMENT '流程编号',
    name VARCHAR(200) NOT NULL COMMENT '流程名称',
    description VARCHAR(1000) COMMENT '流程描述',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态：draft草稿/published已发布/archived已归档',
    version INT DEFAULT 1 COMMENT '版本号',
    user_id BIGINT COMMENT '创建用户ID',
    user_name VARCHAR(100) COMMENT '创建用户名',
    publish_time DATETIME COMMENT '发布时间',
    config TEXT COMMENT '流程配置JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- 3. 创建流程节点表
CREATE TABLE IF NOT EXISTS workflow_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_id BIGINT NOT NULL COMMENT '所属流程ID',
    node_type_id BIGINT COMMENT '节点类型ID',
    node_type VARCHAR(50) COMMENT '节点类型代码',
    name VARCHAR(200) NOT NULL COMMENT '节点名称',
    description VARCHAR(500) COMMENT '节点描述',
    x INT COMMENT 'X坐标',
    y INT COMMENT 'Y坐标',
    config TEXT COMMENT '节点配置JSON',
    timeout INT DEFAULT 60 COMMENT '超时时间（秒）',
    retry_count INT DEFAULT 3 COMMENT '重试次数',
    `order` INT DEFAULT 0 COMMENT '执行顺序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow_id (workflow_id),
    FOREIGN KEY (workflow_id) REFERENCES workflow(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

-- 4. 插入默认节点类型数据
INSERT INTO node_type (type, name, icon, color, category, sort_order, enabled, description) VALUES
('start', '开始节点', 'VideoPlay', '#52c41a', '基础节点', 1, TRUE, '流程的起始节点'),
('end', '结束节点', 'VideoPause', '#ff4d4f', '基础节点', 2, TRUE, '流程的结束节点'),
('task', '任务节点', 'Document', '#1890ff', '任务节点', 3, TRUE, '执行具体的爬虫任务'),
('condition', '条件判断', 'Share', '#faad14', '逻辑节点', 4, TRUE, '根据条件分支执行'),
('parallel', '并行执行', 'Grid', '#722ed1', '逻辑节点', 5, TRUE, '多个分支并行执行'),
('robot', '机器人节点', 'Cpu', '#13c2c2', '机器人节点', 6, TRUE, '调用机器人执行任务'),
('loop', '循环节点', 'Refresh', '#eb2f96', '逻辑节点', 7, TRUE, '循环执行某段流程'),
('delay', '延时节点', 'Clock', '#fa8c16', '工具节点', 8, TRUE, '等待指定时间'),
('notify', '通知节点', 'Bell', '#2f54eb', '工具节点', 9, TRUE, '发送通知消息'),
('webhook', 'Webhook节点', 'Link', '#1890ff', '工具节点', 10, TRUE, '调用外部API');

-- 5. 插入示例流程数据（可选）
INSERT INTO workflow (workflow_code, name, description, status, version, user_id, user_name) VALUES
('WF00000001', '电商数据采集流程', '采集京东、淘宝商品价格数据的流程', 'published', 1, 1, 'admin'),
('WF00000002', '数据同步流程', '定时同步数据库数据的流程', 'draft', 1, 1, 'admin');
