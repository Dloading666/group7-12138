CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    built_in BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (code)
);

CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(128) NOT NULL,
    permission_type VARCHAR(20) NOT NULL,
    parent_id BIGINT,
    path VARCHAR(255),
    component VARCHAR(255),
    icon VARCHAR(64),
    sort_order INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (code)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(32),
    avatar VARCHAR(255),
    role_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    super_admin BOOLEAN NOT NULL,
    last_login_ip VARCHAR(50),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (username)
);

CREATE TABLE role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE user_permission_overrides (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    mode VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, permission_id)
);

CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    task_type VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    progress INT NOT NULL,
    priority VARCHAR(20) NOT NULL,
    execute_type VARCHAR(20) NOT NULL,
    schedule_time TIMESTAMP,
    robot_id BIGINT,
    created_by_user_id BIGINT,
    params LONGTEXT,
    result LONGTEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    duration INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (task_no)
);

CREATE TABLE robots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    robot_type VARCHAR(64),
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(64),
    port INT,
    config LONGTEXT,
    last_heartbeat TIMESTAMP,
    task_count INT,
    success_rate DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE execution_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    robot_id BIGINT,
    level VARCHAR(32),
    message LONGTEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    operation VARCHAR(128),
    method VARCHAR(255),
    params LONGTEXT,
    ip VARCHAR(64),
    status VARCHAR(32),
    error_msg LONGTEXT,
    duration BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
