# 整合计划：project-gl 机器人 × spider_exc 爬取 spider_web

## 目标
让 project-gl 的 RPA 机器人类能够通过 spider_exc 执行器驱动 Playwright 爬取模拟税务网站 spider_web（Vue SPA），并通过 project-gl 前端展示采集结果和指标数据。

---

## 现状

| 系统 | 端口 | 状态 |
|------|------|------|
| project-gl (后端) | 8080 | 运行中 |
| project-gl (前端) | 3000 | 运行中 |
| spider_exc | 8081 | 待启动 |
| spider_web | 5173 | 待启动 |
| spider_db (MySQL) | 3306 | 待建 |

> 注：端口可能有重叠，需要整理。

---

## Phase 1：基础设施（通信打通）

### 1.1 spider_exc 新增任务接收 API

**文件：** `spider_exc/src/main/java/com/spider/exc/controller/SpiderTaskController.java`（新建）

```java
@RestController
@RequestMapping("/api/spider/task")
public class SpiderTaskController {

    @Autowired
    private SpiderTaskService spiderTaskService;

    /**
     * 接收 project-gl 提交的任务
     * POST /api/spider/task/submit
     */
    @PostMapping("/submit")
    public ApiResponse<Long> submitTask(@RequestBody SpiderTaskRequest request) {
        Long taskId = spiderTaskService.submitTask(request);
        return ApiResponse.success(taskId);
    }

    /**
     * 查询任务状态
     * GET /api/spider/task/{taskId}/status
     */
    @GetMapping("/{taskId}/status")
    public ApiResponse<String> getTaskStatus(@PathVariable String taskId) {
        String status = spiderTaskService.getTaskStatus(taskId);
        return ApiResponse.success(status);
    }

    /**
     * 获取四步指标结果
     * GET /api/spider/task/{taskId}/result
     */
    @GetMapping("/{taskId}/result")
    public ApiResponse<IndicatorQuery> getTaskResult(@PathVariable String taskId) {
        IndicatorQuery result = spiderTaskService.getTaskResult(taskId);
        return ApiResponse.success(result);
    }
}
```

### 1.2 spider_exc 新增 DTO

**文件：** `spider_exc/src/main/java/com/spider/exc/dto/SpiderTaskRequest.java`（新建）

```java
@Data
public class SpiderTaskRequest {
    private String taskId;          // project-gl 侧的任务ID
    private String taxNo;           // 纳税人识别号
    private String uscCode;         // 统一社会信用代码
    private String appDate;         // 申请日期 yyyy-MM-dd
    private String callbackUrl;    // project-gl 回调地址
}
```

### 1.3 spider_exc 新增 Service

**文件：** `spider_exc/src/main/java/com/spider/exc/service/SpiderTaskService.java`（新建）

- `submitTask()` — 接收任务，写入 SpiderTask 表（新建），触发四步流程异步执行
- `getTaskStatus()` — 查询任务当前状态（pending/running/completed/failed）
- `getTaskResult()` — 查询最新的 IndicatorQuery 结果

### 1.4 spider_exc 新增 Task 表

**文件：** `spider_exc/docs/sql/spider_task.sql`（新建）

```sql
CREATE TABLE rpa_spider_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'project-gl 侧任务ID',
    tax_no VARCHAR(50) COMMENT '纳税人识别号',
    usc_code VARCHAR(50) COMMENT '统一社会信用代码',
    app_date DATE COMMENT '申请日期',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/completed/failed',
    error_message TEXT COMMENT '失败原因',
    callback_url VARCHAR(500) COMMENT '回调地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## Phase 2：project-gl 侧改造

### 2.1 新增 SpiderApiClient

**文件：** `project-gl/backend/src/main/java/com/rpa/management/client/SpiderApiClient.java`（新建）

```java
@Component
@RequiredArgsConstructor
public class SpiderApiClient {

    private final RestTemplate restTemplate;
    private static final String SPIDER_EXC_URL = "http://localhost:8081/api/spider/task";

    public Long submitSpiderTask(String taskId, String taxNo, String uscCode, String appDate) {
        SpiderTaskRequest request = new SpiderTaskRequest();
        request.setTaskId(taskId);
        request.setTaxNo(taxNo);
        request.setUscCode(uscCode);
        request.setAppDate(appDate);
        request.setCallbackUrl("http://localhost:8080/api/spider/task/callback");

        ResponseEntity<ApiResponse<Long>> response = restTemplate.postForEntity(
            SPIDER_EXC_URL + "/submit", request,
            new ParameterizedTypeReference<ApiResponse<Long>>() {}
        );
        return response.getBody().getData();
    }

    public String getTaskStatus(String taskId) {
        // 调用 spider_exc GET /status
    }

    public IndicatorQuery getTaskResult(String taskId) {
        // 调用 spider_exc GET /result
    }

