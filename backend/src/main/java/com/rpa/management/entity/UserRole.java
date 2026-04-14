package com.rpa.management.entity;

/**
 * 用户角色枚举
 */
public enum UserRole {
    /**
     * 管理员 - 拥有所有权限
     */
    ADMIN("管理员", "拥有系统所有权限"),
    
    /**
     * 普通用户 - 拥有基本权限
     */
    USER("普通用户", "拥有基本操作权限");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
