<template>
  <div class="report-page">
    <section class="hero surface-panel">
      <div>
        <div class="hero-kicker">统计中心</div>
        <h1>统计报表</h1>
        <p>以任务、状态和类型为维度，形成一页式运营报表。</p>
      </div>
      <el-tag type="success">自动汇总</el-tag>
    </section>

    <section class="stats-grid">
      <StatCard :icon="Document" :value="formatNumber(summary.total)" label="任务总数" color="#3b82f6" />
      <StatCard :icon="CircleCheck" :value="`${summary.successRate}%`" label="完成率" color="#22c55e" />
      <StatCard :icon="Loading" :value="formatNumber(summary.running)" label="执行中" color="#f59e0b" />
      <StatCard :icon="TrendCharts" :value="formatNumber(summary.failed)" label="失败任务" color="#ef4444" />
    </section>

    <section class="content-grid">
      <div class="surface-panel chart-panel">
        <div class="panel-head"><h2>任务类型分布</h2><p>展示不同业务类型的任务数量。</p></div>
        <div ref="typeChartRef" class="chart"></div>
      </div>
      <div class="surface-panel chart-panel">
        <div class="panel-head"><h2>任务状态分布</h2><p>展示任务状态的整体健康度。</p></div>
        <div ref="statusChartRef" class="chart"></div>
      </div>
    </section>

    <section class="surface-panel table-panel">
      <div class="panel-head">
        <div>
          <h2>机器人负载概览</h2>
          <p>按任务数排序，查看哪些机器人承担了更多工作。</p>
        </div>
      </div>
      <el-table :data="robotRows" :loading="loading" height="420">
        <el-table-column prop="name" label="机器人" width="150" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><el-tag :type="robotTag(row.status)">{{ robotText(row.status) }}</el-tag></template>
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
import * as echarts from 'echarts'
import { CircleCheck, Document, Loading, TrendCharts } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getRobots } from '@/api/robots'
import { getTasks } from '@/api/tasks'
import { demoRobots, demoTasks } from '@/mock/demo-data'
import { buildTaskStatusSeries, buildTaskTypeSeries } from '@/utils/insights'
import { formatDateTime, formatNumber } from '@/utils/format'
import type { RobotItem, TaskItem } from '@/types/domain'

const loading = ref(false)
const tasks = ref<TaskItem[]>(demoTasks)
const robotRows = ref<RobotItem[]>(demoRobots)
const typeChartRef = ref<HTMLDivElement>()
const statusChartRef = ref<HTMLDivElement>()
const typeChart = ref<echarts.ECharts | null>(null)
const statusChart = ref<echarts.ECharts | null>(null)

const summary = computed(() => {
  const total = tasks.value.length
  const running = tasks.value.filter((item) => item.status === 'running').length
  const failed = tasks.value.filter((item) => item.status === 'failed').length
  const successRate = total ? Math.round((tasks.value.filter((item) => item.status === 'completed').length / total) * 1000) / 10 : 0
  return { total, running, failed, successRate }
})

function robotText(status: RobotItem['status']) { return { online: '在线', offline: '离线', busy: '忙碌', disabled: '禁用' }[status] }
function robotTag(status: RobotItem['status']) { return { online: 'success', busy: 'warning', offline: 'info', disabled: 'danger' }[status] }

async function loadData() {
  loading.value = true
  try {
    const [taskRes, robotRes] = await Promise.all([getTasks(), getRobots()])
    tasks.value = taskRes.data?.list?.length ? taskRes.data.list : demoTasks
    robotRows.value = robotRes.data?.list?.length ? robotRes.data.list : demoRobots
  } catch {
    tasks.value = demoTasks
    robotRows.value = demoRobots
  } finally {
    loading.value = false
    renderCharts()
  }
}

function renderCharts() {
  if (!typeChartRef.value || !statusChartRef.value) return
  typeChart.value?.dispose()
  statusChart.value?.dispose()
  typeChart.value = echarts.init(typeChartRef.value)
  statusChart.value = echarts.init(statusChartRef.value)

  typeChart.value.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['38%', '70%'], data: buildTaskTypeSeries(tasks.value), label: { formatter: '{b}\n{d}%' } }]
  })
  statusChart.value.setOption({
    tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['38%', '70%'], data: buildTaskStatusSeries(tasks.value), label: { formatter: '{b}\n{d}%' } }]
  })
}

onMounted(() => { void loadData() })
onBeforeUnmount(() => { typeChart.value?.dispose(); statusChart.value?.dispose() })
</script>

<style scoped lang="scss">
.report-page { display: grid; gap: 16px; }
.hero, .chart-panel, .table-panel { padding: 20px; }
.hero { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.hero-kicker { font-size: 12px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--app-text-muted); }
.hero h1, .hero p { margin: 0; }
.hero p { margin-top: 8px; color: var(--app-text-muted); }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.content-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.chart { height: 320px; }
.panel-head { margin-bottom: 14px; }
.panel-head h2, .panel-head p { margin: 0; }
.panel-head p { margin-top: 6px; color: var(--app-text-muted); }
@media (max-width: 900px) { .hero, .stats-grid, .content-grid { grid-template-columns: 1fr; } .hero { flex-direction: column; } }
</style>
