package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "system_settings", indexes = {
    @Index(name = "idx_system_settings_group", columnList = "setting_group")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_system_settings_group_key", columnNames = {"setting_group", "setting_key"})
})
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_group", nullable = false, length = 64)
    private String settingGroup;

    @Column(name = "setting_key", nullable = false, length = 64)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "LONGTEXT")
    private String settingValue;

    @Column(name = "description", length = 255)
    private String description;
}
