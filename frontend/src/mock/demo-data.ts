import type { AuthMenuNode } from '@/types/auth'
import type { DashboardOverview, PermissionNode, RobotItem, RoleItem, TaskItem, UserItem } from '@/types/domain'

export const demoMenuTree: AuthMenuNode[] = [
  { name: '首页', path: '/dashboard', icon: 'HomeFilled', permissionCode: 'dashboard:view' },
  {
    name: '系统管理',
    icon: 'Setting',
    children: [
      { name: '用户管理', path: '/system/users', icon: 'User', permissionCode: 'system:user:view' },
      { name: '角色管理', path: '/system/roles', icon: 'UserFilled', permissionCode: 'system:role:view' },
      { name: '权限树管理', path: '/system/permissions', icon: 'Share', permissionCode: 'system:permission:view' }
    ]
  },
  {
    name: '任务管理',
    icon: 'List',
    children: [{ name: '任务列表', path: '/tasks', icon: 'Tickets', permissionCode: 'task:view' }]
  },
  {
    name: '机器人管理',
    path: '/robots',
    icon: 'Cpu',
    permissionCode: 'robot:view'
  }
]

export const demoDashboard: DashboardOverview = {
  totalTasks: 1328,
  runningTasks: 42,
  robotCount: 18,
  successRate: 96.4,
  trend: [120, 132, 101, 134, 90, 230, 210],
  successTrend: [118, 129, 98, 131, 86, 224, 205],
  statusDistribution: [
    { name: '已完成', value: 415 },
    { name: '执行中', value: 168 },
    { name: '等待中', value: 102 },
    { name: '失败', value: 54 }
  ],
  recentTasks: [
    { id: 1, taskId: 'T-20260330-001', name: '日报抓取', type: '数据采集', status: 'running', progress: 72, priority: 'high', robotName: 'Robot-01', createTime: '2026-03-30 08:15:00' },
    { id: 2, taskId: 'T-20260330-002', name: '订单同步', type: '数据同步', status: 'completed', progress: 100, priority: 'medium', robotName: 'Robot-02', createTime: '2026-03-30 07:30:00' },
    { id: 3, taskId: 'T-20260330-003', name: '发票核验', type: '文件处理', status: 'pending', progress: 0, priority: 'high', robotName: 'Robot-03', createTime: '2026-03-30 09:20:00' }
  ]
}

export const demoTasks: TaskItem[] = [
  { id: 1, taskId: 'T-20260330-001', name: '日报抓取', type: '数据采集', status: 'running', progress: 72, priority: 'high', robotName: 'Robot-01', executeType: 'immediate', createTime: '2026-03-30 08:15:00', userName: 'admin' },
  { id: 2, taskId: 'T-20260330-002', name: '订单同步', type: '数据同步', status: 'completed', progress: 100, priority: 'medium', robotName: 'Robot-02', executeType: 'scheduled', createTime: '2026-03-30 07:30:00', userName: 'admin' },
  { id: 3, taskId: 'T-20260330-003', name: '发票核验', type: '文件处理', status: 'pending', progress: 0, priority: 'high', robotName: 'Robot-03', executeType: 'scheduled', createTime: '2026-03-30 09:20:00', userName: 'admin' },
  { id: 4, taskId: 'T-20260330-004', name: '客户回访', type: '报表生成', status: 'failed', progress: 38, priority: 'low', robotName: 'Robot-01', executeType: 'immediate', createTime: '2026-03-30 09:40:00', userName: 'demo' }
]

export const demoUsers: UserItem[] = [
  { id: 1, username: 'admin', realName: '系统管理员', phone: '13800000000', email: 'admin@example.com', roleId: 1, roleName: '管理员', status: 'active', createdAt: '2026-03-01 09:00:00' },
  { id: 2, username: 'zhangsan', realName: '张三', phone: '13811112222', email: 'zhangsan@example.com', roleId: 2, roleName: '运营', status: 'active', createdAt: '2026-03-02 10:00:00' },
  { id: 3, username: 'lisi', realName: '李四', phone: '13822223333', email: 'lisi@example.com', roleId: 3, roleName: '审计', status: 'inactive', createdAt: '2026-03-03 11:00:00' }
]

export const demoRoles: RoleItem[] = [
  { id: 1, name: '管理员', code: 'ADMIN', description: '拥有全部权限', status: 'active', userCount: 1, isDefault: true },
  { id: 2, name: '运营', code: 'OPERATOR', description: '日常运营权限', status: 'active', userCount: 6, isDefault: false },
  { id: 3, name: '审计', code: 'AUDITOR', description: '只读审计权限', status: 'active', userCount: 2, isDefault: false }
]

export const demoPermissions: PermissionNode[] = [
  {
    id: 100,
    name: '首页',
    code: 'dashboard:view',
    type: 'MENU',
    path: '/dashboard',
    icon: 'HomeFilled',
    status: 'active',
    children: []
  },
  {
    id: 200,
    name: '系统管理',
    code: 'system:view',
    type: 'MENU',
    path: '/system',
    icon: 'Setting',
    status: 'active',
    children: [
      { id: 201, name: '用户管理', code: 'system:user:view', type: 'MENU', path: '/system/users', icon: 'User', status: 'active', parentId: 200, children: [] },
      { id: 202, name: '用户范围分配', code: 'system:user:assign-scope', type: 'BUTTON', status: 'active', parentId: 201, children: [] },
      { id: 203, name: '角色管理', code: 'system:role:view', type: 'MENU', path: '/system/roles', icon: 'UserFilled', status: 'active', parentId: 200, children: [] },
      { id: 204, name: '角色授权', code: 'system:role:assign-permissions', type: 'BUTTON', status: 'active', parentId: 203, children: [] },
      { id: 205, name: '权限树管理', code: 'system:permission:view', type: 'MENU', path: '/system/permissions', icon: 'Share', status: 'active', parentId: 200, children: [] }
    ]
  }
]

export const demoRobots: RobotItem[] = [
  { id: 1, robotId: 'R-001', name: 'Robot-01', ip: '10.0.0.11', status: 'online', taskCount: 6, lastHeartbeat: '2026-03-30 09:50:00', description: '数据采集机器人' },
  { id: 2, robotId: 'R-002', name: 'Robot-02', ip: '10.0.0.12', status: 'busy', taskCount: 3, lastHeartbeat: '2026-03-30 09:48:00', description: '报表生成机器人' },
  { id: 3, robotId: 'R-003', name: 'Robot-03', ip: '10.0.0.13', status: 'offline', taskCount: 0, lastHeartbeat: '2026-03-29 21:18:00', description: '文件处理机器人' }
]
