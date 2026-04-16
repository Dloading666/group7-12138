<template>
  <div class="statistics-report app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>统计报表</h2>
        <p>使用真实任务、机器人与采集结果数据生成执行趋势和分布报表。</p>
      </div>
      <div class="page-header-actions">
        <el-select v-model="days" style="width: 140px" @change="loadReportData">
          <el-option label="近 7 天" :value="7" />
          <el-option label="近 14 天" :value="14" />
          <el-option label="近 30 天" :value="30" />
        </el-select>
      </div>
    </div>

    <div class="stats-grid dashboard-stats-grid page-section">
      <div class="stat-tile accent-blue">
        <span class="stat-label">任务总数</span>
        <strong>{{ overview.totalTasks }}</strong>
        <small>系统内全部任务</small>
      </div>
      <div class="stat-tile accent-green">
        <span class="stat-label">已完成</span>
        <strong>{{ overview.completedTasks }}</strong>
        <small>执行成功的任务</small>
      </div>
      <div class="stat-tile accent-red">
        <span class="stat-label">失败任务</span>
        <strong>{{ overview.failedTasks }}</strong>
        <small>需要排查的任务</small>
      </div>
      <div class="stat-tile accent-amber">
        <span class="stat-label">成功率</span>
        <strong>{{ formatRate(overview.successRate) }}</strong>
        <small>已完成 / 全部任务</small>
      </div>
    </div>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>任务执行趋势</span>
          </template>
          <div ref="lineChartRef" class="chart-shell"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>任务类型分布</span>
          </template>
          <div ref="pieChartRef" class="chart-shell"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>机器人执行统计</span>
          </template>
          <div ref="barChartRef" class="chart-shell"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>成功率趋势</span>
          </template>
          <div ref="successChartRef" class="chart-shell"></div>
        </el-card>
      </el-col>
    </el-row>

    <div class="page-section page-table-card">
      <el-table :data="detailRows" border>
        <el-table-column prop="date" label="日期" width="150" />
        <el-table-column prop="total" label="总任务数" width="120" />
        <el-table-column prop="completed" label="已完成" width="120" />
        <el-table-column prop="failed" label="失败任务" width="120" />
        <el-table-column prop="successRate" label="成功率" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getAllRobots } from '../../api/robot.js'
import { getOverviewStats, getTaskTrendStats, getTaskTypeStats } from '../../api/statistics.js'

const days = ref(7)
const overview = ref({
  totalTasks: 0,
  completedTasks: 0,
  failedTasks: 0,
  successRate: 0
})
const detailRows = ref([])

const lineChartRef = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)
const successChartRef = ref(null)
const chartInstances = ref([])

const taskTypeMap = {
  'data-collection': '数据采集',
  ai_workflow: 'AI分析',
  workflow: 'AI分析',
  report: '报表生成',
  'data-sync': '数据同步',
  'web-crawl': '网站抓取'
}

const formatRate = (value) => `${Number(value || 0).toFixed(1)}%`

const renderCharts = (trendRows, typeRows, robots) => {
  chartInstances.value.forEach((instance) => instance.dispose())
  chartInstances.value = []

  if (!lineChartRef.value || !pieChartRef.value || !barChartRef.value || !successChartRef.value) {
    return
  }

  const line = echarts.init(lineChartRef.value)
  const pie = echarts.init(pieChartRef.value)
  const bar = echarts.init(barChartRef.value)
  const success = echarts.init(successChartRef.value)
  chartInstances.value = [line, pie, bar, success]

  const labels = trendRows.map((item) => item.date)
  const totals = trendRows.map((item) => item.total ?? item.count ?? 0)
  const completed = trendRows.map((item) => item.completed ?? 0)
  const failed = trendRows.map((item) => item.failed ?? 0)
  const successRates = trendRows.map((item) => {
    const total = Number(item.total ?? item.count ?? 0)
    return total > 0 ? Number(((Number(item.completed ?? 0) / total) * 100).toFixed(1)) : 0
  })

  line.setOption({
    color: ['#2563eb', '#10b981', '#ef4444'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['总任务', '已完成', '失败任务'] },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value' },
    series: [
      { name: '总任务', type: 'line', smooth: true, data: totals },
      { name: '已完成', type: 'line', smooth: true, data: completed },
      { name: '失败任务', type: 'line', smooth: true, data: failed }
    ]
  })

  pie.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '58%',
      data: typeRows.map((item) => ({
        value: item.count,
        name: taskTypeMap[item.type] || item.type
      }))
    }]
  })

  bar.setOption({
    color: ['#0ea5e9', '#10b981', '#ef4444'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['总任务', '成功任务', '失败任务'] },
    xAxis: {
      type: 'category',
      data: robots.map((robot) => robot.name)
    },
    yAxis: { type: 'value' },
    series: [
      { name: '总任务', type: 'bar', data: robots.map((robot) => robot.totalTasks || 0) },
      { name: '成功任务', type: 'bar', data: robots.map((robot) => robot.successTasks || 0) },
      { name: '失败任务', type: 'bar', data: robots.map((robot) => robot.failedTasks || 0) }
    ]
  })

  success.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value', max: 100 },
    series: [{
      data: successRates,
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(16, 185, 129, 0.18)' }
    }]
  })

  const resize = () => chartInstances.value.forEach((instance) => instance.resize())
  window.onresize = resize
}

const loadReportData = async () => {
  const [overviewRes, trendRes, typeRes, robotRes] = await Promise.all([
    getOverviewStats(),
    getTaskTrendStats({ days: days.value }),
    getTaskTypeStats(),
    getAllRobots()
  ])

  overview.value = overviewRes.data || overview.value
  detailRows.value = (trendRes.data || []).map((item) => {
    const total = Number(item.total ?? item.count ?? 0)
    const completed = Number(item.completed ?? 0)
    return {
      date: item.date,
      total,
      completed,
      failed: Number(item.failed ?? 0),
      successRate: total > 0 ? `${((completed / total) * 100).toFixed(1)}%` : '0.0%'
    }
  })

  await nextTick()
  renderCharts(trendRes.data || [], typeRes.data || [], robotRes.data || [])
}

onMounted(() => {
  loadReportData()
})

onBeforeUnmount(() => {
  chartInstances.value.forEach((instance) => instance.dispose())
  chartInstances.value = []
  window.onresize = null
})
</script>

<style scoped lang="scss">
.chart-row {
  margin-top: 20px;
}

.chart-shell {
  height: 350px;
}
</style>
