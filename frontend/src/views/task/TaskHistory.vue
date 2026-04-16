<template>
  <div class="task-history app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>任务历史</h2>
        <p>按时间回看已完成或失败的任务，支持快速进入详情页。</p>
      </div>
      <div class="page-header-actions">
        <el-select v-model="filterStatus" clearable placeholder="筛选状态" style="width: 160px" @change="loadHistory">
          <el-option label="全部" value="" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
      </div>
    </div>

    <div class="page-section padded" v-loading="loading">
      <el-empty v-if="historyList.length === 0" description="暂无任务历史" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in historyList"
          :key="item.id"
          :timestamp="formatDateTime(item.createTime)"
          :type="statusTypeMap[item.status] || 'info'"
          placement="top"
        >
          <el-card class="history-card" @click="goToDetail(item.id)">
            <div class="card-header">
              <div class="title">{{ item.name }}</div>
              <el-tag :type="statusTypeMap[item.status] || 'info'">{{ statusTextMap[item.status] || item.status }}</el-tag>
            </div>
            <div class="card-body">
              <p><strong>任务编号：</strong>{{ item.taskId || '-' }}</p>
              <p><strong>执行机器人：</strong>{{ item.robotName || '-' }}</p>
              <p><strong>耗时：</strong>{{ formatDuration(item.duration) }}</p>
              <p><strong>执行结果：</strong>{{ item.result || '-' }}</p>
              <p v-if="item.errorMessage" class="error"><strong>错误信息：</strong>{{ item.errorMessage }}</p>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </div>

    <div class="pagination-wrapper page-section page-pagination-bar">
      <div class="total">Total {{ total }}</div>
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        layout="sizes, prev, pager, next"
        :total="total"
        background
        @current-change="loadHistory"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAllTasks } from '../../api/task.js'

const router = useRouter()
const loading = ref(false)
const historyList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')

const statusTypeMap = reactive({
  completed: 'success',
  failed: 'danger',
  running: 'warning',
  pending: 'info'
})

const statusTextMap = reactive({
  completed: '已完成',
  failed: '失败',
  running: '执行中',
  pending: '待执行'
})

const loadHistory = async () => {
  loading.value = true
  try {
    const res = await getAllTasks()
    let tasks = (res.data || []).filter((task) => ['completed', 'failed'].includes(task.status))
    if (filterStatus.value) {
      tasks = tasks.filter((task) => task.status === filterStatus.value)
    }
    tasks.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    total.value = tasks.length
    const start = (currentPage.value - 1) * pageSize.value
    historyList.value = tasks.slice(start, start + pageSize.value)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadHistory()
}

const goToDetail = (id) => {
  router.push(`/task/detail/${id}`)
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatDuration = (seconds) => {
  if (!seconds && seconds !== 0) {
    return '-'
  }
  if (seconds < 60) {
    return `${seconds} 秒`
  }
  const minutes = Math.floor(seconds / 60)
  return `${minutes} 分 ${seconds % 60} 秒`
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="scss">
.task-history {
  padding: 4px;
}

.history-card {
  cursor: pointer;
  border: 1px solid rgba(219, 228, 239, 0.9);
  box-shadow: none;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.title {
  font-size: 16px;
  font-weight: 700;
}

.card-body p {
  margin: 8px 0;
  color: var(--app-text-muted);
}

.error {
  color: var(--app-danger);
}
</style>