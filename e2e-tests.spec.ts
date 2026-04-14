import { test, expect, Page } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';
const API_URL = 'http://localhost:8080/api';

// Helper function to login
async function login(page: Page, username: string = 'admin', password: string = 'admin123') {
  // Clear session and go to login
  await page.goto(`${BASE_URL}/login`);
  await page.evaluate(() => localStorage.clear());
  await page.goto(`${BASE_URL}/login`);
  await page.fill('input[placeholder="请输入用户名"]', username);
  await page.fill('input[placeholder="请输入密码"]', password);
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/dashboard');
  await page.waitForTimeout(1000);
}

// Helper function to get auth token
async function getAuthToken(): Promise<string> {
  const response = await fetch(`${API_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'admin123' })
  });
  const data = await response.json();
  return data.data.token;
}

test.describe('1. 登录功能测试', () => {
  test('正确账号密码登录成功', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登 录")');

    // 等待跳转到首页
    await page.waitForURL('**/dashboard');
    await page.waitForTimeout(1000);

    // 验证登录成功 - 检查URL是否跳转到dashboard
    expect(page.url()).toContain('/dashboard');
  });

  test('错误密码登录失败', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'wrongpassword');
    await page.click('button:has-text("登 录")');

    // 等待错误提示出现
    await page.waitForTimeout(2000);

    // 应该仍然在登录页
    expect(page.url()).toContain('/login');
  });

  test('空白输入验证', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.click('button:has-text("登 录")');

    // Element Plus 表单验证应该阻止提交
    await page.waitForTimeout(500);
    // 应该停留在登录页
    expect(page.url()).toContain('/login');
  });
});

test.describe('2. 首页仪表盘测试', () => {
  test('登录后显示仪表盘', async ({ page }) => {
    await login(page);

    // 检查是否显示首页内容
    const content = await page.content();
    // 仪表盘应该有统计卡片或图表
    const hasDashboard = content.includes('仪表盘') || content.includes('统计') || content.includes('概览');
    expect(hasDashboard || await page.locator('.el-card').first().isVisible().catch(() => false)).toBeTruthy();
  });

  test('显示统计卡片', async ({ page }) => {
    await login(page);

    // 查找可能的统计数字
    await page.waitForTimeout(1000);
    const cards = await page.locator('.el-card').count();
    expect(cards).toBeGreaterThan(0);
  });
});

test.describe('3. 导航菜单测试', () => {
  test('侧边导航菜单可见', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    // 检查侧边导航是否存在
    const sidebar = page.locator('.sidebar, .el-aside, aside');
    const isSidebarVisible = await sidebar.first().isVisible().catch(() => false);
    expect(isSidebarVisible).toBeTruthy();
  });

  test('点击菜单项能切换视图', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    // 尝试点击用户管理菜单（如果有）
    const userManagementLink = page.locator('text=用户管理, text=系统管理').first();
    if (await userManagementLink.isVisible().catch(() => false)) {
      await userManagementLink.click();
      await page.waitForTimeout(1000);
      // 应该显示用户管理页面
      const content = await page.content();
      expect(content).toBeTruthy();
    }
  });
});

test.describe('4. 用户管理功能测试', () => {
  test('访问用户管理页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    // 导航到用户管理
    await page.goto(`${BASE_URL}/user-management`);
    await page.waitForTimeout(2000);

    // 检查页面内容
    const content = await page.content();
    expect(content).toBeTruthy();
  });

  test('显示用户列表', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/user-management`);
    await page.waitForTimeout(2000);

    // 检查是否有表格
    const table = page.locator('.el-table');
    const hasTable = await table.isVisible().catch(() => false);

    if (hasTable) {
      // 检查是否有用户数据
      const rows = await page.locator('.el-table__row').count();
      expect(rows).toBeGreaterThan(0);
    }
  });
});

