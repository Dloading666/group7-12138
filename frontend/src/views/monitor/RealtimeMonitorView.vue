<template>
  <div class="monitor-page">
    <section class="hero surface-panel">
      <div>
        <div class="hero-kicker">监控中心</div>
        <h1>实时监控</h1>
        <p>结合机器人心跳、任务态势和告警概览，持续刷新当前运行状态。</p>
      </div>
      <div class="hero-actions">
        <div class="live-pill"><span class="live-dot"></span> {{ lastUpdatedText }}</div>
        <el-button type="primary" plain :loading="loading" @click="loadData">立即刷新</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <StatCard :icon="Cpu" :value="formatNumber(summary.onlineRobots)" label="在线机器人" color="#22c55e" />
      <StatCard :icon="Loading" :value="formatNumber(summary.runningTasks)" label="运行中任务" color="#3b82f6" />
      <StatCard :icon="WarningFilled" :value="formatNumber(summary.alerts)" label="告警数量" color="#f59e0b" />
      <StatCard :icon="CircleCheck" :value="`${summary.successRate}%`" label="成功率" color="#7c3aed" />
    </section>

    <section class="surface-panel table-panel">
      <div class="panel-head">
        <div>
          <h2>机器人健康态势</h2>
          <p>每 8 秒自动刷新一次，兼容后端数据与本地回退数据。</p>
        </div>
      </div>

      <el-table :data="robots" :loading="loading" height="460">
        <el-table-column prop="name" label="机器人" width="150" />
        <el-table-column prop="robotId" label="编号" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskCount" label="任务数" width="100" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastHeartbeat) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" />
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CircleCheck, Cpu, Loading, WarningFilled } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getDashboardOverview } from '@/api/dashboard'
import { getRealtimeSnapshot } from '@/api/monitor'
import { getRobots } from '@/api/robots'
import { getTasks } from '@/api/tasks'
import { demoDashboard, demoRobots } from '@/mock/demo-data'
import { buildTaskSummary } from '@/utils/insights'
import { formatDateTime, formatNumber } from '@/utils/format'
import type { DashboardOverview, RobotItem, TaskItem } from '@/types/domain'

const loading = ref(false)
const overview = ref<DashboardOverview>(demoDashboard)
const robots = ref<RobotItem[]>(demoRobots)
const tasks = ref<TaskItem[]>(demoDashboard.recentTasks)
const lastUpdated = ref<Date>(new Date())

const summary = computed(() => {
  const stat = buildTaskSummary(overview.value, tasks.value, robots.value)
  return {
    onlineRobots: stat.robotsOnline,
    runningTasks: stat.running,
    alerts: robots.value.filter((item) => item.status === 'offline' || item.status === 'disabled').length,
    successRate: stat.success
  }
})

const lastUpdatedText = computed(() => `最后刷新：${formatDateTime(lastUpdated.value)}`)

function statusText(status: RobotItem['status']) {
  return { online: '在线', offline: '离线', busy: '忙碌', disabled: '禁用' }[status]
}

function statusTag(status: RobotItem['status']) {
  return { online: 'success', busy: 'warning', offline: 'info', disabled: 'danger' }[status]
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, robotsRes, taskRes, snapshotRes] = await Promise.all([
      getDashboardOverview(),
      getRobots(),
      getTasks(),
      getRealtimeSnapshot()
    ])
    overview.value = overviewRes.data || demoDashboard
    robots.value = robotsRes.data?.list?.length ? robotsRes.data.list : demoRobots
    tasks.value = taskRes.data?.list?.length ? taskRes.data.list : demoDashboard.recentTasks
    lastUpdated.value = snapshotRes.data?.updatedAt ? new Date(snapshotRes.data.updatedAt) : new Date()
  } catch {
    overview.value = demoDashboard
    robots.value = demoRobots
    tasks.value = demoDashboard.recentTasks
    lastUpdated.value = new Date()
  } finally {
    loading.value = false
  }
}

let timer: number | undefined

onMounted(() => {
  void loadData()
  timer = window.setInterval(() => void loadData(), 8000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<style scoped lang="scss">
.monitor-page { display: grid; gap: 16px; }
.hero, .table-panel { padding: 20px; }
.hero { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.hero-kicker { font-size: 12px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--app-text-muted); }
.hero h1, .hero p { margin: 0; }
.hero p { margin-top: 8px; color: var(--app-text-muted); max-width: 680px; }
.hero-actions { display: flex; align-items: center; gap: 10px; }
.live-pill { display: inline-flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: 999px; background: rgba(34, 197, 94, 0.08); }
.live-dot { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.65); animation: pulse 1.8s infinite; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.panel-head { margin-bottom: 14px; }
.panel-head h2, .panel-head p { margin: 0; }
.panel-head p { margin-top: 6px; color: var(--app-text-muted); }
@keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.55); } 70% { box-shadow: 0 0 0 10px rgba(34, 197, 94, 0); } 100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); } }
@media (max-width: 900px) { .hero, .stats-grid { grid-template-columns: 1fr; } .hero { flex-direction: column; } }
</style>
