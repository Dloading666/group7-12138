<template>
  <template v-for="item in items" :key="item.path || item.name">
    <el-sub-menu v-if="hasChildren(item)" :index="item.path || item.name">
      <template #title>
        <el-icon v-if="item.icon"><component :is="resolveIcon(item.icon)" /></el-icon>
        <span>{{ item.name }}</span>
      </template>
      <SidebarMenu :items="item.children || []" />
    </el-sub-menu>
    <el-menu-item v-else :index="item.path || ''">
      <el-icon v-if="item.icon"><component :is="resolveIcon(item.icon)" /></el-icon>
      <span>{{ item.name }}</span>
    </el-menu-item>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { iconMap, type IconName } from '@/utils/icons'
import type { AuthMenuNode } from '@/types/auth'

defineOptions({ name: 'SidebarMenu' })

const props = defineProps<{
  items: AuthMenuNode[]
}>()

const resolveIcon = (name?: string) => {
  if (!name) return iconMap.HomeFilled
  return iconMap[name as IconName] || iconMap.HomeFilled
}

const hasChildren = (item: AuthMenuNode) => Boolean(item.children && item.children.length)

const items = computed(() => props.items)
</script>