    public void handleCallback(SpiderCallbackRequest request) {
        // 处理 spider_exc 的回调，更新 project-gl 侧任务状态
    }
}
```

### 2.2 RobotExecutor 改造

**文件：** `project-gl/backend/src/main/java/com/rpa/management/engine/RobotExecutor.java`

改动点：在 `executeDataCollection()` 中识别采集类型

```java
private int executeDataCollection(Task task, Robot robot) {
    CollectConfig config = collectConfigRepository.findByTaskId(task.getId()).stream().findFirst().orElse(null);

    if ("spider-tax".equals(config.getCollectType())) {
        // 调用 spider_exc
        return spiderApiClient.submitSpiderTask(...);
    }

    // 原有 Jsoup 逻辑
    return webCollector.collect(config, task.getId(), robot.getId());
}
```

### 2.3 CollectConfig 实体改造

**文件：** `project-gl/backend/src/main/java/com/rpa/management/entity/CollectConfig.java`

新增字段：

```java
@Column(name = "collect_type", length = 20)
private String collectType = "web";  // web / spider-tax / api

// spider-tax 类型专用
@Column(name = "spider_config", columnDefinition = "TEXT")
private String spiderConfig;  // JSON: { "taxNo": "", "uscCode": "", "appDate": "" }
```

---

## Phase 3：spider_exc 四步流程改造

### 3.1 新建 SpiderTaxExecutor

**文件：** `spider_exc/src/main/java/com/spider/exc/executor/SpiderTaxExecutor.java`（新建）

将 `FourStepIndicatorTest` 的四步逻辑提取为可调用服务：

```java
@Service
public class SpiderTaxExecutor {

    @Autowired private IndicatorCollectionMapper collectionMapper;
    @Autowired private IndicatorParsingMapper parsingMapper;
    @Autowired private IndicatorProcessingMapper processingMapper;
    @Autowired private IndicatorQueryMapper queryMapper;

    public void execute(Long spiderTaskId, String taxNo, String uscCode, String appDate) {
        // Step 1: Collect — Playwright 登录 spider_web，采集发票数据
        // Step 2: Parse — 解析数据，添加月份字段
        // Step 3: Process — 筛选发票，计算统计指标
        // Step 4: Persist — 组装业务JSON，写入 rpa_indicator_query

        // 更新 rpa_spider_task 状态
        // 回调 project-gl
    }
}
```

### 3.2 spider_exc 回调 project-gl

```java
private void callbackProjectGl(String callbackUrl, String taskId, String status, Object result) {
    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> body = new HashMap<>();
    body.put("taskId", taskId);
    body.put("status", status);
    body.put("result", result);

    rt.postForEntity(callbackUrl, new HttpEntity<>(body, headers), String.class);
}
```

---

## Phase 4：前端整合

### 4.1 采集配置页面改造

**文件：** `project-gl/frontend/src/views/collect/CollectConfig.vue`（新建或改造）

- 新增采集类型选择：`web` / `spider-tax`
- 选择 `spider-tax` 时显示税务专用配置表单（taxNo、uscCode、appDate 输入框）
- 保存时将 spider-tax 配置序列化写入 `collectConfig.spiderConfig` JSON 字段

### 4.2 任务结果展示

**文件：** `project-gl/frontend/src/views/task/TaskDetail.vue`（新建）

- 展示任务执行状态
- 展示 spider_exc 返回的四步指标结果：
  - 采集数量（Collect）
  - 解析数量（Parse）
  - 发票汇总金额、处理指标（Process）
  - 完整业务 JSON（Persist）

---

## 执行顺序

```
Step 1:  整理端口，清理冲突（project-gl 后端 8080 → 改为 8082）
Step 2:  spider_exc 新建数据库表 rpa_spider_task
Step 3:  spider_exc 新建 SpiderTaskController + SpiderTaskService
Step 4:  spider_exc 新建 SpiderTaxExecutor（四步逻辑）
Step 5:  project-gl 新增 SpiderApiClient
Step 6:  project-gl CollectConfig 实体新增字段
Step 7:  project-gl RobotExecutor 改造，路由到 spider_exc
Step 8:  project-gl 前端采集配置页面改造
Step 9:  project-gl 前端任务详情页新建
Step 10: 启动顺序验证 + 端到端联调
```

---

## 验证方式

1. **Phase 1 验证：** `curl` 或 Postman 直接调 spider_exc 的 `/api/spider/task/submit`，返回任务ID
2. **Phase 2 验证：** project-gl 前端创建 spider-tax 类型任务，查看 robot 日志是否打印 "submit to spider_exc"
3. **Phase 3 验证：** spider_exc 日志显示四步流程走完，spider_db 的四张表有数据
4. **Phase 4 验证：** project-gl 前端任务详情页能看到指标数据

---

## 风险点

| 风险 | 应对 |
|------|------|
| project-gl 8080 和 spider_exc 8080 端口冲突 | 先改 project-gl 后端端口为 8082 |
| spider_web 是 SPA，Vue 渲染需等待 | Playwright `page.waitForSelector` 等待关键元素 |
| spider_exc 回调 project-gl 网络不通 | 回调失败不阻塞任务，project-gl 前端轮询兜底 |
