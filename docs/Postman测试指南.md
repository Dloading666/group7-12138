# Postman API 测试指南

## 📋 目录
1. [环境准备](#环境准备)
2. [认证接口测试](#认证接口测试)
3. [用户管理接口测试](#用户管理接口测试)
4. [任务管理接口测试](#任务管理接口测试)
5. [机器人管理接口测试](#机器人管理接口测试)
6. [监控接口测试](#监控接口测试)
7. [日志接口测试](#日志接口测试)

---

## 环境准备

### 1. 后端服务地址
```
基础URL: http://localhost:8080/api
```

### 2. Postman 环境变量配置
在 Postman 中创建环境变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| baseUrl | http://localhost:8080/api | API基础地址 |
| token | (留空) | 登录后自动设置 |

---

## 认证接口测试

### 1️⃣ 登录接口

**请求方式：** POST  
**URL：** `{{baseUrl}}/auth/login`  
**Headers：**
```
Content-Type: application/json
```

**Body (raw JSON)：**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**预期响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "系统管理员",
      "role": "ADMIN"
    }
  }
}
```

**📝 自动保存 Token：**
在 Tests 标签页添加以下脚本：
```javascript
pm.test("Save token", function () {
    var jsonData = pm.response.json();
    if (jsonData.code === 200 && jsonData.data.token) {
        pm.environment.set("token", jsonData.data.token);
        console.log("Token saved:", jsonData.data.token);
    }
});
```

---

### 2️⃣ 获取当前用户信息

**请求方式：** GET  
**URL：** `{{baseUrl}}/auth/userinfo`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "email": "admin@example.com"
  }
}
```

---

### 3️⃣ 用户登出

**请求方式：** POST  
**URL：** `{{baseUrl}}/auth/logout`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

## 用户管理接口测试

### 4️⃣ 获取个人资料

**请求方式：** GET  
**URL：** `{{baseUrl}}/user/profile`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

### 5️⃣ 更新个人资料

**请求方式：** PUT  
**URL：** `{{baseUrl}}/user/profile`  
**Headers：**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Body (raw JSON)：**
```json
{
  "realName": "管理员",
  "email": "admin@newemail.com",
  "phone": "13900139000"
}
```

---

### 6️⃣ 修改密码

**请求方式：** PUT  
**URL：** `{{baseUrl}}/user/password`  
**Headers：**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Body (raw JSON)：**
```json
{
  "oldPassword": "admin123",
  "newPassword": "newpassword123"
}
```

---

## 任务管理接口测试

### 7️⃣ 创建任务

**请求方式：** POST  
**URL：** `{{baseUrl}}/tasks`  
**Headers：**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Body (raw JSON)：**
```json
{
  "name": "爬取电商数据",
  "type": "数据采集",
  "robotId": 1,
  "robotName": "Robot-01",
  "priority": "high",
  "executeType": "immediate",
  "description": "爬取京东商品价格数据"
}
```

---

### 8️⃣ 查询任务列表

**请求方式：** GET  
**URL：** `{{baseUrl}}/tasks`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**Query Parameters：**
| 参数 | 值 | 说明 |
|------|------|------|
| page | 1 | 页码 |
| size | 10 | 每页大小 |
| name | (可选) | 任务名称 |
| status | (可选) | 状态：pending/running/completed/failed |

---

### 9️⃣ 启动任务

**请求方式：** POST  
**URL：** `{{baseUrl}}/tasks/1/start`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

### 🔟 停止任务

**请求方式：** POST  
**URL：** `{{baseUrl}}/tasks/1/stop`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

### 1️⃣1️⃣ 获取任务统计

**请求方式：** GET  
**URL：** `{{baseUrl}}/tasks/stats`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "total": 100,
    "pending": 20,
    "running": 5,
    "completed": 70,
    "failed": 5,
    "avgDuration": 45
  }
}
```

---

## 机器人管理接口测试

### 1️⃣2️⃣ 获取机器人列表

**请求方式：** GET  
**URL：** `{{baseUrl}}/robots/all`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

### 1️⃣3️⃣ 创建机器人

**请求方式：** POST  
**URL：** `{{baseUrl}}/robots`  
**Headers：**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Body (raw JSON)：**
```json
{
  "name": "数据采集机器人01",
  "type": "data-collection",
  "description": "用于爬取电商网站数据",
  "environment": "prod",
  "concurrency": 3,
  "timeout": 300,
  "retryCount": 3
}
```

---

### 1️⃣4️⃣ 更新机器人配置

**请求方式：** PUT  
**URL：** `{{baseUrl}}/robots/1`  
**Headers：**
```
Authorization: Bearer {{token}}
Content-Type: application/json
```

**Body (raw JSON)：**
```json
{
  "name": "数据采集机器人01",
  "type": "data-collection",
  "environment": "prod",
  "concurrency": 5,
  "timeout": 600,
  "retryCount": 5,
  "notification": ["email", "webhook"],
  "params": "{\"proxy\": \"http://proxy.example.com:8080\"}"
}
```

---

### 1️⃣5️⃣ 启动机器人

**请求方式：** POST  
**URL：** `{{baseUrl}}/robots/1/start`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

### 1️⃣6️⃣ 停止机器人

**请求方式：** POST  
**URL：** `{{baseUrl}}/robots/1/stop`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

## 监控接口测试

### 1️⃣7️⃣ 获取实时监控统计

**请求方式：** GET  
**URL：** `{{baseUrl}}/monitor/stats`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "running": 5,
    "pending": 20,
    "completed": 70,
    "failed": 5,
    "total": 100,
    "avgDuration": 45,
    "onlineRobots": 3
  }
}
```

---

### 1️⃣8️⃣ 获取执行中任务列表

**请求方式：** GET  
**URL：** `{{baseUrl}}/monitor/running-tasks`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**Query Parameters：**
| 参数 | 值 | 说明 |
|------|------|------|
| limit | 10 | 返回数量限制 |

---

### 1️⃣9️⃣ 获取系统资源使用情况

**请求方式：** GET  
**URL：** `{{baseUrl}}/monitor/system-resources`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**预期响应：**
```json
{
  "code": 200,
  "data": {
    "cpu": 35,
    "memory": 68,
    "disk": 45,
    "network": 20
  }
}
```

---

### 2️⃣0️⃣ 获取任务执行详情

**请求方式：** GET  
**URL：** `{{baseUrl}}/monitor/task/1/detail`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

## 日志接口测试

### 2️⃣1️⃣ 分页查询日志

**请求方式：** GET  
**URL：** `{{baseUrl}}/logs`  
**Headers：**
```
Authorization: Bearer {{token}}
```

**Query Parameters：**
| 参数 | 值 | 说明 |
|------|------|------|
| page | 1 | 页码 |
| size | 50 | 每页大小 |
| level | (可选) | 日志级别：INFO/WARN/ERROR |
| taskCode | (可选) | 任务编号 |
| startTime | (可选) | 开始时间：2024-01-01 |
| endTime | (可选) | 结束时间：2024-01-31 |

---

### 2️⃣2️⃣ 清空所有日志

**请求方式：** DELETE  
**URL：** `{{baseUrl}}/logs/clear`  
**Headers：**
```
Authorization: Bearer {{token}}
```

---

### 2️⃣3️⃣ 清空历史日志

**请求方式：** DELETE  
**URL：** `{{baseUrl}}/logs/clear-before/7`  
**Headers：**
```
Authorization: Bearer {{token}}
```

说明：清空 7 天前的日志

---

## 🔧 Postman 使用技巧

### 1. 批量测试
1. 创建 Collection，将所有接口保存到 Collection 中
2. 点击 Collection 旁的 "Run" 按钮
3. 批量执行所有接口测试

### 2. 环境切换
创建多个环境：
- **本地环境**：baseUrl = http://localhost:8080/api
- **测试环境**：baseUrl = http://test.example.com/api
- **生产环境**：baseUrl = https://api.example.com/api

### 3. 自动化测试脚本
在 Tests 标签页添加断言：

```javascript
// 检查状态码
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// 检查响应格式
pm.test("Response has code field", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('code');
});

// 检查业务逻辑
pm.test("Response code is 200", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.code).to.eql(200);
});
```

### 4. 导入 Collection
可以将上面的接口保存为 JSON 文件，然后在 Postman 中导入。

---

## ⚠️ 常见问题

### 1. 401 Unauthorized
**原因：** Token 无效或过期  
**解决：** 重新登录获取新的 Token

### 2. 403 Forbidden
**原因：** 权限不足  
**解决：** 使用有权限的账号登录

### 3. 404 Not Found
**原因：** 接口路径错误  
**解决：** 检查 URL 是否正确

### 4. 400 Bad Request
**原因：** 参数格式错误  
**解决：** 检查请求体格式和必填参数

---

## 📚 更多资源

- [Postman 官方文档](https://learning.postman.com/)
- [Spring Boot REST API 最佳实践](https://spring.io/guides/gs/rest-service/)
- [JWT 认证详解](https://jwt.io/introduction)
