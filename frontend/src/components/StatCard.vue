<template>
  <section class="stat-card">
    <div class="stat-icon" :style="{ background: color }">
      <el-icon :size="26"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  icon: any
  value: string
  label: string
  color: string
}>()
</script>

<style scoped lang="scss">
.stat-card {
  min-height: 110px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(219, 226, 239, 0.8);
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(18, 32, 58, 0.08);
}

.stat-icon {
  width: 60px;
  height: 60px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  color: #fff;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-label {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 13px;
}
</style>

