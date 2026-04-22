<template>
  <div class="dashboard-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>首页总览</h2>
        <p>把任务、机器人和执行趋势放在一个工作台里，方便你一眼判断系统是否顺畅运转。</p>
      </div>
      <div class="page-header-actions">
        <el-button v-if="canViewTaskList" @click="goTo('/task/list')">任务列表</el-button>
        <el-button v-if="canCreateCollectTask" type="primary" @click="createTask">
          <el-icon><Plus /></el-icon>
          新增任务
        </el-button>
      </div>
    </div>

    <div class="page-section dashboard-hero">
      <div class="hero-copy">
        <el-tag effect="dark" class="hero-tag">{{ roleDisplayName }}</el-tag>
        <h3>{{ greetingText }}</h3>
        <p>{{ heroDescription }}</p>
      </div>
      <div class="hero-metrics">
        <div>
          <span>平均耗时</span>
          <strong>{{ formatDuration(overview.avgDuration) }}</strong>
        </div>
        <div>
          <span>待执行任务</span>
          <strong>{{ overview.pending }}</strong>
        </div>
        <div>
          <span>在线机器人</span>
          <strong>{{ overview.onlineRobots }}</strong>
        </div>
      </div>
    </div>

    <el-alert
      v-if="!isAdmin"
      class="page-section"
      title="当前账号以查看权限为主"
      type="info"
      :closable="false"
      show-icon
      :description="permissionNotice"
    />

    <div v-if="visibleStats.length" class="stats-grid dashboard-stats-grid">
      <div v-for="item in visibleStats" :key="item.title" class="stat-tile dashboard-stat" :class="item.tone" @click="goTo(item.path)">
        <div class="dashboard-stat-head">
          <span class="stat-icon-shell">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="stat-label">{{ item.title }}</span>
        </div>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="page-section padded chart-card large-chart-card">
        <div class="section-heading compact-heading">
          <div>
            <h3>近 7 天任务趋势</h3>
            <p>基于最近拉取到的任务记录生成，帮助快速判断执行量与完成量是否同步。</p>
          </div>
        </div>
        <div ref="lineChartRef" class="chart-shell"></div>
      </div>

      <div class="page-section padded chart-card">
        <div class="section-heading compact-heading">
          <div>
            <h3>任务状态分布</h3>
            <p>实时读取后端统计结果。</p>
          </div>
        </div>
        <div ref="pieChartRef" class="chart-shell"></div>
      </div>
    </div>

    <div v-if="canViewTaskList" class="page-section padded">
      <div class="section-heading compact-heading">
        <div>
          <h3>最近任务</h3>
          <p>保留最近 8 条记录，便于从概览页快速进入详情排查。</p>
        </div>
        <el-button @click="goTo('/task/list')">查看全部</el-button>
      </div>

      <el-table :data="recentTasks" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="taskId" label="任务编号" min-width="170" show-overflow-tooltip />
        <el-table-column prop="name" label="任务名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="130">
          <template #default="{ row }">
            {{ getTypeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="180">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :stroke-width="10" :status="getProgressStatus(row.progress)" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="goTo(`/task/detail/${row.id}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { CircleCheck, Cpu, List, Loading, Plus } from '@element-plus/icons-vue'
import { getRobotStats } from '../api/robot.js'
import { getTaskList, getTaskStats } from '../api/task.js'

const router = useRouter()
const injectedIsAdmin = inject('isAdmin', ref(false))
const injectedUserRole = inject('userRole', ref('USER'))
const injectedHasPermission = inject('hasPermission', () => false)

const loading = ref(false)
const recentTasks = ref([])
const lineChartRef = ref(null)
const pieChartRef = ref(null)
const lineChartInstance = ref(null)
const pieChartInstance = ref(null)
const resizeHandler = ref(null)

const chartState = ref({
  trendLabels: [],
  trendTotal: [],
  trendCompleted: [],
  pieData: []
})

const overview = ref({
  pending: 0,
  avgDuration: 0,
  onlineRobots: 0
})

const stats = ref([
  { title: '任务总数', value: '-', icon: List, hint: '全部任务', tone: 'accent-blue', path: '/task/list', permission: 'task:list' },
  { title: '执行中', value: '-', icon: Loading, hint: '仍在运行中的任务', tone: 'accent-green', path: '/task/list', permission: 'task:list' },
  { title: '机器人数量', value: '-', icon: Cpu, hint: '当前已注册机器人', tone: 'accent-amber', path: '/robot/list', permission: 'robot:list' },
  { title: '成功率', value: '-', icon: CircleCheck, hint: '已完成任务 / 总任务', tone: 'accent-red', path: '/statistics/report', permission: 'statistics:report' }
])

const isAdmin = computed(() => Boolean(injectedIsAdmin?.value ?? injectedIsAdmin))
const userRole = computed(() => injectedUserRole?.value ?? injectedUserRole ?? 'USER')
const hasPermission = (permissionCode) => {
  if (!permissionCode) {
    return true
  }
  if (typeof injectedHasPermission === 'function') {
    return injectedHasPermission(permissionCode)
  }
  return false
}
const localUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch (error) {
    return {}
  }
})

