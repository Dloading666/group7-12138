<template>
  <div class="dashboard">
    <!-- 欢迎栏 -->
    <div class="welcome-banner">
      <div class="welcome-content">
        <div class="welcome-left">
          <h1 class="welcome-title">欢迎回来，{{ roleDisplayName }}！</h1>
          <p class="welcome-date">{{ currentDate }}，系统运行正常</p>
        </div>
        <div class="welcome-right" v-if="isAdmin">
          <el-button type="primary" class="create-task-btn" @click="createTask">
            <el-icon><Plus /></el-icon>
            创建任务
          </el-button>
        </div>
      </div>
    </div>

    <!-- 角色提示 - 普通用户显示 -->
    <el-alert
      v-if="!isAdmin"
      title="您当前为普通用户角色，只有查看权限"
      type="info"
      description="您可以查看系统仪表盘、任务执行记录、采集数据等，实现业务监督与统计分析。如需更多权限，请联系系统管理员。"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.title">
        <el-card class="stat-card clickable" :body-style="{ padding: '20px' }" @click="handleStatClick(item.path)">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: item.color }">
              <el-icon :size="30"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-title">{{ item.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>任务执行趋势</span>
            </div>
          </template>
          <div ref="lineChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>任务状态分布</span>
            </div>
          </template>
          <div ref="pieChart" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="table-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近任务</span>
              <el-button type="primary" size="small" @click="viewMoreTasks">查看更多</el-button>
            </div>
          </template>
          <el-table :data="recentTasks" style="width: 100%" v-loading="loading">
            <el-table-column prop="id" label="任务ID" width="100" />
            <el-table-column prop="name" label="任务名称" />
            <el-table-column prop="type" label="任务类型" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="progress" label="进度" width="180">
              <template #default="{ row }">
                <el-progress :percentage="row.progress" :status="getProgressStatus(row.progress)" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180" />
            <template #empty>
              <el-empty description="暂无任务数据" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, inject, computed } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getTaskStats, getTaskList } from '../api/task.js'
import { getRobotStats } from '../api/robot.js'

const router = useRouter()

// 获取用户权限
const isAdmin = inject('isAdmin')
const userRole = inject('userRole')

// 角色显示名称
const roleDisplayName = computed(() => {
  const roleMap = {
    'ADMIN': '系统管理员',
    'USER': '普通用户'
  }
  return roleMap[userRole.value] || '用户'
})

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  const weekDay = weekDays[now.getDay()]
  return `今天是${year}年${month}月${day}日 ${weekDay}`
})

// 创建任务
const createTask = () => {
  router.push('/task/create')
}

const lineChart = ref(null)
const pieChart = ref(null)

// 加载状态
const loading = ref(false)

// 统计数据
const stats = ref([
  { title: '总任务数', value: '-', icon: 'List', color: '#1890ff', path: '/task/list' },
  { title: '执行中', value: '-', icon: 'Loading', color: '#52c41a', path: '/task/list' },
  { title: '机器人数量', value: '-', icon: 'Cpu', color: '#722ed1', path: '/robot/list' },
  { title: '成功率', value: '-', icon: 'CircleCheck', color: '#faad14', path: '/statistics/report' }
])

// 最近任务列表
const recentTasks = ref([])

