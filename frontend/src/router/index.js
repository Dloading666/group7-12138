import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/Profile.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'system',
        name: 'System',
        meta: { title: '系统管理', icon: 'Setting', permission: 'system:view' },
        children: [
          {
            path: 'user',
            name: 'UserManagement',
            component: () => import('../views/system/UserManagement.vue'),
            meta: { title: '用户管理', permission: 'system:user:view' }
          },
          {
            path: 'role',
            name: 'RoleManagement',
            component: () => import('../views/system/RoleManagement.vue'),
            meta: { title: '角色管理', permission: 'system:role:view' }
          },
          {
            path: 'permission',
            name: 'PermissionManagement',
            component: () => import('../views/system/PermissionManagement.vue'),
            meta: { title: '资源管理', permission: 'system:permission:view' }
          }
        ]
      },
      {
        path: 'task',
        name: 'Task',
        meta: { title: '任务管理', icon: 'List', permission: 'task:view' },
        children: [
          {
            path: 'list',
            name: 'TaskList',
            component: () => import('../views/task/TaskList.vue'),
            meta: { title: '任务列表' }
          },
          {
            path: 'history',
            name: 'TaskHistory',
            component: () => import('../views/task/TaskHistory.vue'),
            meta: { title: '任务历史' }
          }
        ]
      },
      {
        path: 'workflow',
        name: 'Workflow',
        meta: { title: '流程定义与设计', icon: 'Share', permission: 'workflow:view' },
        children: [
          {
            path: 'list',
            name: 'WorkflowList',
            component: () => import('../views/workflow/WorkflowList.vue'),
            meta: { title: '流程列表' }
          },
          {
            path: 'design',
            name: 'WorkflowDesign',
            component: () => import('../views/workflow/WorkflowDesign.vue'),
            meta: { title: '流程设计', permission: 'workflow:create' }
          }
        ]
      },
      {
        path: 'robot',
        name: 'Robot',
        meta: { title: '机器人管理', icon: 'Cpu', permission: 'robot:view' },
        children: [
          {
            path: 'list',
            name: 'RobotList',
            component: () => import('../views/robot/RobotList.vue'),
            meta: { title: '机器人列表' }
          },
          {
            path: 'form/:id?',
            name: 'RobotForm',
            component: () => import('../views/robot/RobotForm.vue'),
            meta: { title: '机器人表单' }
          }
        ]
      },
      {
        path: 'collect',
        name: 'Collect',
        meta: { title: '数据采集', icon: 'Download', permission: 'collect:view' },
        children: [
          {
            path: 'config',
            name: 'CollectConfig',
            component: () => import('../views/collect/CollectConfig.vue'),
            meta: { title: '采集配置', permission: 'collect:config' }
          }
        ]
      },
      {
        path: 'monitor',
        name: 'Monitor',
        meta: { title: '数据管理', icon: 'Monitor', permission: 'monitor:view' },
        children: [
          {
            path: 'realtime',
            name: 'RealtimeMonitor',
            component: () => import('../views/monitor/RealtimeMonitor.vue'),
            meta: { title: '数据采集' }
          },
          {
            path: 'logs',
            name: 'ExecutionLogs',
            component: () => import('../views/monitor/ExecutionLogs.vue'),
            meta: { title: '执行日志' }
          }
        ]
      },
      {
        path: 'statistics',
        name: 'Statistics',
        meta: { title: '数据查询与统计', icon: 'DataAnalysis', permission: 'statistics:view' },
        children: [
          {
            path: 'query',
            name: 'DataQuery',
            component: () => import('../views/statistics/DataQuery.vue'),
            meta: { title: '数据查询' }
          },
          {
            path: 'report',
            name: 'StatisticsReport',
            component: () => import('../views/statistics/StatisticsReport.vue'),
            meta: { title: '统计报表' }
          }
        ]
      },
      {
        path: 'settings',
        name: 'Settings',
        meta: { title: '系统设置', icon: 'Tools', permission: 'settings:view' },
        children: [
          {
            path: 'basic',
            name: 'BasicSettings',
            component: () => import('../views/settings/BasicSettings.vue'),
            meta: { title: '基础设置', permission: 'settings:basic:view' }
          },
          {
            path: 'notification',
            name: 'NotificationSettings',
            component: () => import('../views/settings/NotificationSettings.vue'),
            meta: { title: '通知设置', permission: 'settings:notification:view' }
          }
        ]
      }
    ]
  },
  // 403 无权限页面
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('../views/Forbidden.vue'),
    meta: { title: '无权限' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 检查用户是否有权限
const checkPermission = (permission) => {
  // 获取用户权限列表
  const permissionsStr = localStorage.getItem('userPermissions')
  if (!permissionsStr) return false

  const userPermissions = JSON.parse(permissionsStr)

  // 获取用户角色
  const userInfoStr = localStorage.getItem('userInfo')
  let isAdmin = false
  if (userInfoStr) {
    const userInfo = JSON.parse(userInfoStr)
    isAdmin = userInfo.role === 'ADMIN'
  }

  // 管理员拥有所有权限
  if (isAdmin) return true

  // 检查用户是否有该权限
  return userPermissions.includes(permission)
}

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 管理系统` : '管理系统'

  // 获取token
  const token = localStorage.getItem('token')

  // 如果访问登录页
  if (to.path === '/login') {
    // 已登录则跳转到首页
    if (token) {
      next('/')
    } else {
      next()
    }
    return
  }

  // 访问其他页面需要登录
  if (!token) {
    next('/login')
    return
  }

  // 检查页面权限
  const requiredPermission = to.meta.permission
  if (requiredPermission) {
    if (!checkPermission(requiredPermission)) {
      // 用户没有权限，跳转到403页面
      console.warn(`无权限访问: ${to.path}, 需要权限: ${requiredPermission}`)
      next('/403')
      return
    }
  }

  next()
})

export default router
