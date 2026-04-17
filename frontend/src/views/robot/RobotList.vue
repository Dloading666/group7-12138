<template>
  <div class="robot-list-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>机器人列表</h2>
        <p>统一管理数据采集、报表生成与调度机器人状态。</p>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-tile stat-tile--blue">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">机器人总数</div>
      </div>
      <div class="stat-tile stat-tile--teal">
        <div class="stat-value">{{ stats.online }}</div>
        <div class="stat-label">在线</div>
      </div>
      <div class="stat-tile stat-tile--amber">
        <div class="stat-value">{{ stats.working }}</div>
        <div class="stat-label">运行中</div>
      </div>
      <div class="stat-tile stat-tile--rose">
        <div class="stat-value">{{ stats.offline }}</div>
        <div class="stat-label">离线</div>
      </div>
    </div>

    <div class="page-section padded">
      <div class="toolbar-row">
        <div class="page-actions">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增机器人
          </el-button>
        </div>
      </div>
      <div class="search-form-inline page-filter-bar compact-filter-bar">
        <div class="search-item form-field-inline">
          <span class="search-label">机器人名称</span>
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 200px" />
        </div>
        <div class="search-item form-field-inline">
          <span class="search-label">机器人编码</span>
          <el-input v-model="searchForm.code" placeholder="请输入" clearable style="width: 200px" />
        </div>
        <div class="search-item form-field-inline">
          <span class="search-label">状态</span>
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="运行中" value="running" />
          </el-select>
        </div>
        <div class="search-actions page-filter-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="page-section page-table-card">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="robotCode" label="机器人编码" width="150">
          <template #default="{ row }">
            <span class="code-text">{{ row.robotCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="机器人名称" width="180" />
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            {{ getTypeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentTaskId" label="当前任务ID" width="120" align="center">
          <template #default="{ row }">
            {{ row.currentTaskId || '空闲' }}
          </template>
        </el-table-column>
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="160">
          <template #default="{ row }">
            {{ getLastHeartbeatText(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-section page-pagination-bar">
      <div class="pagination-total">Total {{ total }}</div>
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="viewMode ? '查看机器人' : (isEdit ? '编辑机器人' : '新增机器人')"
      width="520px"
      :close-on-click-modal="false"
      class="robot-dialog"
    >
      <el-form
        ref="formRef"
        :model="robotForm"
        :rules="formRules"
        label-width="100px"
        class="robot-form"
        :disabled="viewMode"
      >
        <el-form-item label="机器人编码" prop="robotCode" required>
          <el-input v-model="robotForm.robotCode" placeholder="请输入机器人编码" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="机器人名称" prop="name" required>
          <el-input v-model="robotForm.name" placeholder="请输入机器人名称" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="robotForm.type" placeholder="例如 data_collector" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="robotForm.description" type="textarea" :rows="3" placeholder="请输入机器人描述" />
        </el-form-item>
        <el-form-item label="状态">
          <template v-if="robotForm.status === 'running'">
            <el-tag type="warning" style="margin-right: 8px">运行中</el-tag>
            <span class="muted-text">机器人运行中，不建议直接修改状态。</span>
          </template>
          <el-radio-group v-else v-model="robotForm.status">
            <el-radio value="online">在线</el-radio>
            <el-radio value="offline">离线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer v-if="!viewMode">
        <span class="dialog-footer page-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAllRobots, deleteRobot, createRobot, updateRobot } from '../../api/robot.js'
import request from '../../utils/request.js'

const stats = ref({
  total: 0,
  online: 0,
  working: 0,
  offline: 0
})

const searchForm = ref({
  name: '',
  code: '',
  status: ''
})

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)

const robotForm = ref({
  robotCode: '',
  name: '',
  type: '',
  description: '',
  status: 'offline'
})

const formRules = {
  robotCode: [{ required: true, message: '请输入机器人编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }]
}

const loadRobotList = async () => {
  loading.value = true
  try {
    const res = await getAllRobots()
    if (res.code === 200) {
      let data = (res.data || []).map((item) => ({
        ...item,
        lastHeartbeat: item?.lastHeartbeat || item?.updateTime || item?.createTime || null
      }))

      if (searchForm.value.name) {
        data = data.filter((item) => item.name && item.name.toLowerCase().includes(searchForm.value.name.toLowerCase()))
      }
      if (searchForm.value.code) {
        data = data.filter((item) => item.robotCode && item.robotCode.toLowerCase().includes(searchForm.value.code.toLowerCase()))
      }
      if (searchForm.value.status) {
        data = data.filter((item) => item.status === searchForm.value.status)
      }

      stats.value = {
        total: data.length,
        online: data.filter((item) => item.status === 'online').length,
        working: data.filter((item) => item.status === 'running').length,
        offline: data.filter((item) => item.status === 'offline').length
      }

      total.value = data.length
      const start = (pageNum.value - 1) * pageSize.value
      tableData.value = data.slice(start, start + pageSize.value)
    }
  } catch (error) {
    console.error('获取机器人列表失败:', error)
    ElMessage.error('获取机器人列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadRobotList()
}

const handleReset = () => {
  searchForm.value = {
    name: '',
    code: '',
    status: ''
  }
  pageNum.value = 1
  loadRobotList()
}

const handleAdd = () => {
  robotForm.value = {
    robotCode: '',
    name: '',
    type: '',
    description: '',
    status: 'offline'
  }
  isEdit.value = false
  viewMode.value = false
  dialogVisible.value = true
}

const handleView = (row) => {
  robotForm.value = {
    id: row.id,
    robotCode: row.robotCode,
    name: row.name,
    type: row.type || '',
    description: row.description || '',
    status: row.status || 'offline'
  }
  isEdit.value = false
  viewMode.value = true
  dialogVisible.value = true
}

const handleEdit = (row) => {
  robotForm.value = {
    id: row.id,
    robotCode: row.robotCode,
    name: row.name,
    type: row.type || '',
    description: row.description || '',
    status: row.status || 'offline'
  }
  isEdit.value = true
  viewMode.value = false
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除机器人“${row.name}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteRobot(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadRobotList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        let res
        if (isEdit.value) {
          res = await updateRobot(robotForm.value.id, robotForm.value)
          if (res.code === 200 && robotForm.value.status) {
            await request.patch(`/robots/${robotForm.value.id}/status`, { status: robotForm.value.status })
          }
        } else {
          res = await createRobot(robotForm.value)
        }

        if (res.code === 200) {
          ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
          dialogVisible.value = false
          loadRobotList()
        }
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadRobotList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  loadRobotList()
}

const getStatusType = (status) => {
  const map = {
    online: 'success',
    offline: 'info',
    running: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    online: '在线',
    offline: '离线',
    running: '运行中'
  }
  return map[status] || status
}

const getTypeText = (type) => {
  const map = {
    data_collector: '数据采集',
    report_generator: '报表生成',
    task_scheduler: '任务调度',
    notification: '消息通知',
    file_processor: '文件处理',
    data_sync: '数据同步'
  }
  return map[type] || type
}

const formatDateTime = (dateStr) => {
  if (!dateStr) {
    return '-'
  }
  return String(dateStr).replace('T', ' ').slice(0, 19)
}

const getLastHeartbeatText = (row) => {
  const heartbeatTime = row?.lastHeartbeat || row?.updateTime || row?.createTime
  return formatDateTime(heartbeatTime)
}

onMounted(() => {
  loadRobotList()
})
</script>

<style scoped lang="scss">
.robot-list-page {
  padding: 4px;
}

.toolbar-row {
  margin-bottom: 8px;
}

.compact-filter-bar {
  padding: 0;
}

.code-text {
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  color: var(--app-text-muted);
}
</style>
