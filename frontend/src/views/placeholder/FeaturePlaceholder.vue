<template>
  <div class="placeholder surface-panel">
    <div class="placeholder-head">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
      <el-tag type="info">一期壳层</el-tag>
    </div>
    <el-empty description="该页面已占位，后续可继续补充业务逻辑与接口。" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title || '功能页'))
const description = computed(() => String(route.meta.description || '该功能页在当前一期先保留页面入口和权限控制。'))
</script>

<style scoped lang="scss">
.placeholder {
  min-height: 560px;
  padding: 24px;
}
.placeholder-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}
h1 {
  margin: 0;
  font-size: 26px;
}
p {
  margin: 8px 0 0;
  color: var(--app-text-muted);
}
</style>

