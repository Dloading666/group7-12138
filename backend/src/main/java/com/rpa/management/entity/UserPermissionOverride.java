package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.common.enums.OverrideMode;
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
@Table(name = "user_permission_overrides", indexes = {
    @Index(name = "idx_user_permission_overrides_user_id", columnList = "userId"),
    @Index(name = "idx_user_permission_overrides_permission_id", columnList = "permissionId")
})
public class UserPermissionOverride extends BaseEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long permissionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OverrideMode mode;

    public static UserPermissionOverride grant(Long userId, Long permissionId) {
        return new UserPermissionOverride().setUserId(userId).setPermissionId(permissionId).setMode(OverrideMode.GRANT);
    }

    public static UserPermissionOverride revoke(Long userId, Long permissionId) {
        return new UserPermissionOverride().setUserId(userId).setPermissionId(permissionId).setMode(OverrideMode.REVOKE);
    }
}
