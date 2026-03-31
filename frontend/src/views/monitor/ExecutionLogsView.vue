<template>
  <div class="monitor-page">
    <section class="hero surface-panel">
      <div>
        <div class="hero-kicker">监控中心</div>
        <h1>执行日志</h1>
        <p>围绕任务与机器人生成执行轨迹，帮助快速定位失败、重试和离线问题。</p>
      </div>
      <div class="hero-actions">
        <el-tag type="success" effect="dark">实时刷新</el-tag>
        <el-button type="primary" plain :loading="loading" @click="loadData">刷新数据</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <StatCard :icon="Document" :value="formatNumber(summary.total)" label="日志总数" color="#3b82f6" />
      <StatCard :icon="WarningFilled" :value="formatNumber(summary.warn)" label="告警日志" color="#f59e0b" />
      <StatCard :icon="CircleClose" :value="formatNumber(summary.error)" label="错误日志" color="#ef4444" />
      <StatCard :icon="Finished" :value="formatNumber(summary.info)" label="信息日志" color="#22c55e" />
    </section>

    <section class="content-grid">
      <div class="surface-panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>日志级别分布</h2>
            <p>从当前筛选结果实时重绘。</p>
          </div>
          <el-tag>{{ visibleLogs.length }} 条可见</el-tag>
        </div>
        <div ref="chartRef" class="chart"></div>
      </div>

      <div class="surface-panel filter-panel">
        <div class="panel-head">
          <div>
            <h2>筛选条件</h2>
            <p>按关键字、级别、来源快速定位日志。</p>
          </div>
        </div>

        <el-form :model="filters" label-width="74px" class="filter-form">
          <el-form-item label="关键字">
            <el-input v-model="filters.keyword" clearable placeholder="任务名、机器人、日志内容" />
          </el-form-item>
          <el-form-item label="级别">
            <el-select v-model="filters.level" clearable placeholder="全部" style="width: 100%">
              <el-option label="信息" value="INFO" />
              <el-option label="告警" value="WARN" />
              <el-option label="错误" value="ERROR" />
            </el-select>
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="filters.source" clearable placeholder="全部" style="width: 100%">
              <el-option label="任务" value="task" />
              <el-option label="机器人" value="robot" />
              <el-option label="系统" value="system" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="applyFilters">应用筛选</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="log-tips">
          <div class="tip-title">当前数据来源</div>
          <div class="tip-text">优先调用后端 `/monitor/logs`，如果接口暂未接入，则自动回退到任务与机器人状态生成的日志流。</div>
        </div>
      </div>
    </section>

    <section class="surface-panel table-panel">
      <div class="panel-head">
        <div>
          <h2>执行轨迹</h2>
          <p>可点击查看单条日志详情。</p>
        </div>
      </div>

      <el-table :data="visibleLogs" :loading="loading" height="560" @row-click="openDetail">
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.time) }}</template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="90">
          <template #default="{ row }">{{ sourceText(row.source) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="message" label="消息" min-width="260" show-overflow-tooltip />
        <el-table-column prop="taskName" label="任务" width="150" />
        <el-table-column prop="robotName" label="机器人" width="140" />
        <el-table-column prop="duration" label="耗时" width="100">
          <template #default="{ row }">{{ row.duration ? `${row.duration} 分钟` : '-' }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="detailVisible" title="日志详情" size="460px">
      <template v-if="currentLog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="时间">{{ formatDateTime(currentLog.time) }}</el-descriptions-item>
          <el-descriptions-item label="级别">{{ currentLog.level }}</el-descriptions-item>
          <el-descriptions-item label="来源">{{ sourceText(currentLog.source) }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ currentLog.title }}</el-descriptions-item>
          <el-descriptions-item label="消息">{{ currentLog.message }}</el-descriptions-item>
          <el-descriptions-item label="任务">{{ currentLog.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="机器人">{{ currentLog.robotName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentLog.duration ? `${currentLog.duration} 分钟` : '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { CircleClose, Document, Finished, WarningFilled } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { getExecutionLogs } from '@/api/monitor'
import { getRobots } from '@/api/robots'
import { getTasks } from '@/api/tasks'
import { demoRobots, demoTasks } from '@/mock/demo-data'
import { buildExecutionLogs, summarizeLogs } from '@/utils/insights'
import { formatDateTime, formatNumber } from '@/utils/format'
import type { ExecutionLogItem, RobotItem, TaskItem } from '@/types/domain'

const loading = ref(false)
const chartRef = ref<HTMLDivElement>()
const chart = ref<echarts.ECharts | null>(null)
const currentLog = ref<ExecutionLogItem | null>(null)
const detailVisible = ref(false)
const tasks = ref<TaskItem[]>(demoTasks)
const robots = ref<RobotItem[]>(demoRobots)
const logs = ref<ExecutionLogItem[]>([])
const summary = computed(() => summarizeLogs(logs.value))

const filters = reactive({
  keyword: '',
  level: '',
  source: ''
})

const visibleLogs = computed(() =>
  logs.value.filter((log) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const haystack = [log.title, log.message, log.taskName, log.robotName].filter(Boolean).join(' ').toLowerCase()
    const matchKeyword = !keyword || haystack.includes(keyword)
    const matchLevel = !filters.level || log.level === filters.level
    const matchSource = !filters.source || log.source === filters.source
    return matchKeyword && matchLevel && matchSource
  })
)

function levelTag(level: ExecutionLogItem['level']) {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

function sourceText(source: ExecutionLogItem['source']) {
  const map = { task: '任务', robot: '机器人', system: '系统' } as const
  return map[source]
}

async function loadData() {
  loading.value = true
  try {
    const [taskRes, robotRes, logRes] = await Promise.all([getTasks(), getRobots(), getExecutionLogs()])
    tasks.value = taskRes.data?.list?.length ? taskRes.data.list : demoTasks
    robots.value = robotRes.data?.list?.length ? robotRes.data.list : demoRobots
    logs.value = logRes.data?.list?.length ? logRes.data.list : buildExecutionLogs(tasks.value, robots.value)
  } catch {
    tasks.value = demoTasks
    robots.value = demoRobots
    logs.value = buildExecutionLogs(tasks.value, robots.value)
  } finally {
    loading.value = false
    renderChart()
  }
}

function applyFilters() {
  renderChart()
}

function resetFilters() {
  filters.keyword = ''
  filters.level = ''
  filters.source = ''
  renderChart()
}

function openDetail(row: ExecutionLogItem) {
  currentLog.value = row
  detailVisible.value = true
}

function renderChart() {
  if (!chartRef.value) return
  chart.value?.dispose()
  chart.value = echarts.init(chartRef.value)
  const items = visibleLogs.value
  const levelData = [
    { name: 'INFO', value: items.filter((item) => item.level === 'INFO').length },
    { name: 'WARN', value: items.filter((item) => item.level === 'WARN').length },
    { name: 'ERROR', value: items.filter((item) => item.level === 'ERROR').length }
  ]

  chart.value.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['38%', '70%'],
        center: ['50%', '45%'],
        data: levelData,
        label: { formatter: '{b}\n{d}%' }
      }
    ]
  })
}

watch(visibleLogs, () => renderChart(), { deep: true })

onMounted(() => {
  void loadData()
})

onBeforeUnmount(() => {
  chart.value?.dispose()
})
</script>

<style scoped lang="scss">
.monitor-page {
  display: grid;
  gap: 16px;
}

.hero,
.table-panel,
.chart-panel,
.filter-panel {
  padding: 20px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--app-text-muted);
}

.hero h1,
.hero p {
  margin: 0;
}

.hero p {
  margin-top: 8px;
  color: var(--app-text-muted);
  max-width: 680px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-grid,
.content-grid {
  display: grid;
  gap: 16px;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.content-grid {
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
}

.chart {
  height: 360px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2,
.panel-head p {
  margin: 0;
}

.panel-head p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.log-tips {
  margin-top: 16px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(59, 130, 246, 0.06);
}

.tip-title {
  font-weight: 700;
}

.tip-text {
  margin-top: 8px;
  color: var(--app-text-muted);
  line-height: 1.65;
}

@media (max-width: 1200px) {
  .stats-grid,
  .content-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .hero,
  .stats-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }
}
</style>
