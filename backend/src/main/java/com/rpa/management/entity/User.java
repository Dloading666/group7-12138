package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import com.rpa.management.common.enums.UserStatus;
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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_role_id", columnList = "roleId"),
    @Index(name = "idx_users_status", columnList = "status")
})
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 64)
    private String realName;

    @Column(length = 128)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String avatar;

    @Column(nullable = false)
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "super_admin", nullable = false)
    private boolean superAdmin = false;

    @Column(length = 50)
    private String lastLoginIp;

    private LocalDateTime lastLoginAt;
}
