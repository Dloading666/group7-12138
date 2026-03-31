package com.rpa.management.security;

public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String DASHBOARD_VIEW = "dashboard:view";

    public static final String USER_VIEW = "system:user:view";
    public static final String USER_CREATE = "system:user:create";
    public static final String USER_UPDATE = "system:user:update";
    public static final String USER_DELETE = "system:user:delete";
    public static final String USER_STATUS = "system:user:status";
    public static final String USER_RESET_PASSWORD = "system:user:reset-password";
    public static final String USER_ASSIGN_SCOPE = "system:user:assign-scope";

    public static final String ROLE_VIEW = "system:role:view";
    public static final String ROLE_CREATE = "system:role:create";
    public static final String ROLE_UPDATE = "system:role:update";
    public static final String ROLE_DELETE = "system:role:delete";
    public static final String ROLE_ASSIGN_PERMISSIONS = "system:role:assign-permissions";

    public static final String PERMISSION_VIEW = "system:permission:view";
    public static final String PERMISSION_CREATE = "system:permission:create";
    public static final String PERMISSION_UPDATE = "system:permission:update";
    public static final String PERMISSION_DELETE = "system:permission:delete";
    public static final String PERMISSION_STATUS = "system:permission:status";

    public static final String MONITOR_VIEW = "monitor:view";
    public static final String MONITOR_LOG = "monitor:log";

    public static final String STATISTICS_VIEW = "statistics:view";
    public static final String STATISTICS_QUERY = "statistics:query";

    public static final String SETTINGS_VIEW = "settings:view";
    public static final String SETTINGS_BASIC = "settings:basic";
    public static final String SETTINGS_NOTIFICATION = "settings:notification";

    public static final String TASK_VIEW = "task:view";
    public static final String TASK_CREATE = "task:create";
    public static final String TASK_UPDATE = "task:update";
    public static final String TASK_DELETE = "task:delete";
    public static final String TASK_START = "task:start";
    public static final String TASK_STOP = "task:stop";

    public static final String ROBOT_VIEW = "robot:view";
    public static final String ROBOT_CREATE = "robot:create";
    public static final String ROBOT_UPDATE = "robot:update";
    public static final String ROBOT_DELETE = "robot:delete";
    public static final String ROBOT_START = "robot:start";
    public static final String ROBOT_STOP = "robot:stop";

    public static final String WORKFLOW_VIEW = "workflow:view";
    public static final String WORKFLOW_DESIGN = "workflow:design";
}
