package com.rpa.management.config;

import java.time.LocalDateTime;
import java.util.List;

import com.rpa.management.entity.*;
import com.rpa.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据初始化组件
 * 应用启动时自动创建管理员账号和默认角色
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RobotRepository robotRepository;
    private final NodeTypeRepository nodeTypeRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final CollectConfigRepository collectConfigRepository;
    private final CollectDataRepository collectDataRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========== 开始初始化数据 ==========");

        // 初始化默认角色
        initRoles();
        initPermissions();

        // 为管理员角色分配所有权限
        assignAdminPermissions();

        // 初始化管理员账号
        initAdminUser();

        // 初始化测试用户
        initTestUsers();

        // 初始化测试机器人
        initTestRobots();

        // 初始化节点类型
        initNodeTypes();

        // 初始化工作流
        initWorkflows();

        // 初始化工作流节点
        initWorkflowNodes();

        // 初始化采集任务配置
        // initCollectConfigs();

        // 初始化采集数据
        // initCollectData();

        log.info("========== 数据初始化完成 ==========");
    }

    /**
     * 初始化测试机器人
     */
    private void initTestRobots() {
        // 检查是否已有机器人数据
        if (robotRepository.count() > 0) {
            log.info("✅ 机器人数据已存在，跳过初始化");
            return;
        }

        // 插入测试机器人数据
        Robot robot1 = new Robot();
        robot1.setRobotCode("DC-001");
        robot1.setName("数据采集机器人-1");
        robot1.setType("data_collector");
        robot1.setDescription("自动采集网站数据");
        robot1.setStatus("online");
        robot1.setTotalTasks(128L);
        robot1.setSuccessTasks(125L);
        robot1.setFailedTasks(3L);
        robot1.setSuccessRate(97.66);
        robotRepository.save(robot1);

        Robot robot2 = new Robot();
        robot2.setRobotCode("RG-001");
        robot2.setName("报表生成机器人-1");
        robot2.setType("report_generator");
        robot2.setDescription("自动生成业务报表");
        robot2.setStatus("online");
        robot2.setTotalTasks(85L);
        robot2.setSuccessTasks(85L);
        robot2.setFailedTasks(0L);
        robot2.setSuccessRate(100.0);
        robotRepository.save(robot2);

        Robot robot3 = new Robot();
        robot3.setRobotCode("TS-001");
        robot3.setName("任务调度机器人-1");
        robot3.setType("task_scheduler");
        robot3.setDescription("定时任务调度管理");
        robot3.setStatus("offline");
        robot3.setTotalTasks(42L);
        robot3.setSuccessTasks(40L);
        robot3.setFailedTasks(2L);
        robot3.setSuccessRate(95.24);
        robotRepository.save(robot3);

        Robot robot4 = new Robot();
        robot4.setRobotCode("NT-001");
        robot4.setName("消息通知机器人-1");
        robot4.setType("notification");
        robot4.setDescription("发送消息通知");
        robot4.setStatus("offline");
        robot4.setTotalTasks(0L);
        robot4.setSuccessTasks(0L);
        robot4.setFailedTasks(0L);
        robot4.setSuccessRate(0.0);
        robotRepository.save(robot4);

        Robot robot5 = new Robot();
        robot5.setRobotCode("FP-001");
        robot5.setName("文件处理机器人-1");
        robot5.setType("file_processor");
        robot5.setDescription("处理文件转换");
        robot5.setStatus("online");
        robot5.setTotalTasks(56L);
        robot5.setSuccessTasks(54L);
        robot5.setFailedTasks(2L);
        robot5.setSuccessRate(96.43);
        robotRepository.save(robot5);

        log.info("✅ 机器人测试数据初始化完成，共插入 {} 条", robotRepository.count());
    }

    /**
     * 修复数据库中角色字段值（小写转大写）
     */
    private void fixRoleData() {
        // 使用原生SQL更新角色值
        userRepository.updateRoleToUppercase();
        log.info("✅ 角色数据修复完成");
    }

    /**
     * 为管理员角色分配所有权限
     */
    private void assignAdminPermissions() {
        Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
        if (adminRole == null) {
            log.warn("⚠️ 管理员角色不存在，跳过权限分配");
            return;
        }

        // 获取所有权限
        List<Permission> allPermissions = permissionRepository.findAll();
        if (allPermissions.isEmpty()) {
            log.info("✅ 权限表为空，跳过权限分配");
            return;
        }

        // 如果角色已有权限且数量一致，不重复分配
        if (adminRole.getPermissions() != null && adminRole.getPermissions().size() >= allPermissions.size()) {
            log.info("✅ 管理员角色已有权限，跳过分配");
            return;
        }

        // 为管理员角色分配所有权限（List转Set）
        if (adminRole.getPermissions() == null) {
            adminRole.setPermissions(new java.util.HashSet<>());
        }
        adminRole.getPermissions().addAll(allPermissions);
        roleRepository.save(adminRole);
        log.info("✅ 已为管理员角色分配所有权限，共 {} 个", allPermissions.size());
    }

    /**
     * 初始化默认角色
     */
    private void initRoles() {
        // 管理员角色
        if (!roleRepository.existsByCode("ADMIN")) {
            Role adminRole = new Role();
            adminRole.setName("管理员");
            adminRole.setCode("ADMIN");
            adminRole.setDescription("系统管理员，拥有所有权限");
            adminRole.setStatus("active");
            adminRole.setSortOrder(1);
            roleRepository.save(adminRole);
            log.info("✅ 管理员角色创建成功");
        }

        // 普通用户角色
        if (!roleRepository.existsByCode("USER")) {
            Role userRole = new Role();
            userRole.setName("普通用户");
            userRole.setCode("USER");
            userRole.setDescription("普通用户，拥有只读权限，可查看系统仪表盘、任务执行记录、采集数据等");
            userRole.setStatus("active");
            userRole.setSortOrder(2);
            roleRepository.save(userRole);
            log.info("✅ 普通用户角色创建成功");
        }

        // 访客角色
        if (!roleRepository.existsByCode("GUEST")) {
            Role guestRole = new Role();
            guestRole.setName("访客");
            guestRole.setCode("GUEST");
            guestRole.setDescription("访客用户，只有基本查看权限");
            guestRole.setStatus("active");
            guestRole.setSortOrder(3);
            roleRepository.save(guestRole);
            log.info("✅ 访客角色创建成功");
        }
    }

    /**
     * 初始化管理员账号
     */
    private void initPermissions() {
        Permission system = ensurePermission("系统管理", "system:view", "menu", 0L, "/system", "Setting", 1, "系统管理模块");
        Permission systemUser = ensurePermission("用户管理", "system:user:view", "menu", system.getId(), "/system/user", "User", 1, "用户管理");
        ensurePermission("查看用户", "system:user:detail", "button", systemUser.getId(), null, null, 1, "查看用户详情");
        ensurePermission("新增用户", "system:user:add", "button", systemUser.getId(), null, null, 2, "新增用户");
        ensurePermission("编辑用户", "system:user:edit", "button", systemUser.getId(), null, null, 3, "编辑用户");
        ensurePermission("删除用户", "system:user:delete", "button", systemUser.getId(), null, null, 4, "删除用户");

        Permission systemRole = ensurePermission("角色管理", "system:role:view", "menu", system.getId(), "/system/role", "UserFilled", 2, "角色管理");
        ensurePermission("查看角色", "system:role:detail", "button", systemRole.getId(), null, null, 1, "查看角色详情");
        ensurePermission("新增角色", "system:role:add", "button", systemRole.getId(), null, null, 2, "新增角色");
        ensurePermission("编辑角色", "system:role:edit", "button", systemRole.getId(), null, null, 3, "编辑角色");
        ensurePermission("删除角色", "system:role:delete", "button", systemRole.getId(), null, null, 4, "删除角色");
        ensurePermission("分配权限", "system:role:permission", "button", systemRole.getId(), null, null, 5, "分配权限");
        ensurePermission("权限管理", "system:permission:view", "menu", system.getId(), "/system/permission", "Key", 3, "权限管理");

        Permission task = ensurePermission("任务管理", "task:view", "menu", 0L, "/task", "List", 2, "任务管理模块");
        Permission taskList = ensurePermission("任务列表", "task:list", "menu", task.getId(), "/task/list", "Document", 1, "任务列表");
        ensurePermission("创建任务", "task:create", "menu", task.getId(), "/task/create", "Plus", 2, "创建任务");
        ensurePermission("任务历史", "task:history", "menu", task.getId(), "/task/history", "Clock", 3, "任务历史");
        ensurePermission("启动任务", "task:start", "button", taskList.getId(), null, null, 1, "启动任务");
        ensurePermission("停止任务", "task:stop", "button", taskList.getId(), null, null, 2, "停止任务");

        Permission workflow = ensurePermission("流程管理", "workflow:view", "menu", 0L, "/workflow", "Share", 3, "流程管理模块");
        ensurePermission("流程列表", "workflow:list", "menu", workflow.getId(), "/workflow/list", "Document", 1, "流程列表");
        ensurePermission("流程设计", "workflow:create", "menu", workflow.getId(), "/workflow/design", "Edit", 2, "流程设计");

        Permission robot = ensurePermission("机器人管理", "robot:view", "menu", 0L, "/robot", "Cpu", 4, "机器人管理模块");
        Permission robotList = ensurePermission("机器人列表", "robot:list", "menu", robot.getId(), "/robot/list", "Monitor", 1, "机器人列表");
        ensurePermission("机器人配置", "robot:create", "menu", robot.getId(), "/robot/config", "Setting", 2, "机器人配置");
        ensurePermission("启动机器人", "robot:start", "button", robotList.getId(), null, null, 1, "启动机器人");
        ensurePermission("停止机器人", "robot:stop", "button", robotList.getId(), null, null, 2, "停止机器人");

        Permission monitor = ensurePermission("数据管理", "monitor:view", "menu", 0L, "/monitor", "View", 5, "数据管理模块");
        ensurePermission("数据采集", "monitor:realtime", "menu", monitor.getId(), "/monitor/realtime", "DataLine", 1, "数据采集");
        ensurePermission("数据解析", "monitor:logs", "menu", monitor.getId(), "/monitor/logs", "Tickets", 2, "数据解析");

        Permission statistics = ensurePermission("数据统计", "statistics:view", "menu", 0L, "/statistics", "DataAnalysis", 6, "数据统计模块");
        ensurePermission("数据查询", "statistics:query", "menu", statistics.getId(), "/statistics/query", "Search", 1, "数据查询");
        ensurePermission("统计报表", "statistics:report", "menu", statistics.getId(), "/statistics/report", "DataBoard", 2, "统计报表");

        Permission settings = ensurePermission("系统设置", "settings:view", "menu", 0L, "/settings", "Tools", 7, "系统设置模块");
        ensurePermission("基础设置", "settings:basic:view", "menu", settings.getId(), "/settings/basic", "Setting", 1, "基础设置");
        ensurePermission("通知设置", "settings:notification:view", "menu", settings.getId(), "/settings/notification", "Bell", 2, "通知设置");
    }

    private Permission ensurePermission(String name, String code, String type, Long parentId,
                                        String path, String icon, int sortOrder, String description) {
        Permission existing = permissionRepository.findByCode(code);
        if (existing != null) {
            boolean changed = false;

            if (!name.equals(existing.getName())) {
                existing.setName(name);
                changed = true;
            }
            if (!type.equals(existing.getType())) {
                existing.setType(type);
                changed = true;
            }
            Long expectedParentId = parentId == null ? 0L : parentId;
            Long currentParentId = existing.getParentId() == null ? 0L : existing.getParentId();
            if (!expectedParentId.equals(currentParentId)) {
                existing.setParentId(parentId);
                changed = true;
            }
            if (!java.util.Objects.equals(path, existing.getPath())) {
                existing.setPath(path);
                changed = true;
            }
            if (!java.util.Objects.equals(icon, existing.getIcon())) {
                existing.setIcon(icon);
                changed = true;
            }
            if (existing.getSortOrder() == null || existing.getSortOrder() != sortOrder) {
                existing.setSortOrder(sortOrder);
                changed = true;
            }
            if (!"active".equals(existing.getStatus())) {
                existing.setStatus("active");
                changed = true;
            }
            if (!java.util.Objects.equals(description, existing.getDescription())) {
                existing.setDescription(description);
                changed = true;
            }

            return changed ? permissionRepository.save(existing) : existing;
        }

        Permission permission = new Permission();
        permission.setName(name);
        permission.setCode(code);
        permission.setType(type);
        permission.setParentId(parentId);
        permission.setPath(path);
        permission.setIcon(icon);
        permission.setSortOrder(sortOrder);
        permission.setStatus("active");
        permission.setDescription(description);
        return permissionRepository.save(permission);
    }

    private void initAdminUser() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setEmail("admin@example.com");
            admin.setPhone("13800138000");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus("active");

            userRepository.save(admin);
            log.info("✅ 管理员账号创建成功: admin / admin123");
        } else {
            log.info("✅ 管理员账号已存在，跳过创建");
        }
    }

    /**
     * 初始化测试用户
     */
    private void initTestUsers() {
        // 创建测试用户 user01
        if (!userRepository.existsByUsername("user01")) {
            User user01 = new User();
            user01.setUsername("user01");
            user01.setPassword(passwordEncoder.encode("user123"));
            user01.setRealName("张三");
            user01.setEmail("zhangsan@example.com");
            user01.setPhone("13900139001");
            user01.setRole(UserRole.USER);
            user01.setStatus("active");

            userRepository.save(user01);
            log.info("✅ 测试用户创建成功: user01 / user123");
        }

        // 创建测试用户 user02
        if (!userRepository.existsByUsername("user02")) {
            User user02 = new User();
            user02.setUsername("user02");
            user02.setPassword(passwordEncoder.encode("user123"));
            user02.setRealName("李四");
            user02.setEmail("lisi@example.com");
            user02.setPhone("13900139002");
            user02.setRole(UserRole.USER);
            user02.setStatus("active");

            userRepository.save(user02);
            log.info("✅ 测试用户创建成功: user02 / user123");
        }

        initGuestUser();
    }

    private void initGuestUser() {
        if (userRepository.existsByUsername("GUEST")) {
            log.info("✅ 访客账号已存在，跳过创建");
            return;
        }

        User guest = new User();
        guest.setUsername("GUEST");
        guest.setPassword(passwordEncoder.encode("guest123"));
        guest.setRealName("访客账号");
        guest.setEmail("guest@example.com");
        guest.setPhone("13900139003");
        guest.setRole(UserRole.GUEST);
        guest.setStatus("active");

        userRepository.save(guest);
        log.info("✅ 访客账号创建成功: GUEST / guest123");
    }

    /**
     * 初始化节点类型
     */
    private void initNodeTypes() {
        if (nodeTypeRepository.count() > 0) {
            log.info("✅ 节点类型已存在，跳过初始化");
            return;
        }

        NodeType startNode = new NodeType();
        startNode.setType("start");
        startNode.setName("开始");
        startNode.setIcon("VideoPlay");
        startNode.setColor("#67C23A");
        startNode.setCategory("基础");
        startNode.setSortOrder(1);
        startNode.setDescription("流程开始节点");
        nodeTypeRepository.save(startNode);

        NodeType endNode = new NodeType();
        endNode.setType("end");
        endNode.setName("结束");
        endNode.setIcon("VideoPause");
        endNode.setColor("#F56C6C");
        endNode.setCategory("基础");
        endNode.setSortOrder(2);
        endNode.setDescription("流程结束节点");
        nodeTypeRepository.save(endNode);

        NodeType httpNode = new NodeType();
        httpNode.setType("http");
        httpNode.setName("HTTP请求");
        httpNode.setIcon("Connection");
        httpNode.setColor("#409EFF");
        httpNode.setCategory("机器人");
        httpNode.setSortOrder(3);
        httpNode.setDescription("发送HTTP请求");
        nodeTypeRepository.save(httpNode);

        NodeType dataProcessNode = new NodeType();
        dataProcessNode.setType("data_process");
        dataProcessNode.setName("数据处理");
        dataProcessNode.setIcon("DataAnalysis");
        dataProcessNode.setColor("#E6A23C");
        dataProcessNode.setCategory("机器人");
        dataProcessNode.setSortOrder(4);
        dataProcessNode.setDescription("数据清洗和转换");
        nodeTypeRepository.save(dataProcessNode);

        NodeType conditionNode = new NodeType();
        conditionNode.setType("condition");
        conditionNode.setName("条件判断");
        conditionNode.setIcon("MostlyCreativeOpposite");
        conditionNode.setColor("#909399");
        conditionNode.setCategory("逻辑");
        conditionNode.setSortOrder(5);
        conditionNode.setDescription("根据条件分支执行");
        nodeTypeRepository.save(conditionNode);

        NodeType delayNode = new NodeType();
        delayNode.setType("delay");
        delayNode.setName("延时等待");
        delayNode.setIcon("Timer");
        delayNode.setColor("#9ACD32");
        delayNode.setCategory("逻辑");
        delayNode.setSortOrder(6);
        delayNode.setDescription("延时等待指定时间");
        nodeTypeRepository.save(delayNode);

        NodeType emailNode = new NodeType();
        emailNode.setType("email");
        emailNode.setName("邮件发送");
        emailNode.setIcon("Message");
        emailNode.setColor("#FF69B4");
        emailNode.setCategory("机器人");
        emailNode.setSortOrder(7);
        emailNode.setDescription("发送邮件通知");
        nodeTypeRepository.save(emailNode);

        log.info("✅ 节点类型初始化完成");
    }

    /**
     * 初始化工作流
     */
    private void initWorkflows() {
        if (workflowRepository.count() > 0) {
            log.info("✅ 工作流已存在，跳过初始化");
            return;
        }

        // 工作流1：数据采集自动处理流程
        Workflow wf1 = new Workflow();
        wf1.setWorkflowCode("WF-DATA-001");
        wf1.setName("数据采集自动处理流程");
        wf1.setDescription("自动采集网页数据并进行清洗处理，发送邮件通知");
        wf1.setStatus("published");
        wf1.setVersion(1);
        wf1.setUserId(1L);
        wf1.setUserName("admin");
        wf1.setPublishTime(LocalDateTime.now());
        wf1.setConfig("{\"nodes\":[],\"edges\":[]}");
        workflowRepository.save(wf1);

        // 工作流2：报表自动生成流程
        Workflow wf2 = new Workflow();
        wf2.setWorkflowCode("WF-REPORT-001");
        wf2.setName("报表自动生成流程");
        wf2.setDescription("定时生成业务报表，发送给相关人员");
        wf2.setStatus("published");
        wf2.setVersion(1);
        wf2.setUserId(1L);
        wf2.setUserName("admin");
        wf2.setPublishTime(LocalDateTime.now());
        wf2.setConfig("{\"nodes\":[],\"edges\":[]}");
        workflowRepository.save(wf2);

        // 工作流3：任务审批流程（草稿状态）
        Workflow wf3 = new Workflow();
        wf3.setWorkflowCode("WF-APPROVAL-001");
        wf3.setName("任务审批流程");
        wf3.setDescription("自动化任务审批流程");
        wf3.setStatus("draft");
        wf3.setVersion(1);
        wf3.setUserId(1L);
        wf3.setUserName("admin");
        wf3.setConfig("{\"nodes\":[],\"edges\":[]}");
        workflowRepository.save(wf3);

        // 工作流4：异常告警处理流程
        Workflow wf4 = new Workflow();
        wf4.setWorkflowCode("WF-ALERT-001");
        wf4.setName("异常告警处理流程");
        wf4.setDescription("监控系统异常并自动告警处理");
        wf4.setStatus("published");
        wf4.setVersion(2);
        wf4.setUserId(1L);
        wf4.setUserName("admin");
        wf4.setPublishTime(LocalDateTime.now());
        wf4.setConfig("{\"nodes\":[],\"edges\":[]}");
        workflowRepository.save(wf4);

        log.info("✅ 工作流初始化完成，共 {} 条", workflowRepository.count());
    }

    /**
     * 初始化工作流节点
     */
    private void initWorkflowNodes() {
        if (workflowNodeRepository.count() > 0) {
            log.info("✅ 工作流节点已存在，跳过初始化");
            return;
        }

        List<Workflow> workflows = workflowRepository.findAll();

        // 为工作流1添加节点
        Workflow wf1 = workflows.stream().filter(w -> "WF-DATA-001".equals(w.getWorkflowCode())).findFirst().orElse(null);
        if (wf1 != null) {
            WorkflowNode node1 = new WorkflowNode();
            node1.setWorkflowId(wf1.getId());
            node1.setNodeType("start");
            node1.setName("开始采集");
            node1.setDescription("开始数据采集流程");
            node1.setX(100);
            node1.setY(200);
            node1.setOrder(1);
            workflowNodeRepository.save(node1);

            WorkflowNode node2 = new WorkflowNode();
            node2.setWorkflowId(wf1.getId());
            node2.setNodeType("http");
            node2.setName("获取数据");
            node2.setDescription("从目标网站获取数据");
            node2.setX(300);
            node2.setY(200);
            node2.setOrder(2);
            node2.setTimeout(60);
            node2.setConfig("{\"url\":\"https://api.example.com/data\",\"method\":\"GET\"}");
            workflowNodeRepository.save(node2);

            WorkflowNode node3 = new WorkflowNode();
            node3.setWorkflowId(wf1.getId());
            node3.setNodeType("condition");
            node3.setName("判断状态");
            node3.setDescription("判断数据获取是否成功");
            node3.setX(500);
            node3.setY(200);
            node3.setOrder(3);
            workflowNodeRepository.save(node3);

            WorkflowNode node4 = new WorkflowNode();
            node4.setWorkflowId(wf1.getId());
            node4.setNodeType("data_process");
            node4.setName("数据清洗");
            node4.setDescription("清洗和转换数据格式");
            node4.setX(700);
            node4.setY(150);
            node4.setOrder(4);
            workflowNodeRepository.save(node4);

            WorkflowNode node5 = new WorkflowNode();
            node5.setWorkflowId(wf1.getId());
            node5.setNodeType("email");
            node5.setName("发送通知");
            node5.setDescription("发送采集完成通知");
            node5.setX(700);
            node5.setY(250);
            node5.setOrder(5);
            workflowNodeRepository.save(node5);

            WorkflowNode node6 = new WorkflowNode();
            node6.setWorkflowId(wf1.getId());
            node6.setNodeType("end");
            node6.setName("流程结束");
            node6.setDescription("流程执行完成");
            node6.setX(900);
            node6.setY(200);
            node6.setOrder(6);
            workflowNodeRepository.save(node6);
        }

        // 为工作流2添加节点
        Workflow wf2 = workflows.stream().filter(w -> "WF-REPORT-001".equals(w.getWorkflowCode())).findFirst().orElse(null);
        if (wf2 != null) {
            WorkflowNode node1 = new WorkflowNode();
            node1.setWorkflowId(wf2.getId());
            node1.setNodeType("start");
            node1.setName("开始生成");
            node1.setX(100);
            node1.setY(200);
            node1.setOrder(1);
            workflowNodeRepository.save(node1);

            WorkflowNode node2 = new WorkflowNode();
            node2.setWorkflowId(wf2.getId());
            node2.setNodeType("http");
            node2.setName("查询数据");
            node2.setDescription("从数据库查询统计数据");
            node2.setX(300);
            node2.setY(200);
            node2.setOrder(2);
            workflowNodeRepository.save(node2);

            WorkflowNode node3 = new WorkflowNode();
            node3.setWorkflowId(wf2.getId());
            node3.setNodeType("data_process");
            node3.setName("生成报表");
            node3.setDescription("生成Excel报表文件");
            node3.setX(500);
            node3.setY(200);
            node3.setOrder(3);
            workflowNodeRepository.save(node3);

            WorkflowNode node4 = new WorkflowNode();
            node4.setWorkflowId(wf2.getId());
            node4.setNodeType("email");
            node4.setName("发送邮件");
            node4.setDescription("发送报表给相关人员");
            node4.setX(700);
            node4.setY(200);
            node4.setOrder(4);
            workflowNodeRepository.save(node4);

            WorkflowNode node5 = new WorkflowNode();
            node5.setWorkflowId(wf2.getId());
            node5.setNodeType("end");
            node5.setName("完成");
            node5.setX(900);
            node5.setY(200);
            node5.setOrder(5);
            workflowNodeRepository.save(node5);
        }

        log.info("✅ 工作流节点初始化完成");
    }

    /**
     * 初始化采集任务配置
     */
    private void initCollectConfigs() {
        if (collectConfigRepository.count() > 0) {
            log.info("✅ 采集任务配置已存在，跳过初始化");
            return;
        }

        // 采集配置1：新闻网站数据采集
        CollectConfig config1 = new CollectConfig();
        config1.setName("新闻网站数据采集");
        config1.setCollectType("web");
        config1.setTargetUrl("https://news.example.com/api/articles");
        config1.setRequestMethod("GET");
        config1.setCollectRules("{\"listSelector\":\".article-item\",\"fields\":[{\"name\":\"title\",\"selector\":\"h3.title\",\"type\":\"text\"},{\"name\":\"summary\",\"selector\":\".summary\",\"type\":\"text\"},{\"name\":\"pubDate\",\"selector\":\".date\",\"type\":\"text\"}]}");
        config1.setCronExpression("0 0 */6 * * ?");
        config1.setIsEnabled(true);
        config1.setTimeout(30000);
        config1.setRetryCount(3);
        config1.setTotalCount(156L);
        config1.setSuccessCount(150L);
        config1.setFailCount(6L);
        config1.setLastExecuteTime(LocalDateTime.now().minusHours(2));
        config1.setLastExecuteStatus("success");
        config1.setCreateBy(1L);
        collectConfigRepository.save(config1);

        // 采集配置2：电商价格监控
        CollectConfig config2 = new CollectConfig();
        config2.setName("竞品价格监控");
        config2.setCollectType("web");
        config2.setTargetUrl("https://shop.example.com/api/products");
        config2.setRequestMethod("POST");
        config2.setRequestBody("{\"category\":\"electronics\",\"page\":1}");
        config2.setCollectRules("{\"listSelector\":\".product-card\",\"fields\":[{\"name\":\"productName\",\"selector\":\".name\",\"type\":\"text\"},{\"name\":\"price\",\"selector\":\".price\",\"type\":\"text\"},{\"name\":\"stock\",\"selector\":\".stock\",\"type\":\"text\"}]}");
        config2.setPageConfig("{\"type\":\"url_param\",\"paramName\":\"page\",\"startPage\":1,\"endPage\":10}");
        config2.setCronExpression("0 0 8,12,18 * * ?");
        config2.setIsEnabled(true);
        config2.setTimeout(60000);
        config2.setRetryCount(5);
        config2.setTotalCount(892L);
        config2.setSuccessCount(875L);
        config2.setFailCount(17L);
        config2.setLastExecuteTime(LocalDateTime.now().minusHours(4));
        config2.setLastExecuteStatus("success");
        config2.setCreateBy(1L);
        collectConfigRepository.save(config2);

        // 采集配置3：社交媒体数据采集
        CollectConfig config3 = new CollectConfig();
        config3.setName("社交媒体舆情采集");
        config3.setCollectType("api");
        config3.setTargetUrl("https://api.social.com/v2/posts");
        config3.setRequestMethod("GET");
        config3.setRequestHeaders("{\"Authorization\":\"Bearer xxx\",\"Content-Type\":\"application/json\"}");
        config3.setCollectRules("{\"fields\":[{\"name\":\"content\",\"type\":\"text\"},{\"name\":\"likes\",\"type\":\"number\"},{\"name\":\"comments\",\"type\":\"number\"}]}");
        config3.setCronExpression("0 0 */2 * * ?");
        config3.setIsEnabled(true);
        config3.setTimeout(45000);
        config3.setRetryCount(3);
        config3.setTotalCount(2341L);
        config3.setSuccessCount(2300L);
        config3.setFailCount(41L);
        config3.setLastExecuteTime(LocalDateTime.now().minusMinutes(30));
        config3.setLastExecuteStatus("success");
        config3.setCreateBy(1L);
        collectConfigRepository.save(config3);

        // 采集配置4：天气数据采集（禁用状态）
        CollectConfig config4 = new CollectConfig();
        config4.setName("城市天气数据采集");
        config4.setCollectType("api");
        config4.setTargetUrl("https://api.weather.com/v3/wx/news");
        config4.setRequestMethod("GET");
        config4.setCollectRules("{\"fields\":[{\"name\":\"city\",\"type\":\"text\"},{\"name\":\"temperature\",\"type\":\"text\"},{\"name\":\"condition\",\"type\":\"text\"}]}");
        config4.setIsEnabled(false);
        config4.setTimeout(20000);
        config4.setRetryCount(2);
        config4.setTotalCount(500L);
        config4.setSuccessCount(480L);
        config4.setFailCount(20L);
        config4.setLastExecuteTime(LocalDateTime.now().minusDays(3));
        config4.setLastExecuteStatus("failed");
        config4.setCreateBy(1L);
        collectConfigRepository.save(config4);

        // 采集配置5：数据库数据同步
        CollectConfig config5 = new CollectConfig();
        config5.setName("业务数据库数据同步");
        config5.setCollectType("database");
        config5.setTargetUrl("jdbc:mysql://localhost:3306/business_db");
        config5.setCollectRules("{\"sql\":\"SELECT * FROM orders WHERE status = 'pending'\",\"fields\":[{\"name\":\"orderId\",\"type\":\"text\"},{\"name\":\"customerName\",\"type\":\"text\"},{\"name\":\"amount\",\"type\":\"number\"}]}");
        config5.setCronExpression("0 0/30 * * * ?");
        config5.setIsEnabled(true);
        config5.setTimeout(120000);
        config5.setRetryCount(3);
        config5.setTotalCount(3200L);
        config5.setSuccessCount(3180L);
        config5.setFailCount(20L);
        config5.setLastExecuteTime(LocalDateTime.now().minusMinutes(15));
        config5.setLastExecuteStatus("success");
        config5.setCreateBy(1L);
        collectConfigRepository.save(config5);

        log.info("✅ 采集任务配置初始化完成，共 {} 条", collectConfigRepository.count());
    }

    /**
     * 初始化采集数据
     */
    private void initCollectData() {
        if (collectDataRepository.count() > 0) {
            log.info("✅ 采集数据已存在，跳过初始化");
            return;
        }

        List<CollectConfig> configs = collectConfigRepository.findAll();

        // 为配置1添加采集数据
        CollectConfig cfg1 = configs.stream().filter(c -> "新闻网站数据采集".equals(c.getName())).findFirst().orElse(null);
        if (cfg1 != null) {
            for (int i = 1; i <= 5; i++) {
                CollectData data = new CollectData();
                data.setConfigId(cfg1.getId());
                data.setSourceUrl("https://news.example.com/article/" + i);
                data.setDataHash("hash_news_" + i);
                data.setDataContent("{\"title\":\"新闻标题" + i + "\",\"summary\":\"这是新闻摘要内容" + i + "\",\"pubDate\":\"2026-04-" + String.format("%02d", i) + "\",\"author\":\"记者张三\"}");
                data.setStatus("valid");
                collectDataRepository.save(data);
            }
        }

        // 为配置2添加采集数据
        CollectConfig cfg2 = configs.stream().filter(c -> "竞品价格监控".equals(c.getName())).findFirst().orElse(null);
        if (cfg2 != null) {
            String[] products = {"手机", "电脑", "平板", "耳机", "键盘"};
            for (int i = 0; i < 5; i++) {
                CollectData data = new CollectData();
                data.setConfigId(cfg2.getId());
                data.setSourceUrl("https://shop.example.com/product/" + (i + 1));
                data.setDataHash("hash_product_" + i);
                data.setDataContent("{\"productName\":\"" + products[i] + "\",\"price\":\"" + (1999 + i * 1000) + "元\",\"stock\":\"现货\",\"seller\":\"官方旗舰店\"}");
                data.setStatus("valid");
                collectDataRepository.save(data);
            }
        }

        // 为配置3添加采集数据
        CollectConfig cfg3 = configs.stream().filter(c -> "社交媒体舆情采集".equals(c.getName())).findFirst().orElse(null);
        if (cfg3 != null) {
            String[] topics = {"产品发布", "用户反馈", "行业趋势", "优惠活动", "新品测评"};
            for (int i = 0; i < 5; i++) {
                CollectData data = new CollectData();
                data.setConfigId(cfg3.getId());
                data.setSourceUrl("https://api.social.com/post/" + (i + 1));
                data.setDataHash("hash_social_" + i);
                data.setDataContent("{\"content\":\"社交媒体内容关于" + topics[i] + "\",\"likes\":" + ((i + 1) * 128) + ",\"comments\":" + ((i + 1) * 23) + ",\"shares\":" + ((i + 1) * 15) + "}");
                data.setStatus("valid");
                collectDataRepository.save(data);
            }
        }

        // 为配置5添加采集数据
        CollectConfig cfg5 = configs.stream().filter(c -> "业务数据库数据同步".equals(c.getName())).findFirst().orElse(null);
        if (cfg5 != null) {
            String[] customers = {"北京科技有限公司", "上海贸易公司", "广州实业集团", "深圳创新企业", "杭州电商公司"};
            for (int i = 0; i < 5; i++) {
                CollectData data = new CollectData();
                data.setConfigId(cfg5.getId());
                data.setSourceUrl("jdbc:mysql://localhost:3306/business_db/orders/" + (i + 1));
                data.setDataHash("hash_order_" + i);
                data.setDataContent("{\"orderId\":\"ORD202604" + String.format("%03d", i + 1) + "\",\"customerName\":\"" + customers[i] + "\",\"amount\":" + ((i + 1) * 15800) + ",\"status\":\"pending\",\"createTime\":\"2026-04-" + String.format("%02d", i + 1) + " 10:30:00\"}");
                data.setStatus("valid");
                collectDataRepository.save(data);
            }
        }

        log.info("✅ 采集数据初始化完成，共 {} 条", collectDataRepository.count());
    }
}


