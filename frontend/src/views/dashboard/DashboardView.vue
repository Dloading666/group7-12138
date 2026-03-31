<template>
  <div class="dashboard-page" data-testid="dashboard-page">
    <section class="hero surface-panel" data-testid="dashboard-hero">
      <div class="hero-copy">
        <div class="hero-kicker">首页</div>
        <h1>欢迎回来，{{ authStore.roleName }}！</h1>
        <p>{{ todayText }}，系统运行正常。</p>
      </div>
      <el-button
        v-permission="'task:create'"
        type="primary"
        size="large"
        @click="router.push({ path: '/tasks', query: { create: '1' } })"
      >
        + 创建任务
      </el-button>
    </section>

    <section class="stats-grid">
      <div data-testid="dashboard-kpi-total">
        <StatCard
          :icon="Document"
          :value="formatNumber(overview.totalTasks)"
          label="总任务数"
          color="#3380ff"
        />
      </div>
      <div data-testid="dashboard-kpi-running">
        <StatCard
          :icon="Loading"
          :value="formatNumber(overview.runningTasks)"
          label="执行中"
          color="#58c73d"
        />
      </div>
      <div data-testid="dashboard-kpi-robot">
        <StatCard
          :icon="Cpu"
          :value="formatNumber(overview.robotCount)"
          label="机器人数量"
          color="#7a3ce7"
        />
      </div>
      <div data-testid="dashboard-kpi-success-rate">
        <StatCard
          :icon="CircleCheck"
          :value="formatPercent(overview.successRate)"
          label="成功率"
          color="#f8b31a"
        />
      </div>
    </section>

    <section class="charts-grid">
      <div class="surface-panel chart-panel">
        <div class="panel-head">
          <h2>任务执行趋势</h2>
          <span>最近 7 天</span>
        </div>
        <div ref="lineChartRef" class="chart" data-testid="dashboard-trend-chart"></div>
      </div>

      <div class="surface-panel chart-panel narrow">
        <div class="panel-head">
          <h2>任务状态分布</h2>
          <span>当前</span>
        </div>
        <div ref="pieChartRef" class="chart" data-testid="dashboard-status-chart"></div>
      </div>
    </section>

    <section class="surface-panel table-panel" data-testid="dashboard-recent-tasks">
      <div class="panel-head table-head">
        <div>
          <h2>最近任务</h2>
          <p>查看最近创建和执行中的任务。</p>
        </div>
        <el-button type="primary" plain size="small" @click="router.push('/tasks')">
          查看更多
        </el-button>
      </div>

      <el-table :data="recentTasks" height="320">
        <el-table-column prop="taskId" label="任务 ID" width="160" />
        <el-table-column prop="name" label="任务名称" min-width="180" />
        <el-table-column prop="type" label="任务类型" width="130" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="180">
          <template #default="{ row }">
            <el-progress
              :percentage="row.progress"
              :status="progressStatus(row.status)"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { CircleCheck, Cpu, Document, Loading } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { getDashboardOverview } from '@/api/dashboard'
import { demoDashboard } from '@/mock/demo-data'
import { formatDateTime, formatNumber, formatPercent } from '@/utils/format'
import type { DashboardOverview, TaskItem } from '@/types/domain'

const authStore = useAuthStore()
const router = useRouter()
const lineChartRef = ref<HTMLDivElement>()
const pieChartRef = ref<HTMLDivElement>()
const overview = ref<DashboardOverview>(demoDashboard)
const recentTasks = ref<TaskItem[]>(demoDashboard.recentTasks)

let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
let fallbackNotified = false

const todayText = computed(() => {
  const date = new Date()
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `今天是 ${date.getFullYear()} 年 ${String(date.getMonth() + 1).padStart(2, '0')} 月 ${String(date.getDate()).padStart(2, '0')} 日 ${days[date.getDay()]}`
})

function statusText(status: TaskItem['status']) {
  const map = {
    pending: '等待中',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
    stopped: '已停止'
  } as const
  return map[status] || status
}

function statusTag(status: TaskItem['status']) {
  const map = {
    pending: 'info',
    running: 'primary',
    completed: 'success',
    failed: 'danger',
    stopped: 'warning'
  } as const
  return map[status] || 'info'
}

function progressStatus(status: TaskItem['status']) {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return undefined
}

async function loadOverview() {
  try {
    const res = await getDashboardOverview()
    if (res.code === 200 && res.data) {
      overview.value = res.data
      recentTasks.value = res.data.recentTasks || []
      fallbackNotified = false
      return
    }
  } catch {
    // fall back to demo data below
  }

  if (!fallbackNotified) {
    ElMessage.warning('仪表盘服务暂时不可用，当前展示的是演示数据')
    fallbackNotified = true
  }
  overview.value = demoDashboard
  recentTasks.value = demoDashboard.recentTasks
}

function renderCharts() {
  if (!lineChartRef.value || !pieChartRef.value) return

  lineChart = echarts.init(lineChartRef.value)
  pieChart = echarts.init(pieChartRef.value)
  const totalStatusCount = overview.value.statusDistribution.reduce((sum, item) => sum + Number(item.value || 0), 0)

  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { top: 32, left: 32, right: 20, bottom: 20, containLabel: true },
    legend: { top: 0, data: ['执行次数', '成功次数'] },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '执行次数',
        type: 'line',
        smooth: true,
        data: overview.value.trend,
        symbolSize: 7,
        lineStyle: { width: 3 }
      },
      {
        name: '成功次数',
        type: 'line',
        smooth: true,
        data: overview.value.successTrend,
        symbolSize: 7,
        lineStyle: { width: 3 }
      }
    ]
  })

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: {
      left: 'center',
      bottom: 4,
      icon: 'roundRect',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: {
        color: '#5f7596',
        fontSize: 13
      }
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '39%',
        style: {
          text: String(totalStatusCount),
          fill: '#12203a',
          fontSize: 34,
          fontWeight: 800,
          textAlign: 'center'
        }
      },
      {
        type: 'text',
        left: 'center',
        top: '51%',
        style: {
          text: '任务总量',
          fill: '#7a8fab',
          fontSize: 13,
          fontWeight: 500,
          textAlign: 'center'
        }
      }
    ],
    series: [
      {
        type: 'pie',
        radius: ['50%', '71%'],
        center: ['50%', '42%'],
        data: overview.value.statusDistribution,
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 6,
          label: {
            show: true,
            formatter: '{b}\n{c} 项',
            fontSize: 16,
            fontWeight: 700,
            color: '#12203a'
          }
        }
      }
    ]
  })
}

function resizeCharts() {
  lineChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await loadOverview()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  display: grid;
  gap: 18px;
}

.hero {
  min-height: 138px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #0c2235;
  color: #fff;
}

.hero-copy h1 {
  margin: 8px 0;
  font-size: clamp(28px, 3vw, 44px);
}

.hero-copy p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.66);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.charts-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(340px, 1fr);
  gap: 16px;
}

.chart-panel,
.table-panel {
  padding: 20px;
}

.chart-panel {
  min-height: 440px;
}

.chart-panel.narrow {
  display: flex;
  flex-direction: column;
}

.chart {
  width: 100%;
  height: 360px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  font-size: 18px;
}

.panel-head span,
.panel-head p {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 13px;
}

.table-head {
  margin-bottom: 8px;
}

@media (max-width: 1200px) {
  .stats-grid,
  .charts-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .stats-grid,
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
