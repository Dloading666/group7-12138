import { createRouter, createWebHistory } from 'vue-router'

const APP_TITLE = 'RPA 管理平台'

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
            meta: { title: '权限管理', permission: 'system:permission:view' }
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
            meta: { title: '任务列表', permission: 'task:list' }
          },
          {
            path: 'history',
            name: 'TaskHistory',
            component: () => import('../views/task/TaskHistory.vue'),
            meta: { title: '任务历史', permission: 'task:history' }
          },
          {
            path: 'detail/:id',
            name: 'TaskDetail',
            component: () => import('../views/task/TaskDetail.vue'),
            meta: { title: '任务详情' }
          }
        ]
      },
      {
        path: 'workflow',
        name: 'Workflow',
        meta: { title: '流程中心', icon: 'Share', permission: 'workflow:view' },
        children: [
          {
            path: 'list',
            name: 'WorkflowList',
            component: () => import('../views/workflow/WorkflowList.vue'),
            meta: { title: '流程列表', permission: 'workflow:list' }
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
            meta: { title: '机器人列表', permission: 'robot:list' }
          },
          {
            path: 'form/:id?',
            name: 'RobotForm',
            component: () => import('../views/robot/RobotForm.vue'),
            meta: { title: '机器人编辑' }
          }
        ]
      },
      {
        path: 'statistics',
        name: 'Statistics',
        meta: { title: '数据中心', icon: 'DataAnalysis', permission: 'statistics:view' },
        children: [
          {
            path: 'query',
            name: 'DataQuery',
            component: () => import('../views/statistics/DataQuery.vue'),
            meta: { title: '采集结果', permission: 'statistics:query' }
          },
          {
            path: 'logs',
            name: 'ExecutionLogs',
            component: () => import('../views/monitor/ExecutionLogs.vue'),
            meta: { title: '执行日志', permission: 'monitor:logs' }
          },
          {
            path: 'report',
            name: 'StatisticsReport',
            component: () => import('../views/statistics/StatisticsReport.vue'),
            meta: { title: '统计报表', permission: 'statistics:report' }
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

const parseLocalJson = (key, fallback) => {
  const raw = localStorage.getItem(key)
  if (!raw) {
    return fallback
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    localStorage.removeItem(key)
    return fallback
  }
}

const checkPermission = (permission) => {
  const userPermissions = parseLocalJson('userPermissions', [])
  const userInfo = parseLocalJson('userInfo', {})
  const isAdmin = userInfo?.role === 'ADMIN'
  if (isAdmin) {
    return true
  }
  return Array.isArray(userPermissions) && userPermissions.includes(permission)
}

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${APP_TITLE}` : APP_TITLE

  if (to.path === '/login') {
    next()
    return
  }

  const token = localStorage.getItem('token')
  if (!token) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    return
  }

  const requiredPermission = to.meta.permission
  if (requiredPermission && !checkPermission(requiredPermission)) {
    next('/403')
    return
  }

  next()
})

export default router
