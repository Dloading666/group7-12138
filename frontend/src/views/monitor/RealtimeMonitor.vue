<template>
  <div class="realtime-monitor">
    <!-- 左侧主内容区 -->
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-title">数据采集</div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon blue">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.running }}</div>
            <div class="stat-label">执行中任务</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.onlineRobots }}</div>
            <div class="stat-label">在线机器人</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon purple">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgDuration }}s</div>
            <div class="stat-label">平均耗时</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.failed }}</div>
            <div class="stat-label">失败任务</div>
          </div>
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="table-wrapper">
        <el-table 
          :data="taskList" 
          v-loading="taskLoading"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column type="index" label="序号" width="70" align="center" />
          
          <el-table-column prop="taskId" label="任务ID" width="120">
            <template #default="{ row }">
              <span class="task-id">{{ row.taskId || '-' }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small" effect="light">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="taxId" label="纳税人识别号" width="180">
            <template #default="{ row }">
              <span class="tax-id">{{ row.taxId || '-' }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="companyName" label="企业名称" min-width="180">
            <template #default="{ row }">
              <span class="company-name">{{ row.companyName || '-' }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="source" label="数据来源" min-width="150">
            <template #default="{ row }">
              <span class="source-text">{{ row.source || '-' }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="collectTime" label="采集时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ row.collectTime || '-' }}</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
        
        <el-empty v-if="taskList.length === 0 && !taskLoading" description="暂无数据" />
    </div>

    <!-- 右侧面板 -->
    <div class="side-panel">
      <!-- 机器人状态 -->
      <div class="panel">
        <div class="panel-header">
          <span class="panel-title">机器人状态</span>
        </div>
        
        <div class="robot-list" v-loading="robotLoading">
          <div 
            v-for="robot in robotList" 
            :key="robot.id" 
            class="robot-item"
          >
            <div class="robot-info">
              <el-icon class="robot-avatar"><User /></el-icon>
              <div class="robot-detail">
                <div class="robot-name">{{ robot.name }}</div>
                <div class="robot-code">{{ robot.code }}</div>
              </div>
            </div>
            <el-tag :type="getStatusType(robot.status)" size="small">
              {{ getStatusText(robot.status) }}
            </el-tag>
          </div>
          <el-empty v-if="robotList.length === 0" description="暂无机器人" :image-size="60" />
        </div>
      </div>


    </div>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="任务详情" width="600px" class="detail-dialog">
      <el-descriptions :column="2" border v-if="currentTask">
        <el-descriptions-item label="任务编号">{{ currentTask.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask.name }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ currentTask.type }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentTask.status)" size="small">
            {{ getStatusText(currentTask.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行进度">{{ currentTask.progress }}%</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ formatDuration(currentTask.duration) }}</el-descriptions-item>
        <el-descriptions-item label="执行机器人">{{ currentTask.robotName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentTask.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDateTime(currentTask.startTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentTask.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="任务描述" :span="2">{{ currentTask.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentTask.errorMessage">
          <span style="color: #f56c6c">{{ currentTask.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, Cpu, Timer, Warning, Refresh, User } from '@element-plus/icons-vue'
import { getMonitorStats, getRunningTasks, getSystemResources, getTaskExecutionDetail } from '../../api/monitor.js'
import { getAllRobots } from '../../api/robot.js'

const router = useRouter()

// 统计数据
const stats = reactive({
  running: 0,
  onlineRobots: 0,
  avgDuration: 0,
  failed: 0
})

// 任务列表
const taskList = ref([])
const taskLoading = ref(false)

// 机器人列表
const robotList = ref([])
const robotLoading = ref(false)

// 系统资源
const systemResources = reactive({
  cpu: 45,
  memory: 68,
  disk: 35,
  network: 22
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentTask = ref(null)

// 定时刷新定时器
let refreshTimer = null

// 获取统计数据
const fetchStats = async () => {
  try {
    const res = await getMonitorStats()
    if (res.code === 200) {
      const data = res.data
      stats.running = data.running || 0
      stats.failed = data.failed || 0
      stats.onlineRobots = data.onlineRobots || 0
      stats.avgDuration = data.avgDuration || 0
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取任务列表
const fetchTasks = async () => {
  taskLoading.value = true
  try {
    const res = await getRunningTasks({ limit: 10 })
    if (res.code === 200) {
      taskList.value = res.data || []
    }
  } catch (error) {
    console.error('获取任务列表失败:', error)
  } finally {
    taskLoading.value = false
  }
}

// 获取机器人列表
const fetchRobots = async () => {
  robotLoading.value = true
  try {
    const res = await getAllRobots()
    if (res.code === 200) {
      robotList.value = (res.data || []).slice(0, 5)
    }
  } catch (error) {
    console.error('获取机器人列表失败:', error)
  } finally {
    robotLoading.value = false
  }
}

// 更新系统资源监控
const updateSystemResources = async () => {
  try {
    const res = await getSystemResources()
    if (res.code === 200) {
      const data = res.data
      systemResources.cpu = data.cpu || 0
      systemResources.memory = data.memory || 0
      systemResources.disk = data.disk || 0
      systemResources.network = data.network || 0
    }
  } catch (error) {
    console.error('获取系统资源失败:', error)
  }
}

// 刷新
const handleRefresh = () => {
  fetchStats()
  fetchTasks()
  ElMessage.success('刷新成功')
}

// 查看任务详情
const handleDetail = async (task) => {
  try {
    const res = await getTaskExecutionDetail(task.id)
    if (res.code === 200) {
      currentTask.value = res.data.task
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取任务详情失败:', error)
    ElMessage.error('获取任务详情失败')
  }
}

// 查看机器人详情
const handleRobotDetail = (robot) => {
  router.push({ path: '/robot/list', query: { robotId: robot.id } })
}

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'pending': 'info',
    'running': 'warning',
    'completed': 'success',
    'failed': 'danger',
    'online': 'success',
    'offline': 'info'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'pending': '等待中',
    'running': '执行中',
    'completed': '已完成',
    'failed': '失败',
    'online': '在线',
    'offline': '离线'
  }
  return map[status] || status
}

// 资源颜色
const getResourceColor = (percentage) => {
  if (percentage < 50) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

// 格式化耗时
const formatDuration = (seconds) => {
  if (!seconds || seconds === 0) return '-'
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}分${secs}秒`
}

// 格式化日期时间
const formatDateTime = (datetime) => {
  if (!datetime) return ''
  return datetime.replace('T', ' ').slice(0, 19)
}

// 初始化
onMounted(() => {
  fetchStats()
  fetchTasks()
  fetchRobots()
  
  // 定时刷新（每30秒）
  refreshTimer = setInterval(() => {
    fetchStats()
    fetchTasks()
    updateSystemResources()
  }, 30000)
})

// 清理定时器
onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped lang="scss">
.realtime-monitor {
  display: flex;
  gap: 20px;
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  .main-content {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
    }
  }

  .side-panel {
    width: 320px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

// 统计卡片
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-bottom: 20px;

  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;

      &.blue {
        background: #ecf5ff;
        color: #409eff;
      }

      &.green {
        background: #f0f9eb;
        color: #67c23a;
      }

      &.purple {
        background: #f4f0ff;
        color: #722ed1;
      }

      &.yellow {
        background: #fdf6ec;
        color: #e6a23c;
      }
    }

    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: 600;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

// 面板
.panel {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #ebeef5;

    .panel-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }
}

// 机器人列表
.robot-list {
  .robot-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f5f7fa;

    &:last-child {
      border-bottom: none;
    }

    .robot-info {
      display: flex;
      align-items: center;
      gap: 10px;

      .robot-avatar {
        font-size: 32px;
        color: #909399;
      }

      .robot-detail {
        .robot-name {
          font-size: 14px;
          color: #303133;
          font-weight: 500;
        }

        .robot-code {
          font-size: 12px;
          color: #909399;
          margin-top: 2px;
        }
      }
    }
  }
}

// 系统资源监控
.resource-monitor {
  .resource-item {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }

    .resource-label {
      display: block;
      font-size: 14px;
      color: #606266;
      margin-bottom: 8px;
    }
  }
}

// Element Plus 标签样式
:deep(.el-tag) {
  border-radius: 4px;
  
  &.el-tag--success {
    background-color: #f0f9eb;
    border-color: #e1f3d8;
    color: #67c23a;
  }

  &.el-tag--info {
    background-color: #f4f4f5;
    border-color: #e9e9eb;
    color: #909399;
  }

  &.el-tag--warning {
    background-color: #fdf6ec;
    border-color: #faecd8;
    color: #e6a23c;
  }

  &.el-tag--danger {
    background-color: #fef0f0;
    border-color: #fde2e2;
    color: #f56c6c;
  }
}

// 表格样式
:deep(.el-table) {
  border-radius: 4px;
  overflow: hidden;

  .el-table__header-wrapper {
    th {
      background: #fafafa;
      color: #606266;
      font-weight: 600;
      font-size: 14px;
      padding: 12px 0;
    }
  }

  .el-table__body-wrapper {
    .el-table__body {
      td {
        padding: 14px 0;
        font-size: 14px;
      }
    }
  }

  .el-table__row {
    &:hover > td {
      background: #f5f7fa !important;
    }
  }
}

// 弹窗样式
:deep(.detail-dialog) {
  border-radius: 8px;
  
  .el-dialog__header {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .el-dialog__title {
      font-weight: 600;
      color: #303133;
    }
  }
  
  .el-dialog__body {
    padding: 20px;
  }
  
  .el-dialog__footer {
    padding: 15px 20px;
    border-top: 1px solid #ebeef5;
  }
}
</style>
