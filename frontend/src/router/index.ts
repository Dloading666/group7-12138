import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import AppLayout from '@/layout/AppLayout.vue'
import { appNavigation } from '@/config/navigation'
import { useAuthStore } from '@/stores/auth'
import { findFirstLeafPath } from '@/utils/menu'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { title: '无权限', public: true }
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页', permission: 'dashboard:view' } },
      { path: 'system', redirect: '/system/users' },
      { path: 'system/users', alias: ['/users'], name: 'Users', component: () => import('@/views/user/UserManagementView.vue'), meta: { title: '用户管理', permission: 'system:user:view' } },
      { path: 'system/roles', alias: ['/roles'], name: 'Roles', component: () => import('@/views/role/RoleManagementView.vue'), meta: { title: '角色管理', permission: 'system:role:view' } },
      { path: 'system/permissions', alias: ['/permissions'], name: 'Permissions', component: () => import('@/views/permission/PermissionManagementView.vue'), meta: { title: '权限树管理', permission: 'system:permission:view' } },
      { path: 'tasks', name: 'Tasks', component: () => import('@/views/task/TaskListView.vue'), meta: { title: '任务列表', permission: 'task:view' } },
      { path: 'robots', name: 'Robots', component: () => import('@/views/robot/RobotManagementView.vue'), meta: { title: '机器人管理', permission: 'robot:view' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/profile/ProfileView.vue'), meta: { title: '个人中心', permission: 'profile:view' } },
      { path: 'workflow/list', name: 'WorkflowList', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '流程列表', permission: 'workflow:view', description: '流程列表页面一期作为壳层保留。' } },
      { path: 'workflow/design', name: 'WorkflowDesign', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '流程设计', permission: 'workflow:design', description: '流程设计页面一期作为壳层保留。' } },
      { path: 'monitor/realtime', name: 'RealtimeMonitor', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '实时监控', permission: 'monitor:view', description: '实时监控页面一期作为壳层保留。' } },
      { path: 'monitor/logs', name: 'ExecutionLogs', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '执行日志', permission: 'monitor:view', description: '执行日志页面一期作为壳层保留。' } },
      { path: 'statistics/query', name: 'DataQuery', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '数据查询', permission: 'statistics:view', description: '数据查询页面一期作为壳层保留。' } },
      { path: 'statistics/report', name: 'StatisticsReport', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '统计报表', permission: 'statistics:view', description: '统计报表页面一期作为壳层保留。' } },
      { path: 'settings/basic', name: 'BasicSettings', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '基础设置', permission: 'settings:view', description: '基础设置页面一期作为壳层保留。' } },
      { path: 'settings/notification', name: 'NotificationSettings', component: () => import('@/views/placeholder/FeaturePlaceholder.vue'), meta: { title: '通知设置', permission: 'settings:view', description: '通知设置页面一期作为壳层保留。' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  document.title = `${String(to.meta.title || 'RPA 管理系统')} - RPA 管理系统`

  const authStore = useAuthStore()
  const isPublic = Boolean(to.meta.public)
  const requiresPermission = typeof to.meta.permission === 'string'

  if (!authStore.hydrated) {
    authStore.hydrate()
  }

  if (to.path === '/login' && authStore.isLoggedIn) {
    const firstPath = findFirstLeafPath(authStore.menuTree.length ? authStore.menuTree : appNavigation)
    next(firstPath)
    return
  }

  if (!isPublic && !authStore.isLoggedIn) {
    next('/login')
    return
  }

  if (authStore.isLoggedIn && authStore.permissionCodes.length === 0) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      authStore.clearAuth()
      next('/login')
      return
    }
  }

  if (requiresPermission && !authStore.hasPermission(to.meta.permission as string)) {
    next('/403')
    return
  }

  if (to.path === '/') {
    const firstPath = findFirstLeafPath(authStore.menuTree.length ? authStore.menuTree : appNavigation)
    next(firstPath)
    return
  }

  next()
})

export default router
