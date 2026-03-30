import type { AuthMenuNode } from '@/types/auth'

export const appNavigation: AuthMenuNode[] = [
  {
    name: '首页',
    path: '/dashboard',
    icon: 'HomeFilled',
    permissionCode: 'dashboard:view'
  },
  {
    name: '系统管理',
    icon: 'Setting',
    permissionCode: 'system:view',
    children: [
      { name: '用户管理', path: '/system/users', icon: 'User', permissionCode: 'system:user:view' },
      { name: '角色管理', path: '/system/roles', icon: 'UserFilled', permissionCode: 'system:role:view' },
      { name: '权限树管理', path: '/system/permissions', icon: 'Share', permissionCode: 'system:permission:view' }
    ]
  },
  {
    name: '任务管理',
    icon: 'List',
    permissionCode: 'task:view',
    children: [
      { name: '任务列表', path: '/tasks', icon: 'Tickets', permissionCode: 'task:view' }
    ]
  },
  {
    name: '流程定义与设计',
    icon: 'Share',
    permissionCode: 'workflow:view',
    children: [
      { name: '流程列表', path: '/workflow/list', icon: 'Tickets', permissionCode: 'workflow:view' },
      { name: '流程设计', path: '/workflow/design', icon: 'Operation', permissionCode: 'workflow:design' }
    ]
  },
  {
    name: '机器人管理',
    path: '/robots',
    icon: 'Cpu',
    permissionCode: 'robot:view'
  },
  {
    name: '执行监控与记录',
    icon: 'Monitor',
    permissionCode: 'monitor:view',
    children: [
      { name: '实时监控', path: '/monitor/realtime', icon: 'Histogram', permissionCode: 'monitor:view' },
      { name: '执行日志', path: '/monitor/logs', icon: 'Document', permissionCode: 'monitor:view' }
    ]
  },
  {
    name: '数据查询与统计',
    icon: 'DataAnalysis',
    permissionCode: 'statistics:view',
    children: [
      { name: '数据查询', path: '/statistics/query', icon: 'Search', permissionCode: 'statistics:view' },
      { name: '统计报表', path: '/statistics/report', icon: 'PieChart', permissionCode: 'statistics:view' }
    ]
  },
  {
    name: '系统设置',
    icon: 'Tools',
    permissionCode: 'settings:view',
    children: [
      { name: '基础设置', path: '/settings/basic', icon: 'Setting', permissionCode: 'settings:view' },
      { name: '通知设置', path: '/settings/notification', icon: 'Bell', permissionCode: 'settings:view' }
    ]
  },
  {
    name: '个人中心',
    path: '/profile',
    icon: 'UserFilled',
    permissionCode: 'profile:view'
  }
]
