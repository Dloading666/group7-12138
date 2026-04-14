<template>
  <div class="task-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务历史</span>
          <div class="header-actions">
            <!-- 筛选 -->
            <el-select v-model="filterStatus" placeholder="任务状态" clearable style="width: 120px; margin-right: 10px;" @change="loadHistory">
              <el-option label="全部" value="" />
              <el-option label="已完成" value="completed" />
              <el-option label="失败" value="failed" />
            </el-select>
            <!-- 清空历史按钮 - 仅管理员可见 -->
            <el-button type="danger" @click="handleClear" v-if="isAdmin">
              <el-icon><Delete /></el-icon>
              清空历史
            </el-button>
          </div>
        </div>
      </template>

      <!-- 加载中 -->
      <el-skeleton :loading="loading" animated :rows="5" v-if="loading" />

      <!-- 空状态 -->
      <el-empty description="暂无任务历史" v-else-if="historyList.length === 0" />

      <!-- 时间线展示 -->
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in historyList"
          :key="item.id"
          :timestamp="formatTime(item.createTime)"
          :type="getType(item.status)"
          placement="top"
        >
          <el-card>
            <div class="history-item">
              <div class="item-header">
                <span class="task-name">{{ item.name }}</span>
                <el-tag :type="getType(item.status)" size="small">
                  {{ getStatusText(item.status) }}
                </el-tag>
              </div>
              <div class="item-body">
                <p><strong>任务编号:</strong> {{ item.taskId || '-' }}</p>
                <p><strong>执行机器人:</strong> {{ item.robotName || '-' }}</p>
                <p><strong>耗时:</strong> {{ formatDuration(item.duration) }}</p>
                <p><strong>创建人:</strong> {{ item.userName || '-' }}</p>
                <p v-if="item.result"><strong>执行结果:</strong> {{ item.result }}</p>
                <p v-if="item.errorMessage" class="error-msg"><strong>错误信息:</strong> {{ item.errorMessage }}</p>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <!-- 分页 -->
      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllTasks, batchDeleteTasks } from '../../api/task.js'

// 获取用户权限
const isAdmin = inject('isAdmin')

const loading = ref(false)
const historyList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')

// 加载任务历史
const loadHistory = async () => {
  loading.value = true
  try {
    const res = await getAllTasks()
    let tasks = res.data || []
    
    // 只显示已完成或失败的任务
    tasks = tasks.filter(t => t.status === 'completed' || t.status === 'failed')
    
    // 按状态筛选
    if (filterStatus.value) {
      tasks = tasks.filter(t => t.status === filterStatus.value)
    }
    
    // 按创建时间倒序排列
    tasks.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
    
    total.value = tasks.length
    
    // 分页
    const start = (currentPage.value - 1) * pageSize.value
    historyList.value = tasks.slice(start, start + pageSize.value)
  } catch (error) {
    console.error('加载任务历史失败:', error)
    ElMessage.error('加载任务历史失败')
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 格式化耗时
const formatDuration = (seconds) => {
  if (!seconds) return '-'
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}分${secs}秒`
}

const getType = (status) => {
  const typeMap = {
    'completed': 'success',
    'failed': 'danger',
    'running': 'primary',
    'pending': 'info'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'completed': '成功',
    'failed': '失败',
    'running': '运行中',
    'pending': '等待中'
  }
  return textMap[status] || status
}

// 分页改变
const handlePageChange = (page) => {
  currentPage.value = page
  loadHistory()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadHistory()
}

// 清空历史
const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有历史记录吗？此操作不可恢复！', '警告', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    // 获取所有已完成和失败的任务ID
    const idsToDelete = historyList.value.map(t => t.id)
    if (idsToDelete.length > 0) {
      await batchDeleteTasks(idsToDelete)
    }
    
    ElMessage.success('历史记录已清空')
    loadHistory()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('清空失败')
    }
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped lang="scss">
.task-history {
  padding: 20px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      align-items: center;
    }
  }

  .history-item {
    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;

      .task-name {
        font-size: 16px;
        font-weight: bold;
      }
    }

    .item-body {
      p {
        margin: 8px 0;
        color: #666;

        &.error-msg {
          color: #f56c6c;
        }
      }
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>
