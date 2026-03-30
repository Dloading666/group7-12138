<template>
  <el-container class="app-layout" data-testid="app-shell">
    <el-aside class="app-sidebar" data-testid="app-sidebar" :width="appStore.sidebarCollapsed ? '84px' : '240px'">
      <div class="brand">
        <div class="brand-copy">
          <div class="brand-name">{{ appStore.sidebarCollapsed ? 'RPA' : 'RPA管理系统' }}</div>
        </div>
      </div>

      <el-scrollbar class="sidebar-scroll">
        <el-menu
          class="sidebar-menu"
          :default-active="route.path"
          router
          :collapse="appStore.sidebarCollapsed"
          :collapse-transition="false"
          background-color="transparent"
          text-color="rgba(255,255,255,0.82)"
          active-text-color="#ffffff"
        >
          <SidebarMenu :items="visibleMenus" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="app-main">
      <el-header class="app-header" data-testid="app-topbar">
        <div class="header-left">
          <el-button text class="collapse-trigger" @click="appStore.toggleSidebar()">
            <el-icon :size="20">
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/" data-testid="app-breadcrumb">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.title">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <div class="user-chip">
            <el-avatar :size="34" :src="authStore.user?.avatar || avatarFallback" />
            <div class="user-meta">
              <div class="user-name">{{ authStore.displayName }}</div>
              <div class="user-role">{{ authStore.roleName }}</div>
            </div>
          </div>
          <el-dropdown trigger="click">
            <span class="dropdown-trigger">
              管理员
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/profile?tab=password')">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue'
import SidebarMenu from './SidebarMenu.vue'
import { appNavigation } from '@/config/navigation'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { filterMenuTree } from '@/utils/menu'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()
const avatarFallback = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const visibleMenus = computed(() => {
  const source = authStore.menuTree.length ? authStore.menuTree : appNavigation
  return filterMenuTree(source, authStore.permissionCodes)
})

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta.title && item.path !== '/')
    .map((item) => ({ title: String(item.meta.title) }))
)

async function handleLogout() {
  await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  await authStore.logout()
  await router.push('/login')
}
</script>

<style scoped lang="scss">
.app-layout {
  min-height: 100vh;
  background: var(--app-bg);
}

.app-sidebar {
  background:
    linear-gradient(180deg, rgba(7, 24, 39, 0.98), rgba(10, 31, 49, 0.98)),
    var(--app-sidebar);
  color: #fff;
  box-shadow: 18px 0 48px rgba(8, 19, 32, 0.18);
  overflow: hidden;
}

.brand {
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-copy {
  width: 100%;
  text-align: center;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.sidebar-scroll {
  height: calc(100vh - 76px);
}

.sidebar-menu {
  border-right: none;
  padding: 12px 10px 20px;
}

.app-main {
  min-width: 0;
}

.app-header {
  height: 76px;
  padding: 0 20px 0 18px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid rgba(219, 226, 239, 0.8);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.7);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-trigger {
  color: var(--app-text);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.06);
}

.user-name {
  font-size: 13px;
  font-weight: 700;
}

.user-role {
  font-size: 12px;
  color: var(--app-text-muted);
}

.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--app-text);
}

.app-content {
  padding: 18px;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  border-radius: 14px;
  margin: 4px 0;
  height: 46px;
  line-height: 46px;
}

:deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.22), rgba(106, 92, 255, 0.22));
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.06);
}

@media (max-width: 900px) {
  .app-content {
    padding: 12px;
  }
}
</style>
