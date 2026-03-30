package com.rpa.management.config;

import com.rpa.management.common.enums.ExecuteType;
import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.enums.PermissionType;
import com.rpa.management.common.enums.RobotStatus;
import com.rpa.management.common.enums.RoleStatus;
import com.rpa.management.common.enums.TaskPriority;
import com.rpa.management.common.enums.TaskStatus;
import com.rpa.management.common.enums.UserStatus;
import com.rpa.management.entity.Permission;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.RolePermission;
import com.rpa.management.entity.Robot;
import com.rpa.management.entity.Task;
import com.rpa.management.entity.User;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RolePermissionRepository;
import com.rpa.management.repository.RoleRepository;
import com.rpa.management.repository.RobotRepository;
import com.rpa.management.repository.TaskRepository;
import com.rpa.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final RobotRepository robotRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Permission> permissionMap = seedPermissions();
        Role adminRole = seedAdminRole(permissionMap);
        Role operatorRole = seedOperatorRole(permissionMap);
        seedUsers(adminRole, operatorRole);
        seedRobots();
        seedTasks();
    }

    private Map<String, Permission> seedPermissions() {
        Map<String, Permission> permissionMap = new LinkedHashMap<>();
        if (!permissionRepository.existsByCode("dashboard:view")) {
            savePermission(permissionMap, "dashboard:view", "仪表盘", PermissionType.MENU, null, "/dashboard", "Dashboard", "HomeFilled", 1);
            savePermission(permissionMap, "system:view", "系统管理", PermissionType.MENU, null, "/system", "Layout", "Setting", 2);
            savePermission(permissionMap, "system:user:view", "用户管理", PermissionType.MENU, "system:view", "/system/users", "system/user/index", "User", 1);
            savePermission(permissionMap, "system:user:create", "新增用户", PermissionType.BUTTON, "system:user:view", null, null, null, 1);
            savePermission(permissionMap, "system:user:update", "编辑用户", PermissionType.BUTTON, "system:user:view", null, null, null, 2);
            savePermission(permissionMap, "system:user:delete", "删除用户", PermissionType.BUTTON, "system:user:view", null, null, null, 3);
            savePermission(permissionMap, "system:user:status", "启停用户", PermissionType.BUTTON, "system:user:view", null, null, null, 4);
            savePermission(permissionMap, "system:user:reset-password", "重置密码", PermissionType.BUTTON, "system:user:view", null, null, null, 5);
            savePermission(permissionMap, "system:user:assign-scope", "权限范围", PermissionType.BUTTON, "system:user:view", null, null, null, 6);

            savePermission(permissionMap, "system:role:view", "角色管理", PermissionType.MENU, "system:view", "/system/roles", "system/role/index", "UserFilled", 2);
            savePermission(permissionMap, "system:role:create", "新增角色", PermissionType.BUTTON, "system:role:view", null, null, null, 1);
            savePermission(permissionMap, "system:role:update", "编辑角色", PermissionType.BUTTON, "system:role:view", null, null, null, 2);
            savePermission(permissionMap, "system:role:delete", "删除角色", PermissionType.BUTTON, "system:role:view", null, null, null, 3);
            savePermission(permissionMap, "system:role:assign-permissions", "分配权限", PermissionType.BUTTON, "system:role:view", null, null, null, 4);

            savePermission(permissionMap, "system:permission:view", "权限管理", PermissionType.MENU, "system:view", "/system/permissions", "system/permission/index", "Lock", 3);
            savePermission(permissionMap, "system:permission:create", "新增权限", PermissionType.BUTTON, "system:permission:view", null, null, null, 1);
            savePermission(permissionMap, "system:permission:update", "编辑权限", PermissionType.BUTTON, "system:permission:view", null, null, null, 2);
            savePermission(permissionMap, "system:permission:status", "启停权限", PermissionType.BUTTON, "system:permission:view", null, null, null, 3);
            savePermission(permissionMap, "system:permission:delete", "删除权限", PermissionType.BUTTON, "system:permission:view", null, null, null, 4);

            savePermission(permissionMap, "task:view", "任务管理", PermissionType.MENU, null, "/tasks", "task/index", "List", 3);
            savePermission(permissionMap, "task:create", "创建任务", PermissionType.BUTTON, "task:view", null, null, null, 1);
            savePermission(permissionMap, "task:update", "编辑任务", PermissionType.BUTTON, "task:view", null, null, null, 2);
            savePermission(permissionMap, "task:delete", "删除任务", PermissionType.BUTTON, "task:view", null, null, null, 3);
            savePermission(permissionMap, "task:start", "启动任务", PermissionType.BUTTON, "task:view", null, null, null, 4);
            savePermission(permissionMap, "task:stop", "停止任务", PermissionType.BUTTON, "task:view", null, null, null, 5);

            savePermission(permissionMap, "robot:view", "机器人管理", PermissionType.MENU, null, "/robots", "robot/index", "Cpu", 4);
            savePermission(permissionMap, "robot:create", "新增机器人", PermissionType.BUTTON, "robot:view", null, null, null, 1);
            savePermission(permissionMap, "robot:update", "编辑机器人", PermissionType.BUTTON, "robot:view", null, null, null, 2);
            savePermission(permissionMap, "robot:delete", "删除机器人", PermissionType.BUTTON, "robot:view", null, null, null, 3);
            savePermission(permissionMap, "robot:start", "启动机器人", PermissionType.BUTTON, "robot:view", null, null, null, 4);
            savePermission(permissionMap, "robot:stop", "停止机器人", PermissionType.BUTTON, "robot:view", null, null, null, 5);

            savePermission(permissionMap, "workflow:view", "流程定义与设计", PermissionType.MENU, null, "/workflow", "workflow/index", "Share", 5);
            savePermission(permissionMap, "workflow:design", "流程设计", PermissionType.MENU, "workflow:view", "/workflow/design", "workflow/design", "Edit", 1);

            savePermission(permissionMap, "monitor:view", "执行监控与记录", PermissionType.MENU, null, "/monitor", "monitor/index", "Monitor", 6);
            savePermission(permissionMap, "monitor:log", "执行日志", PermissionType.MENU, "monitor:view", "/monitor/logs", "monitor/logs", "Document", 1);

            savePermission(permissionMap, "statistics:view", "数据查询与统计", PermissionType.MENU, null, "/statistics", "statistics/index", "DataAnalysis", 7);
            savePermission(permissionMap, "statistics:query", "数据查询", PermissionType.MENU, "statistics:view", "/statistics/query", "statistics/query", "Search", 1);

            savePermission(permissionMap, "settings:view", "系统设置", PermissionType.MENU, null, "/settings", "settings/index", "Tools", 8);
            savePermission(permissionMap, "settings:basic", "基础设置", PermissionType.MENU, "settings:view", "/settings/basic", "settings/basic", "Setting", 1);
            savePermission(permissionMap, "settings:notification", "通知设置", PermissionType.MENU, "settings:view", "/settings/notification", "settings/notification", "Bell", 2);
        } else {
            for (Permission permission : permissionRepository.findAll()) {
                permissionMap.put(permission.getCode(), permission);
            }
        }
        return permissionMap;
    }

    private void savePermission(Map<String, Permission> permissionMap,
                                String code,
                                String name,
                                PermissionType type,
                                String parentCode,
                                String path,
                                String component,
                                String icon,
                                int sortOrder) {
        if (permissionMap.containsKey(code)) {
            return;
        }
        Permission permission = new Permission()
            .setCode(code)
            .setName(name)
            .setType(type)
            .setParentId(parentCode == null ? null : permissionMap.get(parentCode).getId())
            .setPath(path)
            .setComponent(component)
            .setIcon(icon)
            .setSortOrder(sortOrder)
            .setStatus(PermissionStatus.ACTIVE);
        permission = permissionRepository.save(permission);
        permissionMap.put(code, permission);
    }

    private Role seedAdminRole(Map<String, Permission> permissionMap) {
        Role role = roleRepository.findByCode("ADMIN").orElseGet(() -> roleRepository.save(new Role()
            .setName("管理员")
            .setCode("ADMIN")
            .setDescription("系统固定超级管理员")
            .setStatus(RoleStatus.ACTIVE)
            .setBuiltIn(true)));
        rolePermissionRepository.deleteAllByRoleId(role.getId());
        for (Permission permission : permissionMap.values()) {
            rolePermissionRepository.save(RolePermission.of(role.getId(), permission.getId()));
        }
        return role;
    }

    private Role seedOperatorRole(Map<String, Permission> permissionMap) {
        Role role = roleRepository.findByCode("OPERATOR").orElseGet(() -> roleRepository.save(new Role()
            .setName("运营人员")
            .setCode("OPERATOR")
            .setDescription("普通业务角色")
            .setStatus(RoleStatus.ACTIVE)
            .setBuiltIn(false)));
        rolePermissionRepository.deleteAllByRoleId(role.getId());
        List<String> codes = List.of(
            "dashboard:view",
            "task:view",
            "task:create",
            "task:update",
            "task:start",
            "task:stop",
            "robot:view",
            "robot:start",
            "robot:stop",
            "workflow:view",
            "monitor:view",
            "statistics:view",
            "settings:view"
        );
        for (String code : codes) {
            Permission permission = permissionMap.get(code);
            if (permission != null) {
                rolePermissionRepository.save(RolePermission.of(role.getId(), permission.getId()));
            }
        }
        return role;
    }

    private void seedUsers(Role adminRole, Role operatorRole) {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User()
                .setUsername("admin")
                .setPassword(passwordEncoder.encode("admin123"))
                .setRealName("系统管理员")
                .setEmail("admin@example.com")
                .setPhone("13800138000")
                .setRoleId(adminRole.getId())
                .setStatus(UserStatus.ACTIVE)
                .setSuperAdmin(true)
                .setLastLoginAt(LocalDateTime.now().minusDays(1))
                .setLastLoginIp("127.0.0.1"));
        }
        if (!userRepository.existsByUsername("user01")) {
            userRepository.save(new User()
                .setUsername("user01")
                .setPassword(passwordEncoder.encode("user123"))
                .setRealName("张三")
                .setEmail("zhangsan@example.com")
                .setPhone("13900139001")
                .setRoleId(operatorRole.getId())
                .setStatus(UserStatus.ACTIVE)
                .setSuperAdmin(false));
        }
        if (!userRepository.existsByUsername("user02")) {
            userRepository.save(new User()
                .setUsername("user02")
                .setPassword(passwordEncoder.encode("user123"))
                .setRealName("李四")
                .setEmail("lisi@example.com")
                .setPhone("13900139002")
                .setRoleId(operatorRole.getId())
                .setStatus(UserStatus.ACTIVE)
                .setSuperAdmin(false));
        }
    }

    private void seedRobots() {
        if (robotRepository.count() > 0) {
            return;
        }
        robotRepository.save(new Robot()
            .setName("Robot-01")
            .setType("数据采集")
            .setStatus(RobotStatus.ONLINE)
            .setIpAddress("192.168.1.101")
            .setPort(8080)
            .setTaskCount(128)
            .setSuccessRate(new BigDecimal("98.00"))
            .setLastHeartbeat(LocalDateTime.now().minusMinutes(5)));
        robotRepository.save(new Robot()
            .setName("Robot-02")
            .setType("报表生成")
            .setStatus(RobotStatus.ONLINE)
            .setIpAddress("192.168.1.102")
            .setPort(8080)
            .setTaskCount(85)
            .setSuccessRate(new BigDecimal("100.00"))
            .setLastHeartbeat(LocalDateTime.now().minusMinutes(10)));
        robotRepository.save(new Robot()
            .setName("Robot-03")
            .setType("文件处理")
            .setStatus(RobotStatus.OFFLINE)
            .setIpAddress("192.168.1.103")
            .setPort(8080)
            .setTaskCount(42)
            .setSuccessRate(new BigDecimal("95.00"))
            .setLastHeartbeat(LocalDateTime.now().minusHours(2)));
    }

    private void seedTasks() {
        if (taskRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        saveTask("T001", "数据采集任务A", "数据采集", TaskStatus.RUNNING, 75, TaskPriority.HIGH, ExecuteType.IMMEDIATE, 1L, 1L, now.minusDays(6));
        saveTask("T002", "报表生成任务B", "报表生成", TaskStatus.COMPLETED, 100, TaskPriority.MEDIUM, ExecuteType.IMMEDIATE, 2L, 1L, now.minusDays(4));
        saveTask("T003", "文件处理任务C", "文件处理", TaskStatus.PENDING, 0, TaskPriority.LOW, ExecuteType.SCHEDULED, null, 2L, now.minusDays(2));
        saveTask("T004", "数据同步任务D", "数据同步", TaskStatus.FAILED, 45, TaskPriority.HIGH, ExecuteType.IMMEDIATE, 1L, 2L, now.minusDays(1));
    }

    private void saveTask(String taskNo,
                          String name,
                          String type,
                          TaskStatus status,
                          int progress,
                          TaskPriority priority,
                          ExecuteType executeType,
                          Long robotId,
                          Long createdByUserId,
                          LocalDateTime createdAt) {
        Task task = new Task()
            .setTaskNo(taskNo)
            .setName(name)
            .setType(type)
            .setStatus(status)
            .setProgress(progress)
            .setPriority(priority)
            .setExecuteType(executeType)
            .setRobotId(robotId)
            .setCreatedByUserId(createdByUserId)
            .setStartTime(status == TaskStatus.RUNNING ? createdAt.plusHours(1) : null)
            .setEndTime(status == TaskStatus.COMPLETED || status == TaskStatus.FAILED ? createdAt.plusHours(2) : null)
            .setDuration(status == TaskStatus.RUNNING ? 3600 : 1800);
        task.setCreatedAt(createdAt);
        task.setUpdatedAt(createdAt);
        taskRepository.save(task);
    }
}
