package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.common.enums.RoleStatus;
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
@Table(name = "roles", indexes = {
    @Index(name = "idx_roles_code", columnList = "code"),
    @Index(name = "idx_roles_status", columnList = "status")
})
public class Role extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleStatus status = RoleStatus.ACTIVE;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn = false;
}
