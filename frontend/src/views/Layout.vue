<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '72px' : '228px'" class="sidebar">
      <div class="logo">
        <el-icon :size="28"><Monitor /></el-icon>
        <span v-show="!isCollapse" class="logo-text">RPA 管理平台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        background-color="transparent"
        text-color="#f8fbff"
        active-text-color="#7dd3fc"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <el-sub-menu v-if="hasPermission('system:view')" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="hasPermission('system:user:view')" index="/system/user">用户管理</el-menu-item>
          <el-menu-item v-if="hasPermission('system:role:view')" index="/system/role">角色管理</el-menu-item>
          <el-menu-item v-if="hasPermission('system:permission:view')" index="/system/permission">权限管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasPermission('task:view')" index="task">
          <template #title>
            <el-icon><List /></el-icon>
            <span>任务管理</span>
          </template>
          <el-menu-item v-if="hasPermission('task:list')" index="/task/list">任务列表</el-menu-item>
          <el-menu-item v-if="hasPermission('task:history')" index="/task/history">任务历史</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasPermission('workflow:view')" index="workflow">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>流程中心</span>
          </template>
          <el-menu-item v-if="hasPermission('workflow:list')" index="/workflow/list">流程列表</el-menu-item>
          <el-menu-item v-if="hasPermission('workflow:create')" index="/workflow/design">流程设计</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasPermission('robot:view')" index="robot">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>机器人管理</span>
          </template>
          <el-menu-item v-if="hasPermission('robot:list')" index="/robot/list">机器人列表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasDataCenterAccess" index="statistics">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据中心</span>
          </template>
          <el-menu-item v-if="hasPermission('statistics:query')" index="/statistics/query">采集结果</el-menu-item>
          <el-menu-item v-if="hasPermission('monitor:logs')" index="/statistics/logs">执行日志</el-menu-item>
          <el-menu-item v-if="hasPermission('statistics:report')" index="/statistics/report">统计报表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="hasPermission('settings:view')" index="settings">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item v-if="hasPermission('settings:basic:view')" index="/settings/basic">基础设置</el-menu-item>
          <el-menu-item v-if="hasPermission('settings:notification:view')" index="/settings/notification">通知设置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="toggleCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar" />
              <span class="username">{{ username }}</span>
              <el-tag size="small" :type="getRoleTagType(userRole)">{{ roleDisplayName }}</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleProfile">个人中心</el-dropdown-item>
                <el-dropdown-item @click="handleChangePassword">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, provide, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  Cpu,
  DataAnalysis,
  Expand,
  Fold,
  HomeFilled,
  List,
  Monitor,
  Setting,
  Share,
  Tools
} from '@element-plus/icons-vue'
import { logout } from '../api/user.js'
import { getRoleDisplayName, isAdmin as checkIsAdmin } from '../utils/permission.js'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

const username = ref('平台成员')
const userRole = ref('USER')
const roleDisplayName = ref('普通用户')
const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')
const userPermissions = ref([])

const isAdmin = computed(() => checkIsAdmin(userRole.value))
const activeMenu = computed(() => route.path)
const breadcrumbs = computed(() => {
  return route.matched
    .filter((item) => item.meta?.title)
    .map((item) => ({
      path: item.path,
      title: item.meta.title
    }))
})

const hasPermission = (permissionCode) => {
  if (!permissionCode || isAdmin.value) {
    return true
  }
  return userPermissions.value.includes(permissionCode)
}

const hasDataCenterAccess = computed(() => {
  return hasPermission('statistics:view')
    || hasPermission('statistics:query')
    || hasPermission('statistics:report')
    || hasPermission('monitor:logs')
})

const getRoleTagType = (role) => {
  if (role === 'ADMIN') return 'primary'
  if (role === 'GUEST') return 'warning'
  return 'success'
}

provide('userRole', userRole)
provide('isAdmin', isAdmin)
provide('hasPermission', hasPermission)
provide('hasActionPermission', hasPermission)

const loadUserInfo = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    try {
      const userInfo = JSON.parse(userInfoStr)
      username.value = userInfo.realName || userInfo.username || '平台成员'
      userRole.value = userInfo.role || 'USER'
      roleDisplayName.value = userInfo.roleDisplayName || getRoleDisplayName(userInfo.role || 'USER')
      userAvatar.value = userInfo.avatar || userAvatar.value
    } catch (error) {
      localStorage.removeItem('userInfo')
    }
  }

  const permissionsStr = localStorage.getItem('userPermissions')
  if (!permissionsStr) {
    userPermissions.value = []
    return
  }

  try {
    userPermissions.value = JSON.parse(permissionsStr)
  } catch (error) {
    localStorage.removeItem('userPermissions')
    userPermissions.value = []
  }
}

loadUserInfo()

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleProfile = () => {
  router.push('/profile')
}

const handleChangePassword = () => {
  router.push('/profile')
}

const handleLogout = () => {
  ElMessageBox.confirm('确认退出当前登录状态吗？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    logout()
      .catch(() => {})
      .finally(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('userPermissions')
        ElMessage.success('已退出登录')
        router.push('/login')
      })
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, #06233f 0%, #0a3157 48%, #0e4468 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  transition: width 0.3s ease;
  box-shadow: 14px 0 30px rgba(2, 12, 27, 0.18);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 68px;
  color: #f8fbff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.02em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
}

.logo-text {
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none;
}

.main-container {
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.username {
  font-weight: 600;
}

.main {
  min-width: 0;
}
</style>
