# Spider Execution - 基于Playwright的金融指标采集项目

## 项目简介

本项目是一个基于 Playwright Java 的金融指标采集爬虫系统，采用四步流程设计：
1. **Collect（采集）** - 使用 Playwright 爬取网页数据
2. **Parse（解析）** - 解析采集数据，添加月份字段
3. **Process（处理）** - 筛选发票，按月份汇总，计算统计指标
4. **Persist（持久化）** - 组装业务JSON，写入查询表

## 技术栈

- **Java 17**
- **Spring Boot 3.2.0**
- **Playwright Java 1.40.0** - 无头浏览器爬虫
- **MyBatis-Plus 3.5.5** - 持久化框架
- **MySQL 8.0** - 数据库
- **Druid** - 数据库连接池
- **Fastjson2** - JSON处理
- **Lombok** - 简化代码

## 项目结构

```
spider_exc/
├── docs/
│   └── sql/
│       └── schema.sql              # 数据库表结构
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/spider/exc/
│   │   │       ├── SpiderExcApplication.java    # 启动类
│   │   │       ├── config/                       # 配置类
│   │   │       ├── controller/                   # 控制器（API接口）
│   │   │       ├── domain/
│   │   │       │   ├── entity/                   # 实体类
│   │   │       │   └── mapper/                    # Mapper接口
│   │   │       ├── dto/                           # 数据传输对象
│   │   │       └── util/                          # 工具类
│   │   └── resources/
│   │       └── application.yml                    # 配置文件
│   └── test/
│       └── java/
│           └── com/spider/exc/
│               └── FourStepIndicatorTest.java    # 四步流程测试类
└── pom.xml
```

## 数据库表结构

项目包含4个中间表：

1. **rpa_indicator_collection** - 采集表
2. **rpa_indicator_parsing** - 解析表
3. **rpa_indicator_processing** - 处理表
4. **rpa_indicator_query** - 查询表

详细表结构见 `docs/sql/schema.sql`

## 环境准备

### 1. 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE spider_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本：
```bash
mysql -u root -p spider_db < docs/sql/schema.sql
```

3. 修改 `src/main/resources/application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/spider_db?...
    username: root
    password: root  # 修改为你的密码
```

### 2. 启动前端项目

确保前端项目 `spider_web` 已启动：
```bash
cd D:\javaWeb\demo\spider\spider_web
npm install
npm run dev
```

前端服务默认运行在 `http://localhost:3000`

### 3. 安装 Playwright 浏览器

首次运行前需要安装 Playwright 浏览器：
```bash
cd D:\javaWeb\demo\spider\spider_exc
mvn test-compile
# 或者手动安装
java -cp target/classes com.microsoft.playwright.CLI install chromium
```

## 运行测试

### 顺序执行四步测试

按照以下顺序运行测试方法：

1. **testCollect()** - 采集数据
2. **testParse()** - 解析数据
3. **testProcess()** - 处理数据
4. **testComplete()** - 持久化数据

### 运行方式

#### 方式1：使用IDE运行
在IDE中依次运行 `FourStepIndicatorTest` 类的四个测试方法。

#### 方式2：使用Maven命令
```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=FourStepIndicatorTest

# 运行指定测试方法
mvn test -Dtest=FourStepIndicatorTest#testCollect
```

## 测试流程说明

### 步骤1: Collect（采集）

- 使用 Playwright 访问申请页面 (`/application`)
- 填写表单并提交，获取入参（taxNo, uscCode, appDate）
- 访问发票查询页面 (`/invoice-query`)
- 填写查询条件并查询
- 遍历表格提取发票数据（sign, state, invoice_time, jshj）
- 将采集数据写入 `rpa_indicator_collection` 表

### 步骤2: Parse（解析）

- 从API读取最新的采集记录
- 解析采集的JSON数据
- 为每条发票添加月份字段（yyyy-MM）
- 转换金额字段（去除千分位分隔符）
- 将解析数据写入 `rpa_indicator_parsing` 表

### 步骤3: Process（处理）

- 从API读取最新的解析记录
- 筛选符合条件的发票：
  - sign = '销项'
  - state = '正常'
  - invoice_time 在申请日期前1-12个月（不含当月）
- 按月份汇总金额
- 计算统计指标：
  - mean_amt（均值）
  - std_amt（标准差）
  - cv（波动系数 = 标准差/均值）
- 计算指标1：符合条件的销项发票总金额
- 将处理结果写入 `rpa_indicator_processing` 表

### 步骤4: Persist（持久化）

- 从API读取前三步的最新记录
- 组装业务JSON，包含：
  - 各步骤的关联ID
  - 完整的采集数据
  - 解析后的结构化数据
  - 处理结果（统计指标）
- 将业务JSON写入 `rpa_indicator_query` 表

## 注意事项

1. **前端服务必须运行**：测试前确保前端项目 `spider_web` 已启动
2. **数据库连接**：确保数据库已创建并配置正确
3. **Playwright浏览器**：首次运行需要安装浏览器驱动
4. **测试顺序**：必须按照 Collect → Parse → Process → Complete 的顺序执行
5. **元素ID**：确保前端页面的元素ID与文档说明一致

## 常见问题

### 1. Playwright浏览器未安装
```
错误：BrowserType.launch: Executable doesn't exist
解决：运行 mvn test-compile 或手动安装浏览器
```

### 2. 数据库连接失败
```
错误：Communications link failure
解决：检查 application.yml 中的数据库配置
```

### 3. 前端页面无法访问
```
错误：net::ERR_CONNECTION_REFUSED
解决：确保前端项目已启动在 localhost:3000
```

## 开发说明

### 添加新的指标计算

1. 在 `ProcessedResult` 中添加新的指标字段
2. 在 `testProcess()` 方法中添加计算逻辑
3. 更新数据库表结构（如需要）

### 修改爬取逻辑

1. 修改 `testCollect()` 方法中的元素定位
2. 根据前端页面变化调整选择器
3. 参考 `爬虫参数说明.md` 和 `指标2参数位置和操作说明.md`

## 许可证

本项目仅供学习和研究使用。
