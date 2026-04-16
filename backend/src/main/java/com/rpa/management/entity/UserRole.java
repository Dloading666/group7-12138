package com.rpa.management.entity;

/**
 * 用户角色枚举
 */
public enum UserRole {
    /**
     * 管理员 - 拥有系统全部权限
     */
    ADMIN("管理员", "拥有系统全部权限"),

    /**
     * 普通用户 - 拥有管理员分配的业务权限
     */
    USER("普通用户", "拥有管理员分配的业务权限"),

    /**
     * 访客 - 默认只拥有管理员分配的可访问权限
     */
    GUEST("访客", "默认只拥有管理员分配的可访问权限");

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
