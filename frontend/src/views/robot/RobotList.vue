<template>
  <div class="robot-list-page">
    <!-- 页面标题 -->
    <div class="page-title">机器人列表</div>

    <!-- 统计卡片区域 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">总机器人数</div>
      </div>
      <div class="stat-card online">
        <div class="stat-value">{{ stats.online }}</div>
        <div class="stat-label">在线</div>
      </div>
      <div class="stat-card working">
        <div class="stat-value">{{ stats.working }}</div>
        <div class="stat-label">工作中</div>
      </div>
      <div class="stat-card offline">
        <div class="stat-value">{{ stats.offline }}</div>
        <div class="stat-label">离线</div>
      </div>
    </div>

    <!-- 操作与查询区域 -->
    <div class="action-search-area">
      <!-- 左侧操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增机器人
        </el-button>
      </div>

      <!-- 右侧查询表单 -->
      <div class="search-form-inline">
        <div class="search-item">
          <span class="search-label">机器人名称</span>
          <el-input 
            v-model="searchForm.name" 
            placeholder="请输入" 
            clearable
            style="width: 200px;"
          />
        </div>
        <div class="search-item">
          <span class="search-label">机器人编码</span>
          <el-input 
            v-model="searchForm.code" 
            placeholder="请输入" 
            clearable
            style="width: 200px;"
          />
        </div>
        <div class="search-item">
          <span class="search-label">状态</span>
          <el-select 
            v-model="searchForm.status" 
            placeholder="请选择" 
            clearable
            style="width: 150px;"
          >
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="运行中" value="running" />
          </el-select>
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-area">
      <el-table 
        :data="tableData" 
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column prop="robotCode" label="机器人编码" width="150">
          <template #default="{ row }">
            <span class="code-text">{{ row.robotCode || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="name" label="机器人名称" width="180">
          <template #default="{ row }">
            <span class="name-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <span class="type-text">{{ getTypeText(row.type) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="currentTaskId" label="当前任务ID" width="120" align="center">
          <template #default="{ row }">
            <span class="task-text">{{ row.currentTaskId || '空闲' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ row.lastHeartbeat ? formatDateTime(row.lastHeartbeat) : '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="updateTime" label="更新时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.updateTime) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleView(row)">
                查看
              </el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-area">
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
    </div>

    <!-- 查看/新增/编辑机器人弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="viewMode ? '查看机器人' : (isEdit ? '编辑机器人' : '新增机器人')" 
      width="500px"
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
          <el-input 
            v-model="robotForm.robotCode" 
            placeholder="请输入机器人编码"
            :disabled="isEdit"
          />
        </el-form-item>
        
        <el-form-item label="机器人名称" prop="name" required>
          <el-input 
            v-model="robotForm.name" 
            placeholder="请输入机器人名称"
          />
        </el-form-item>
        
        <el-form-item label="类型">
          <el-input 
            v-model="robotForm.type" 
            placeholder="可选：用于区分不同用途的机器人"
          />
        </el-form-item>
        
        <el-form-item label="描述">
          <el-input 
            v-model="robotForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入机器人描述"
          />
        </el-form-item>
        
        <el-form-item label="状态">
          <template v-if="robotForm.status === 'running'">
            <el-tag type="warning" size="small" style="margin-right:8px">运行中</el-tag>
            <span style="font-size:12px;color:#909399">机器人运行中，不可修改状态</span>
          </template>
          <el-radio-group v-else v-model="robotForm.status">
            <el-radio value="online">在线</el-radio>
            <el-radio value="offline">离线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer v-if="!viewMode">
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAllRobots, deleteRobot, createRobot, updateRobot } from '../../api/robot.js'
import request from '../../utils/request.js'

const router = useRouter()

// 统计卡片数据
const stats = ref({
  total: 0,
  online: 0,
  working: 0,
  offline: 0
})

// 搜索表单
const searchForm = ref({
  name: '',
  code: '',
  status: ''
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 弹窗相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)

// 机器人表单
const robotForm = ref({
  robotCode: '',
  name: '',
  type: '',
  description: '',
  status: 'offline'
})

// 表单验证规则
const formRules = {
  robotCode: [
    { required: true, message: '请输入机器人编码', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入机器人名称', trigger: 'blur' }
  ]
}

// 加载机器人列表
const loadRobotList = async () => {
  loading.value = true
  try {
    const res = await getAllRobots()
    if (res.code === 200) {
      let data = res.data || []
      
      // 前端筛选
      if (searchForm.value.name) {
        data = data.filter(item => 
          item.name && item.name.toLowerCase().includes(searchForm.value.name.toLowerCase())
        )
      }
      if (searchForm.value.code) {
        data = data.filter(item => 
          item.robotCode && item.robotCode.toLowerCase().includes(searchForm.value.code.toLowerCase())
        )
      }
      if (searchForm.value.status) {
        data = data.filter(item => item.status === searchForm.value.status)
      }
      
      // 计算统计数据
      stats.value = {
        total: data.length,
        online: data.filter(item => item.status === 'online').length,
        working: data.filter(item => item.status === 'running').length,
        offline: data.filter(item => item.status === 'offline').length
      }
      
      // 前端分页
      total.value = data.length
      const start = (pageNum.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = data.slice(start, end)
    }
  } catch (error) {
    console.error('获取机器人列表失败:', error)
    ElMessage.error('获取机器人列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadRobotList()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    name: '',
    code: '',
    status: ''
  }
  pageNum.value = 1
  loadRobotList()
}

// 新增机器人 - 打开弹窗
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

// 查看机器人详情
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

// 编辑机器人
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

// 删除机器人
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除机器人"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
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

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        let res
        if (isEdit.value) {
          // 编辑基本信息
          res = await updateRobot(robotForm.value.id, robotForm.value)
          // 状态单独更新（避免后端 Hibernate 缓存问题）
          if (res.code === 200 && robotForm.value.status) {
            await request.patch(`/robots/${robotForm.value.id}/status`, { status: robotForm.value.status })
          }
        } else {
          // 新增
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

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  loadRobotList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  pageNum.value = val
  loadRobotList()
}

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'online': 'success',
    'offline': 'info',
    'running': 'warning'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'online': '在线',
    'offline': '离线',
    'running': '运行中'
  }
  return map[status] || status
}

// 类型文本映射
const getTypeText = (type) => {
  const map = {
    'data_collector': '数据采集',
    'report_generator': '报表生成',
    'task_scheduler': '任务调度',
    'notification': '消息通知',
    'file_processor': '文件处理',
    'data_sync': '数据同步'
  }
  return map[type] || type
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ')
}

// 页面加载
onMounted(() => {
  loadRobotList()
})
</script>

<style scoped lang="scss">
.robot-list-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  // 页面标题
  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
  }

  // 统计卡片区域
  .stats-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 15px;
    margin-bottom: 20px;

    .stat-card {
      background: #fff;
      border-radius: 8px;
      padding: 20px;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

      .stat-value {
        font-size: 28px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 8px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }

      &.online {
        .stat-value {
          color: #52c41a;
        }
      }

      &.working {
        .stat-value {
          color: #fa8c16;
        }
      }

      &.offline {
        .stat-value {
          color: #ff4d4f;
        }
      }
    }
  }

  // 操作与查询区域
  .action-search-area {
    background: #fff;
    border-radius: 8px;
    padding: 15px 20px;
    margin-bottom: 15px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .action-bar {
      margin-bottom: 15px;
    }

    .search-form-inline {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 15px;

      .search-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .search-label {
          font-size: 14px;
          color: #606266;
          white-space: nowrap;
        }
      }

      .search-actions {
        margin-left: auto;
        display: flex;
        gap: 10px;
      }
    }
  }

  // 表格区域
  .table-area {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .code-text {
      font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
      color: #606266;
    }

    .name-text {
      font-weight: 500;
      color: #303133;
    }

    .type-text {
      color: #606266;
    }

    .task-text {
      color: #606266;
    }

    .time-text {
      color: #909399;
      font-size: 13px;
    }

    .action-buttons {
      display: flex;
      gap: 8px;
      justify-content: center;
    }
  }

  // 分页区域
  .pagination-area {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;

    .pagination-total {
      color: #606266;
      font-size: 14px;
    }
  }
}

// Element Plus 表格样式覆盖
:deep(.el-table) {
  .el-table__header-wrapper {
    .el-table__header {
      th {
        background: #fafafa;
        color: #606266;
        font-weight: 600;
        font-size: 14px;
      }
    }
  }

  .el-table__body-wrapper {
    .el-table__body {
      td {
        padding: 12px 0;
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

// Element Plus 标签样式
:deep(.el-tag) {
  &.el-tag--success {
    background-color: #f4ffec;
    border-color: #e1f3d8;
    color: #67c23a;
  }

  &.el-tag--info {
    background-color: #f4f4f5;
    border-color: #e9e9eb;
    color: #909399;
  }

  &.el-tag--warning {
    background-color: #fff8e6;
    border-color: #faecd8;
    color: #e6a23c;
  }

  &.el-tag--danger {
    background-color: #ffeded;
    border-color: #fde2e2;
    color: #f56c6c;
  }
}

// Element Plus 按钮样式（仅针对非 link 类型）
:deep(.el-button--primary:not(.is-link):not(.is-text)) {
  background-color: #409eff;
  border-color: #409eff;

  &:hover {
    background-color: #66b1ff;
    border-color: #66b1ff;
  }
}

:deep(.el-button) {
  border-radius: 4px;
}

// Element Plus 输入框样式
:deep(.el-input__wrapper) {
  border-radius: 4px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 4px;
}

// Element Plus 分页样式
:deep(.el-pagination) {
  .el-pager li {
    &.is-active {
      background-color: #409eff;
      color: #fff;
    }

    &:hover {
      color: #409eff;
    }
  }
}

// 弹窗样式
:deep(.robot-dialog) {
  border-radius: 8px;
  
  .el-dialog__header {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    border-radius: 8px 8px 0 0;
    margin: 0;
    
    .el-dialog__title {
      font-weight: 600;
      color: #303133;
      font-size: 16px;
    }

    .el-dialog__headerbtn {
      top: 15px;
      right: 15px;

      .el-dialog__close {
        color: #909399;
        font-size: 16px;

        &:hover {
          color: #409eff;
        }
      }
    }
  }
  
  .el-dialog__body {
    padding: 20px;
  }
  
  .el-dialog__footer {
    padding: 15px 20px;
    border-top: 1px solid #ebeef5;
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    
    .el-button {
      border-radius: 4px;
      padding: 9px 20px;
      
      &.el-button--primary {
        background-color: #409eff;
        border-color: #409eff;
        color: #fff;
        
        &:hover {
          background-color: #66b1ff;
          border-color: #66b1ff;
        }
      }
      
      &:not(.el-button--primary) {
        background-color: #fff;
        border-color: #dcdfe6;
        color: #606266;
        
        &:hover {
          color: #409eff;
          border-color: #c6e2ff;
          background-color: #ecf5ff;
        }
      }
    }
  }
}

// 机器人表单样式
:deep(.robot-form) {
  .el-form-item {
    margin-bottom: 20px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .el-form-item__label {
    font-weight: 500;
    color: #606266;
    padding-right: 8px;
    
    &::before {
      color: #f56c6c;
      margin-right: 2px;
    }
  }
  
  .el-form-item__error {
    font-size: 12px;
    padding-top: 2px;
  }
  
  .el-input__inner {
    border-radius: 4px;
    
    &::placeholder {
      color: #c0c4cc;
    }
  }
  
  .el-textarea__inner {
    border-radius: 4px;
    
    &::placeholder {
      color: #c0c4cc;
    }
  }
  
  .el-radio-group {
    .el-radio {
      margin-right: 20px;
      
      .el-radio__inner {
        border-radius: 50%;
      }
      
      .el-radio__label {
        color: #606266;
        font-weight: normal;
      }
      
      &.is-checked {
        .el-radio__inner {
          background-color: #409eff;
          border-color: #409eff;
        }
        
        .el-radio__label {
          color: #409eff;
        }
      }
    }
  }
}

// 响应式调整
@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr) !important;
  }

  .search-form-inline {
    flex-direction: column;
    align-items: flex-start !important;

    .search-item {
      width: 100%;
    }

    .search-actions {
      margin-left: 0 !important;
      width: 100%;
      justify-content: flex-end;
    }
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr !important;
  }
}
</style>
