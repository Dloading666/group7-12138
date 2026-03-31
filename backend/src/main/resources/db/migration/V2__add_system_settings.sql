CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_group VARCHAR(64) NOT NULL,
    setting_key VARCHAR(64) NOT NULL,
    setting_value LONGTEXT,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (setting_group, setting_key)
);

CREATE INDEX idx_system_settings_group ON system_settings (setting_group);
