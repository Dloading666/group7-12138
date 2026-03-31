<template>
  <div class="report-page">
    <section class="hero surface-panel">
      <div>
        <div class="hero-kicker">统计中心</div>
        <h1>数据查询</h1>
        <p>按任务时间、状态、类型和机器人筛选，快速定位运营结果。</p>
      </div>
      <div class="hero-actions"><el-button type="primary" plain :loading="loading" @click="loadData">刷新数据</el-button></div>
    </section>

    <section class="stats-grid">
      <StatCard :icon="Document" :value="formatNumber(summary.total)" label="任务总数" color="#3b82f6" />
      <StatCard :icon="CircleCheck" :value="`${summary.successRate}%`" label="完成率" color="#22c55e" />
      <StatCard :icon="Loading" :value="formatNumber(summary.running)" label="执行中" color="#f59e0b" />
      <StatCard :icon="TrendCharts" :value="formatNumber(summary.robots)" label="机器人数量" color="#7c3aed" />
    </section>

    <section class="surface-panel filter-panel">
      <div class="panel-head">
        <div>
          <h2>查询条件</h2>
          <p>接口未返回时会自动回退到本地任务数据。</p>
        </div>
      </div>
      <el-form :model="filters" inline class="filter-form">
        <el-form-item label="关键字"><el-input v-model="filters.keyword" clearable placeholder="任务名 / 编号" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="待执行" value="pending" /><el-option label="执行中" value="running" /><el-option label="已完成" value="completed" /><el-option label="已停止" value="stopped" /><el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="filters.priority" clearable placeholder="全部" style="width: 140px">
            <el-option label="高" value="high" /><el-option label="中" value="medium" /><el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilters">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="surface-panel table-panel">
      <el-table :data="filteredRows" :loading="loading" height="520">
        <el-table-column prop="taskId" label="任务编号" width="160" />
        <el-table-column prop="name" label="任务名称" min-width="180" />
        <el-table-column prop="type" label="任务类型" width="140" />
        <el-table-column prop="status" label="状态" width="110"><template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="priority" label="优先级" width="100"><template #default="{ row }"><el-tag effect="plain" :type="priorityTag(row.priority)">{{ priorityText(row.priority) }}</el-tag></template></el-table-column>
        <el-table-column prop="robotName" label="执行机器人" width="140" />
        <el-table-column prop="createTime" label="创建时间" width="180"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleCheck, Document, Loading, TrendCharts } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getRobots } from '@/api/robots'
import { getTasks } from '@/api/tasks'
import { demoRobots, demoTasks } from '@/mock/demo-data'
import { formatDateTime, formatNumber } from '@/utils/format'
import type { RobotItem, TaskItem } from '@/types/domain'

const loading = ref(false)
const rows = ref<TaskItem[]>(demoTasks)
const robots = ref<RobotItem[]>(demoRobots)
const filteredRows = ref<TaskItem[]>(demoTasks)

const filters = reactive({ keyword: '', status: '', priority: '' })

const summary = computed(() => {
  const total = rows.value.length
  const running = rows.value.filter((item) => item.status === 'running').length
  const successRate = total ? Math.round((rows.value.filter((item) => item.status === 'completed').length / total) * 1000) / 10 : 0
  return { total, running, successRate, robots: robots.value.length }
})

function statusText(status: TaskItem['status']) { return { pending: '待执行', running: '执行中', completed: '已完成', failed: '失败', stopped: '已停止' }[status] }
function statusTag(status: TaskItem['status']) { return { pending: 'info', running: 'primary', completed: 'success', failed: 'danger', stopped: 'warning' }[status] }
function priorityText(priority: TaskItem['priority']) { return { high: '高', medium: '中', low: '低' }[priority] }
function priorityTag(priority: TaskItem['priority']) { return { high: 'danger', medium: 'warning', low: 'info' }[priority] }

async function loadData() {
  loading.value = true
  try {
    const [taskRes, robotRes] = await Promise.all([getTasks(), getRobots()])
    rows.value = taskRes.data?.list?.length ? taskRes.data.list : demoTasks
    robots.value = robotRes.data?.list?.length ? robotRes.data.list : demoRobots
  } catch {
    rows.value = demoTasks
    robots.value = demoRobots
  } finally {
    loading.value = false
    applyFilters()
  }
}

function applyFilters() {
  filteredRows.value = rows.value.filter((row) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const matchKeyword = !keyword || [row.taskId, row.name, row.robotName].filter(Boolean).join(' ').toLowerCase().includes(keyword)
    const matchStatus = !filters.status || row.status === filters.status
    const matchPriority = !filters.priority || row.priority === filters.priority
    return matchKeyword && matchStatus && matchPriority
  })
}

function resetFilters() { filters.keyword = ''; filters.status = ''; filters.priority = ''; applyFilters() }

onMounted(() => { void loadData() })
</script>

<style scoped lang="scss">
.report-page { display: grid; gap: 16px; }
.hero, .filter-panel, .table-panel { padding: 20px; }
.hero { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; }
.hero-kicker { font-size: 12px; letter-spacing: 0.16em; text-transform: uppercase; color: var(--app-text-muted); }
.hero h1, .hero p { margin: 0; }
.hero p { margin-top: 8px; color: var(--app-text-muted); max-width: 680px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.panel-head { margin-bottom: 14px; }
.panel-head h2, .panel-head p { margin: 0; }
.panel-head p { margin-top: 6px; color: var(--app-text-muted); }
.filter-form { margin-top: 8px; }
@media (max-width: 900px) { .hero, .stats-grid { grid-template-columns: 1fr; } .hero { flex-direction: column; } }
</style>