const roleDisplayName = computed(() => {
  const map = {
    ADMIN: '系统管理员',
    USER: '普通用户',
    GUEST: '访客'
  }
  return map[userRole.value] || '平台成员'
})

const canViewTaskList = computed(() => hasPermission('task:view') && hasPermission('task:list'))
const canCreateCollectTask = computed(() => hasPermission('task:view') && hasPermission('task:list'))
const canViewRobotList = computed(() => hasPermission('robot:view') && hasPermission('robot:list'))
const canViewStatisticsReport = computed(() => hasPermission('statistics:view') && hasPermission('statistics:report'))
const visibleStats = computed(() => stats.value.filter((item) => !item.permission || hasPermission(item.permission)))

const greetingText = computed(() => {
  const name = localUser.value.realName || localUser.value.username || '伙伴'
  return `欢迎回来，${name}`
})

const currentDate = computed(() => {
  const now = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${now.getFullYear()}年${String(now.getMonth() + 1).padStart(2, '0')}月${String(now.getDate()).padStart(2, '0')}日 ${weekdays[now.getDay()]}`
})

const availableShortcuts = computed(() => {
  const shortcuts = []
  if (canCreateCollectTask.value) {
    shortcuts.push('新增任务')
  }
  if (canViewTaskList.value) {
    shortcuts.push('任务列表')
  }
  if (canViewRobotList.value) {
    shortcuts.push('机器人管理')
  }
  if (canViewStatisticsReport.value) {
    shortcuts.push('统计报表')
  }
  return shortcuts
})

const heroDescription = computed(() => {
  if (!availableShortcuts.value.length) {
    return `${currentDate.value}，这里是今天的运行概览。当前账号暂未分配业务模块权限，可先查看首页信息，后续由管理员按需开通访问范围。`
  }
  return `${currentDate.value}，这里是今天的运行概览。你可以从这里直接跳到${availableShortcuts.value.join('、')}。`
})

const permissionNotice = computed(() => {
  if (!availableShortcuts.value.length) {
    return '当前账号尚未分配业务模块权限，现阶段仅可访问首页概览。如需访问任务、流程、机器人或统计页面，请联系管理员在角色权限树中勾选对应项。'
  }
  return `当前账号已开放 ${availableShortcuts.value.join('、')} 等入口；未勾选的页面和操作默认不会显示，也无法访问。`
})

const loadDashboardData = async () => {
  loading.value = true
  try {
    const taskStatsFallback = { data: { total: 0, running: 0, completed: 0, pending: 0, failed: 0, avgDuration: 0 } }
    const robotStatsFallback = { data: { total: 0, online: 0, running: 0 } }
    const taskListFallback = { data: { content: [] } }
    const canLoadTaskOverview = canViewTaskList.value || canViewStatisticsReport.value

    const [taskStatsRes, robotStatsRes, tasksRes] = await Promise.all([
      canLoadTaskOverview
        ? getTaskStats({ silent: true }).catch(() => taskStatsFallback)
        : Promise.resolve(taskStatsFallback),
      canViewRobotList.value
        ? getRobotStats({ silent: true }).catch(() => robotStatsFallback)
        : Promise.resolve(robotStatsFallback),
      canViewTaskList.value
        ? getTaskList({ page: 1, size: 50 }, { silent: true }).catch(() => taskListFallback)
        : Promise.resolve(taskListFallback)
    ])

    const taskStats = taskStatsRes.data || {}
    const robotStats = robotStatsRes.data || {}
    const taskRows = tasksRes.data?.content || []
    const total = taskStats.total || 0
    const completed = taskStats.completed || 0
    const successRate = total > 0 ? `${((completed / total) * 100).toFixed(1)}%` : '0.0%'

    stats.value = [
      { title: '任务总数', value: `${total}`, icon: List, hint: '全部任务', tone: 'accent-blue', path: '/task/list', permission: 'task:list' },
      { title: '执行中', value: `${taskStats.running || 0}`, icon: Loading, hint: '仍在运行中的任务', tone: 'accent-green', path: '/task/list', permission: 'task:list' },
      { title: '机器人数量', value: `${robotStats.total || 0}`, icon: Cpu, hint: '当前已注册机器人', tone: 'accent-amber', path: '/robot/list', permission: 'robot:list' },
      { title: '成功率', value: successRate, icon: CircleCheck, hint: '已完成任务 / 总任务', tone: 'accent-red', path: '/statistics/report', permission: 'statistics:report' }
    ]

    overview.value = {
      pending: taskStats.pending || 0,
      avgDuration: taskStats.avgDuration || 0,
      onlineRobots: robotStats.online || robotStats.running || 0
    }

    recentTasks.value = taskRows.slice(0, 8)
    chartState.value = buildChartState(taskRows, taskStats)

    await nextTick()
    renderCharts()
  } catch (error) {
    console.error('加载首页数据失败:', error)
  } finally {
    loading.value = false
  }
}

const buildChartState = (tasks, taskStats) => {
  const today = new Date()
  const labels = []
  const totals = []
  const completedList = []

  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    const key = date.toISOString().slice(0, 10)
    const label = `${date.getMonth() + 1}/${date.getDate()}`
    labels.push(label)

    const rows = tasks.filter((task) => String(task.createTime || '').slice(0, 10) === key)
    totals.push(rows.length)
    completedList.push(rows.filter((task) => task.status === 'completed').length)
  }

  return {
    trendLabels: labels,
    trendTotal: totals,
    trendCompleted: completedList,
    pieData: [
      { value: taskStats.completed || 0, name: '已完成' },
      { value: taskStats.running || 0, name: '执行中' },
      { value: taskStats.pending || 0, name: '待执行' },
      { value: taskStats.failed || 0, name: '失败' }
    ]
  }
}

const renderCharts = () => {
  if (!lineChartRef.value || !pieChartRef.value) {
    return
  }

  lineChartInstance.value?.dispose()
  pieChartInstance.value?.dispose()

  lineChartInstance.value = echarts.init(lineChartRef.value)
  pieChartInstance.value = echarts.init(pieChartRef.value)

  lineChartInstance.value.setOption({
    color: ['#0f766e', '#2563eb'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['新建任务', '完成任务'] },
    grid: { left: 24, right: 16, top: 40, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: chartState.value.trendLabels,
      axisLine: { lineStyle: { color: '#cbd5e1' } }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#e2e8f0' } }
    },
    series: [
      {
        name: '新建任务',
        type: 'line',
        smooth: true,
        data: chartState.value.trendTotal,
        areaStyle: { color: 'rgba(15, 118, 110, 0.12)' }
      },
      {
        name: '完成任务',
        type: 'line',
        smooth: true,
        data: chartState.value.trendCompleted,
        areaStyle: { color: 'rgba(37, 99, 235, 0.10)' }
      }
    ]
  })

  pieChartInstance.value.setOption({
    color: ['#10b981', '#3b82f6', '#f59e0b', '#ef4444'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, left: 'center' },
    series: [
      {
        type: 'pie',
        radius: ['48%', '72%'],
        avoidLabelOverlap: false,
        label: { formatter: '{b}\n{d}%' },
        data: chartState.value.pieData
      }
    ]
  })

  resizeHandler.value = () => {
    lineChartInstance.value?.resize()
    pieChartInstance.value?.resize()
  }
  window.addEventListener('resize', resizeHandler.value)
}

const goTo = (path) => {
  if (path) {
    router.push(path)
  }
}

const createTask = () => {
  router.push('/task/list?create=1')
}

const getStatusText = (status) => {
  const map = {
    pending: '待执行',
    running: '执行中',
    completed: '已完成',
    failed: '失败'
  }
  return map[status] || status || '-'
}

const getStatusType = (status) => {
  const map = {
    pending: 'info',
    running: 'warning',
    completed: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}

const getProgressStatus = (progress) => {
  if (progress === 100) return 'success'
  if ((progress || 0) < 30) return 'exception'
  return undefined
}

const getTypeText = (type) => {
  const map = {
    crawl: '网页采集',
    spider: '网页采集',
    ai_workflow: '历史 AI 工作流',
    workflow: '流程任务',
    report: '报表任务',
    'data-collection': '数据采集',
    'web-crawl': '网站抓取'
  }
  return map[type] || type || '-'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatDuration = (seconds) => {
  const value = Number(seconds || 0)
  if (!value) return '—'
  if (value < 60) return `${value.toFixed(0)} 秒`
  if (value < 3600) return `${Math.floor(value / 60)} 分 ${Math.round(value % 60)} 秒`
  return `${Math.floor(value / 3600)} 小时 ${Math.floor((value % 3600) / 60)} 分`
}

onMounted(async () => {
  await loadDashboardData()
})

onBeforeUnmount(() => {
  if (resizeHandler.value) {
    window.removeEventListener('resize', resizeHandler.value)
  }
  lineChartInstance.value?.dispose()
  pieChartInstance.value?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  padding: 4px;
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 20px;
  padding: 28px;
  color: #fff;
  background: #1f2633;
  border-radius: 24px;
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.24);
}

.hero-copy h3 {
  margin: 14px 0 10px;
  font-size: 30px;
  line-height: 1.2;
}

.hero-copy p {
  margin: 0;
  max-width: 720px;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.8;
}

.hero-tag {
  border: none;
  background: rgba(255, 255, 255, 0.18);
}

.hero-metrics {
  display: grid;
  gap: 12px;
}

.hero-metrics div {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.hero-metrics span {
  display: block;
  margin-bottom: 6px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.hero-metrics strong {
  font-size: 24px;
  color: #fff;
}

.dashboard-stats-grid {
  margin-top: 18px;
}

.dashboard-stat {
  cursor: pointer;
}

.dashboard-stat-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat-icon-shell {
  display: inline-grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: #fff;
  background: rgba(15, 23, 42, 0.88);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.9fr);
  gap: 18px;
  margin-top: 18px;
}

.chart-card {
  min-height: 420px;
}

.chart-shell {
  width: 100%;
  height: 340px;
}

.compact-heading {
  margin-bottom: 18px;
}

@media (max-width: 1200px) {
  .dashboard-hero,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
