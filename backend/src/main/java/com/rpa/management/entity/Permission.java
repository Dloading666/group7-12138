package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.enums.PermissionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_code", columnList = "code"),
    @Index(name = "idx_permissions_parent_id", columnList = "parentId"),
    @Index(name = "idx_permissions_type", columnList = "type")
})
public class Permission extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false, length = 20)
    private PermissionType type;

    private Long parentId;

    @Column(length = 255)
    private String path;

    @Column(length = 255)
    private String component;

    @Column(length = 64)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PermissionStatus status = PermissionStatus.ACTIVE;
}
