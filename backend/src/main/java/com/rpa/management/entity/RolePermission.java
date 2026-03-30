package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_permissions_role_id", columnList = "roleId"),
    @Index(name = "idx_role_permissions_permission_id", columnList = "permissionId")
})
public class RolePermission extends BaseEntity {

    @Column(nullable = false)
    private Long roleId;

    @Column(nullable = false)
    private Long permissionId;

    public static RolePermission of(Long roleId, Long permissionId) {
        return new RolePermission().setRoleId(roleId).setPermissionId(permissionId);
    }
}