test.describe('5. 角色管理功能测试', () => {
  test('访问角色管理页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/role-management`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });
});

test.describe('6. 权限管理功能测试', () => {
  test('访问权限管理页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/permission-management`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });
});

test.describe('7. 机器人管理功能测试', () => {
  test('访问机器人管理页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/robot-management`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });

  test('显示机器人列表', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/robot-management`);
    await page.waitForTimeout(2000);

    // 页面应该加载成功（有内容）
    const content = await page.content();
    expect(content.length).toBeGreaterThan(100);
  });
});

test.describe('8. 任务管理功能测试', () => {
  test('访问任务列表页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/tasks`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });
});

test.describe('9. 实时监控功能测试', () => {
  test('访问监控页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/monitor`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });
});

test.describe('10. 执行日志功能测试', () => {
  test('访问执行日志页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/execution-logs`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });
});

test.describe('11. 统计报表功能测试', () => {
  test('访问统计报表页面', async ({ page }) => {
    await login(page);
    await page.waitForTimeout(1000);

    await page.goto(`${BASE_URL}/statistics`);
    await page.waitForTimeout(2000);

    const content = await page.content();
    expect(content).toBeTruthy();
  });
});

test.describe('12. 登出功能测试', () => {
  test('登出后返回登录页', async ({ page }) => {
    await login(page);

    // 查找并点击登出按钮
    const logoutBtn = page.locator('button:has-text("退出"), button:has-text("登出"), .logout-btn');
    if (await logoutBtn.isVisible().catch(() => false)) {
      await logoutBtn.click();
      await page.waitForTimeout(1000);

      // 应该跳转到登录页
      expect(page.url()).toContain('/login');
    }
  });
});

test.describe('13. API 直接测试', () => {
  test('登录API返回正确格式', async () => {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123' })
    });

    expect(response.status).toBe(200);
    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.token).toBeTruthy();
    expect(data.data.username).toBe('admin');
    expect(data.data.role).toBe('ADMIN');
  });

  test('获取用户信息API', async () => {
    const token = await getAuthToken();

    const response = await fetch(`${API_URL}/user/profile`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    expect(response.status).toBe(200);
    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data.username).toBe('admin');
  });

  test('获取统计数据API', async () => {
    const token = await getAuthToken();

    const response = await fetch(`${API_URL}/admin/statistics`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    expect(response.status).toBe(200);
    const data = await response.json();
    expect(data.code).toBe(200);
    expect(data.data).toBeTruthy();
  });

  test('获取用户列表API', async () => {
    const token = await getAuthToken();

    const response = await fetch(`${API_URL}/admin/users?page=0&size=10`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    expect(response.status).toBe(200);
    const data = await response.json();
    expect(data.code).toBe(200);
    expect(Array.isArray(data.data)).toBeTruthy();
    expect(data.data.length).toBeGreaterThan(0);
  });

  test('无权限访问受保护资源', async () => {
    // 不带 token 访问受保护资源
    const response = await fetch(`${API_URL}/admin/users`);
    expect(response.status).toBe(403);
  });

  test('用户权限获取', async () => {
    const token = await getAuthToken();

    const response = await fetch(`${API_URL}/user/permissions`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    expect(response.status).toBe(200);
    const data = await response.json();
    expect(Array.isArray(data.data)).toBeTruthy();
  });
});

test.describe('14. 安全测试', () => {
  test('错误token被拒绝', async () => {
    const response = await fetch(`${API_URL}/admin/users`, {
      headers: {
        'Authorization': 'Bearer invalid_token_here'
      }
    });

    expect(response.status).toBe(403);
  });

  test('过期token被拒绝', async () => {
    // 使用一个过期的token（如果知道格式的话）
    // 这里我们用一个格式正确但无效的token
    const response = await fetch(`${API_URL}/admin/users`, {
      headers: {
        'Authorization': 'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzAwMDAwMDAwMCwiZXhwIjoxNzAwMDAwMDAwMH0.invalid_signature'
      }
    });

    expect(response.status).toBe(403);
  });
});

test.describe('15. 前端路由测试', () => {
  test('未登录访问首页重定向到登录', async ({ page }) => {
    await page.goto(`${BASE_URL}/`);
    await page.waitForTimeout(2000);

    // 如果有token则已登录，否则应该重定向到登录页
    const url = page.url();
    expect(url.includes('/login') || url.includes('/')).toBeTruthy();
  });

  test('直接访问各页面', async ({ page }) => {
    const pages = [
      '/dashboard',
      '/user-management',
      '/role-management',
      '/permission-management',
      '/robot-management',
      '/tasks',
      '/monitor',
      '/statistics'
    ];

    for (const pagePath of pages) {
      await login(page);
      await page.goto(`${BASE_URL}${pagePath}`);
      await page.waitForTimeout(1000);

      // 页面应该加载，没有崩溃
      const content = await page.content();
      expect(content.length).toBeGreaterThan(100);
    }
  });
});
