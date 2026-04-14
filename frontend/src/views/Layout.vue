<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon :size="28"><Monitor /></el-icon>
        <span v-show="!isCollapse" class="logo-text">管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
        class="sidebar-menu"
      >
        <!-- 首页 - 所有用户可见 -->
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>

        <!-- 系统管理 -->
        <el-sub-menu index="system" v-if="hasPermission('system:view')">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user" v-if="hasPermission('system:user:view')">用户管理</el-menu-item>
          <el-menu-item index="/system/role" v-if="hasPermission('system:role:view')">角色管理</el-menu-item>
          <el-menu-item index="/system/permission" v-if="hasPermission('system:permission:view')">资源管理</el-menu-item>
        </el-sub-menu>

        <!-- 任务管理 -->
        <el-sub-menu index="task" v-if="hasPermission('task:view')">
          <template #title>
            <el-icon><List /></el-icon>
            <span>任务管理</span>
          </template>
          <el-menu-item index="/task/list">任务列表</el-menu-item>
          <el-menu-item index="/task/history">任务历史</el-menu-item>
        </el-sub-menu>

        <!-- 流程定义与设计 -->
        <el-sub-menu index="workflow" v-if="hasPermission('workflow:view')">
          <template #title>
            <el-icon><Share /></el-icon>
            <span>流程定义与设计</span>
          </template>
          <el-menu-item index="/workflow/list">流程列表</el-menu-item>
        </el-sub-menu>

        <!-- 机器人管理 -->
        <el-sub-menu index="robot" v-if="hasPermission('robot:view')">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>机器人管理</span>
          </template>
          <el-menu-item index="/robot/list">机器人列表</el-menu-item>
        </el-sub-menu>

        <!-- 执行监控与记录 -->
        <el-sub-menu index="monitor" v-if="hasPermission('monitor:view')">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>数据管理</span>
          </template>
          <el-menu-item index="/monitor/realtime">数据采集</el-menu-item>
          <el-menu-item index="/monitor/logs">数据解析</el-menu-item>
        </el-sub-menu>

        <!-- 数据查询与统计 -->
        <el-sub-menu index="statistics" v-if="hasPermission('statistics:view')">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据查询与统计</span>
          </template>
          <el-menu-item index="/statistics/query">数据查询</el-menu-item>
          <el-menu-item index="/statistics/report">统计报表</el-menu-item>
        </el-sub-menu>

        <!-- 系统设置 -->
        <el-sub-menu index="settings" v-if="hasPermission('settings:view')">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/settings/basic">基础设置</el-menu-item>
          <el-menu-item index="/settings/notification">通知设置</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon 
            class="collapse-btn" 
            :size="20" 
            @click="toggleCollapse"
          >
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info">
              <el-avatar :size="32" :src="userAvatar" />
              <span class="username">{{ username }}</span>
              <el-tag size="small" :type="isAdmin ? 'primary' : 'success'" style="margin-left: 8px;">
                {{ roleDisplayName }}
              </el-tag>
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

      <!-- 主内容区域 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { logout } from '../api/user.js'
import { isAdmin as checkIsAdmin, hasActionPermission } from '../utils/permission.js'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

// 用户信息
const username = ref('用户')
const userRole = ref('USER')
const roleDisplayName = ref('普通用户')
const userAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png')
const userPermissions = ref([])

// 是否为管理员
const isAdmin = computed(() => checkIsAdmin(userRole.value))

// 检查是否有权限
const hasPermission = (permissionCode) => {
  // 管理员拥有所有权限
  if (isAdmin.value) return true
  // 检查用户是否有该权限
  return userPermissions.value.includes(permissionCode)
}

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta.title)
  return matched.map(item => ({ title: item.meta.title }))
})

// 加载用户信息
const loadUserInfo = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    const userInfo = JSON.parse(userInfoStr)
    username.value = userInfo.realName || userInfo.username
    userRole.value = userInfo.role || 'USER'
    roleDisplayName.value = userInfo.roleDisplayName || '普通用户'
    userAvatar.value = userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
  }
  
  // 加载用户权限
  const permissionsStr = localStorage.getItem('userPermissions')
  if (permissionsStr) {
    userPermissions.value = JSON.parse(permissionsStr)
  }
}

// 提供权限判断方法给子组件使用
provide('userRole', userRole)
provide('isAdmin', isAdmin)
provide('hasActionPermission', (action) => hasActionPermission(action, userRole.value))
provide('hasPermission', hasPermission)

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
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 调用后端登出接口
    logout().then(() => {
      // 清除本地存储
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      
      ElMessage.success('退出登录成功')
      
      // 跳转到登录页
      router.push('/login')
    }).catch(() => {
      // 即使接口失败也清除本地信息
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    })
  }).catch(() => {
    // 用户取消
  })
}

// 组件挂载时加载用户信息
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped lang="scss">
.layout-container {
  width: 100%;
  height: 100%;
}

.sidebar {
  background: #001529;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .logo-text {
      margin-left: 10px;
    }
  }

  .sidebar-menu {
    border: none;
    height: calc(100% - 64px);
    overflow-y: auto;
  }
}

.main-container {
  background: #f0f2f5;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

  .header-left {
    display: flex;
    align-items: center;

    .collapse-btn {
      cursor: pointer;
      margin-right: 20px;
      transition: color 0.3s;

      &:hover {
        color: #1890ff;
      }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;

      .username {
        margin: 0 8px;
      }
    }
  }
}

.main {
  padding: 20px;
  overflow-y: auto;
}
</style>