// 加载仪表盘数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    // 并行获取任务统计和机器人统计
    const [taskStatsRes, robotStatsRes, tasksRes] = await Promise.all([
      getTaskStats().catch(() => ({ data: { total: 0, running: 0, completed: 0, pending: 0 } })),
      getRobotStats().catch(() => ({ data: { total: 0, running: 0, stopped: 0 } })),
      getTaskList({ page: 1, size: 5 }).catch(() => ({ data: { content: [] } }))
    ])

    // 更新统计数据
    const taskStats = taskStatsRes.data || {}
    const robotStats = robotStatsRes.data || {}

    // 计算成功率
    const total = taskStats.total || 0
    const completed = taskStats.completed || 0
    const successRate = total > 0 ? ((completed / total) * 100).toFixed(1) : '0.0'

    stats.value = [
      { title: '总任务数', value: total.toLocaleString(), icon: 'List', color: '#1890ff', path: '/task/list' },
      { title: '执行中', value: (taskStats.running || 0).toString(), icon: 'Loading', color: '#52c41a', path: '/task/list' },
      { title: '机器人数量', value: (robotStats.total || 0).toString(), icon: 'Cpu', color: '#722ed1', path: '/robot/list' },
      { title: '成功率', value: `${successRate}%`, icon: 'CircleCheck', color: '#faad14', path: '/statistics/report' }
    ]

    // 更新最近任务
    const tasks = tasksRes.data?.content || []
    recentTasks.value = tasks.map(task => ({
      id: task.id,
      name: task.name,
      type: task.type || '未知类型',
      status: getStatusText(task.status),
      progress: task.progress || 0,
      createTime: task.createTime
    }))
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 状态文本转换
const getStatusText = (status) => {
  const statusMap = {
    'pending': '等待中',
    'running': '执行中',
    'completed': '已完成',
    'failed': '失败',
    'stopped': '已停止'
  }
  return statusMap[status] || status
}

const getStatusType = (status) => {
  const map = {
    '执行中': 'primary',
    '已完成': 'success',
    '等待中': 'info',
    '失败': 'danger',
    '已停止': 'warning'
  }
  return map[status] || 'info'
}

const getProgressStatus = (progress) => {
  if (progress === 100) return 'success'
  if (progress < 30) return 'exception'
  return null
}

// 点击统计卡片跳转
const handleStatClick = (path) => {
  if (path) {
    router.push(path)
  }
}

const viewMoreTasks = () => {
  router.push('/task/list')
}

onMounted(async () => {
  // 加载仪表盘数据
  await loadDashboardData()

  // 初始化折线图
  const lineInstance = echarts.init(lineChart.value)
  lineInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['执行次数', '成功次数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value' },
    series: [
      { name: '执行次数', type: 'line', data: [120, 132, 101, 134, 90, 230, 210] },
      { name: '成功次数', type: 'line', data: [118, 130, 98, 132, 88, 225, 205] }
    ]
  })

  // 初始化饼图
  const pieInstance = echarts.init(pieChart.value)
  pieInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '任务状态',
        type: 'pie',
        radius: '50%',
        data: [
          { value: 1048, name: '已完成' },
          { value: 735, name: '执行中' },
          { value: 580, name: '等待中' },
          { value: 484, name: '失败' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  })

  // 响应式
  window.addEventListener('resize', () => {
    lineInstance.resize()
    pieInstance.resize()
  })
})
</script>

<style scoped lang="scss">
.dashboard {
  // 欢迎栏样式
  .welcome-banner {
    background: linear-gradient(135deg, #a855f7 0%, #7c3aed 50%, #6d28d9 100%);
    border-radius: 12px;
    padding: 28px 32px;
    margin-bottom: 20px;
    box-shadow: 0 4px 15px rgba(139, 92, 246, 0.3);

    .welcome-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .welcome-left {
      .welcome-title {
        color: #fff;
        font-size: 24px;
        font-weight: 600;
        margin: 0 0 8px 0;
      }

      .welcome-date {
        color: rgba(255, 255, 255, 0.85);
        font-size: 14px;
        margin: 0;
      }
    }

    .welcome-right {
      .create-task-btn {
        background: #3b82f6;
        border-color: #3b82f6;
        padding: 12px 24px;
        font-size: 15px;
        font-weight: 500;
        border-radius: 8px;
        display: flex;
        align-items: center;
        gap: 6px;

        &:hover {
          background: #2563eb;
          border-color: #2563eb;
        }
      }
    }
  }

  .stat-card {
    .stat-content {
      display: flex;
      align-items: center;

      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        margin-right: 15px;
      }

      .stat-info {
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
        }

        .stat-title {
          font-size: 14px;
          color: #999;
          margin-top: 5px;
        }
      }
    }
  }

  // 可点击卡片样式
  .stat-card.clickable {
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }

  .chart-row {
    margin-top: 20px;
  }

  .table-row {
    margin-top: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
