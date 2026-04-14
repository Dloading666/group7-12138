<template>
  <div class="task-list-container">
    <!-- 顶部操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增任务
      </el-button>
    </div>

    <!-- 查询区域 -->
    <div class="search-area">
      <div class="search-item">
        <span class="search-label">任务编码/名称:</span>
        <el-input 
          v-model="searchForm.keyword" 
          placeholder="任务编码或名称"
          style="width: 200px;"
          clearable
        />
      </div>
      <div class="search-item">
        <span class="search-label">任务状态:</span>
        <el-select 
          v-model="searchForm.status" 
          placeholder="请选择"
          style="width: 150px;"
          clearable
        >
          <el-option label="等待中" value="pending" />
          <el-option label="执行中" value="running" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">开始时间:</span>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 260px;"
        />
      </div>
      <div class="search-actions">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <el-table 
        :data="tableData" 
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
      >
        <el-table-column type="index" label="序号" width="70" align="center" />
        
        <el-table-column prop="taskId" label="任务编码" width="180">
          <template #default="{ row }">
            <span class="code-link">
              {{ row.taskId || '-' }}
              <el-icon class="link-icon"><Link /></el-icon>
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="name" label="任务名称" min-width="180">
          <template #default="{ row }">
            <span class="name-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="taxId" label="纳税人识别号" width="180">
          <template #default="{ row }">
            <span class="code-link">
              {{ row.taxId || '-' }}
              <el-icon class="link-icon"><Link /></el-icon>
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="companyName" label="企业名称" min-width="180">
          <template #default="{ row }">
            <span class="company-text">{{ row.companyName || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="任务状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" effect="light">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ row.createTime || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleView(row)">
                查看详情
              </el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button 
                type="success" 
                link 
                size="small" 
                @click="handleExecute(row)"
                :disabled="row.status === 'running'"
              >
                执行
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 查看详情弹窗 -->
    <el-dialog 
      v-model="detailVisible" 
      title="任务详情" 
      width="600px"
      class="detail-dialog"
    >
      <el-descriptions :column="2" border v-if="currentTask">
        <el-descriptions-item label="任务编码">{{ currentTask.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask.name }}</el-descriptions-item>
        <el-descriptions-item label="纳税人识别号">{{ currentTask.taxId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="企业名称">{{ currentTask.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="getStatusType(currentTask.status)" size="small" effect="light">
            {{ getStatusText(currentTask.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTask.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行机器人">{{ currentTask.robotName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="绑定流程">{{ currentTask.workflowName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ currentTask.priority || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentTask.startTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ currentTask.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentTask.duration ? currentTask.duration + '秒' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentTask.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行结果" :span="2">{{ currentTask.result || '-' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentTask.errorMessage">
          <span style="color: #f56c6c;">{{ currentTask.errorMessage }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务弹窗 -->
    <el-dialog 
      v-model="formVisible" 
      :title="isEdit ? '编辑任务' : '新建任务'" 
      width="500px"
      class="form-dialog"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef"
        :model="formData" 
        :rules="rules" 
        label-width="100px"
      >
        <el-form-item label="任务名称" prop="name">
          <el-input 
            v-model="formData.name" 
            placeholder="请输入任务名称"
            maxlength="100"
          />
        </el-form-item>
        
        <el-form-item label="纳税人识别号" prop="taxId">
          <el-input 
            v-model="formData.taxId" 
            placeholder="请输入纳税人识别号"
          />
        </el-form-item>
        
        <el-form-item label="企业名称" prop="companyName">
          <el-input 
            v-model="formData.companyName" 
            placeholder="请输入企业名称"
          />
        </el-form-item>
        
        <el-form-item label="绑定流程" prop="workflowId">
          <el-select 
            v-model="formData.workflowId" 
            placeholder="请选择绑定流程"
            style="width: 100%"
          >
            <el-option 
              v-for="workflow in workflowList" 
              :key="workflow.id" 
              :label="`${workflow.code} (${workflow.name})`" 
              :value="workflow.id" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="绑定机器人" prop="robotId">
          <el-select 
            v-model="formData.robotId" 
            placeholder="请选择绑定机器人"
            style="width: 100%"
          >
            <el-option 
              v-for="robot in robotList" 
              :key="robot.id" 
              :label="`${robot.name} (${robot.code})`" 
              :value="robot.id" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="优先级" prop="priority">
          <el-input-number 
            v-model="formData.priority" 
            :min="1" 
            :max="10" 
            :step="1"
            controls-position="right"
          />
        </el-form-item>
        
        <el-form-item label="备注" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea"
            placeholder="请输入备注信息"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
          <el-button @click="handleSubmitAndExecute" :loading="executeLoading" v-if="!isEdit">
            创建并执行
          </el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import { getTaskList, createTask, updateTask, deleteTask, startTask } from '../../api/task.js'
import { getRobotList } from '../../api/robot.js'
import { getWorkflowList } from '../../api/workflow.js'

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 查询表单
const searchForm = ref({
  keyword: '',
  status: '',
  dateRange: null
})

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情弹窗
const detailVisible = ref(false)
const currentTask = ref(null)

// 表单弹窗
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)
const executeLoading = ref(false)

// 机器人列表
const robotList = ref([])

// 流程列表
const workflowList = ref([])

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  taxId: '',
  companyName: '',
  workflowId: null,
  robotId: null,
  priority: 5,
  description: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  taxId: [
    { required: true, message: '请输入纳税人识别号', trigger: 'blur' }
  ],
  companyName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' }
  ],
  workflowId: [
    { required: true, message: '请选择绑定流程', trigger: 'change' }
  ],
  robotId: [
    { required: true, message: '请选择绑定机器人', trigger: 'change' }
  ]
}

// 加载任务列表
const loadTaskList = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      page: pageNum.value,
      size: pageSize.value
    }
    
    // 添加查询条件
    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword
    }
    if (searchForm.value.status) {
      params.status = searchForm.value.status
    }
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      const [startDate, endDate] = searchForm.value.dateRange
      params.startDate = formatDate(startDate)
      params.endDate = formatDate(endDate)
    }
    
    const res = await getTaskList(params)
    
    if (res.code === 200) {
      tableData.value = res.data.content || []
      total.value = res.data.totalElements || 0
    } else {
      ElMessage.error(res.message || '获取任务列表失败')
    }
  } catch (error) {
    console.error('获取任务列表失败:', error)
    ElMessage.error('获取任务列表失败')
  } finally {
    loading.value = false
  }
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadTaskList()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: '',
    dateRange: null
  }
  pageNum.value = 1
  loadTaskList()
}

// 加载机器人列表
const loadRobotList = async () => {
  try {
    const res = await getRobotList()
    if (res.code === 200) {
      robotList.value = res.data.content || []
    }
  } catch (error) {
    console.error('获取机器人列表失败:', error)
  }
}

// 加载流程列表
const loadWorkflowList = async () => {
  try {
    const res = await getWorkflowList()
    if (res.code === 200) {
      workflowList.value = res.data.content || []
    }
  } catch (error) {
    console.error('获取流程列表失败:', error)
  }
}

// 查看详情
const handleView = (row) => {
  currentTask.value = { ...row }
  detailVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    taxId: row.taxId || '',
    companyName: row.companyName || '',
    workflowId: row.workflowId,
    robotId: row.robotId,
    priority: row.priority || 5,
    description: row.description || ''
  })
  formVisible.value = true
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  formVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务"${row.name}"吗？`, 
      '提示', 
      { type: 'warning' }
    )
    
    const res = await deleteTask(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTaskList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除任务失败:', error)
    }
  }
}

// 执行
const handleExecute = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要执行任务"${row.name}"吗？`, 
      '提示', 
      { type: 'warning' }
    )
    
    const res = await startTask(row.id)
    if (res.code === 200) {
      ElMessage.success('任务已启动')
      loadTaskList()
    } else {
      ElMessage.error(res.message || '启动失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('执行任务失败:', error)
    }
  }
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.taxId = ''
  formData.companyName = ''
  formData.workflowId = null
  formData.robotId = null
  formData.priority = 5
  formData.description = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    submitLoading.value = true
    
    const res = isEdit.value 
      ? await updateTask({ ...formData })
      : await createTask(formData)
    
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
      formVisible.value = false
      loadTaskList()
    } else {
      ElMessage.error(res.message || (isEdit.value ? '保存失败' : '创建失败'))
    }
  } catch (error) {
    if (error !== false) {
      console.error('提交失败:', error)
    }
  } finally {
    submitLoading.value = false
  }
}

// 提交并执行
const handleSubmitAndExecute = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    executeLoading.value = true
    
    // 先创建任务
    const res = await createTask(formData)
    
    if (res.code === 200) {
      ElMessage.success('创建成功，正在执行...')
      formVisible.value = false
      
      // 启动任务
      const taskId = res.data?.id
      if (taskId) {
        await startTask(taskId)
      }
      
      loadTaskList()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error) {
    if (error !== false) {
      console.error('提交失败:', error)
    }
  } finally {
    executeLoading.value = false
  }
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  loadTaskList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  pageNum.value = val
  loadTaskList()
}

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'pending': 'info',
    'running': 'warning',
    'completed': 'success',
    'failed': 'danger'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'pending': '等待中',
    'running': '执行中',
    'completed': '已完成',
    'failed': '失败'
  }
  return map[status] || status
}

// 暴露给父组件的方法
defineExpose({
  handleAdd
})

// 页面加载
onMounted(() => {
  loadTaskList()
  loadRobotList()
  loadWorkflowList()
})
</script>

<style scoped lang="scss">
.task-list-container {
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 60px);

  .action-bar {
    margin-bottom: 16px;

    .el-button {
      border-radius: 4px;
    }
  }

  .search-area {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;
    padding: 16px 20px;
    background: #fafafa;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    margin-bottom: 16px;

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

  .table-wrapper {
    border: 1px solid #ebeef5;
    border-radius: 4px;
    overflow: hidden;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }
}

// 表格样式
:deep(.el-table) {
  .el-table__header-wrapper {
    th {
      background: #f5f7fa;
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

// 编码链接样式
.code-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #409eff;
  cursor: pointer;

  .link-icon {
    font-size: 12px;
  }

  &:hover {
    text-decoration: underline;
  }
}

// 名称文本
.name-text {
  color: #303133;
  font-weight: 500;
}

// 企业名称
.company-text {
  color: #606266;
}

// 时间文本
.time-text {
  color: #909399;
  font-size: 13px;
}

// 操作按钮
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 8px;
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

// 详情弹窗
:deep(.detail-dialog) {
  border-radius: 4px;
  
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

// 表单弹窗
:deep(.form-dialog) {
  border-radius: 4px;
  
  .el-dialog__header {
    padding: 15px 20px;
    border-bottom: 1px solid #ebeef5;
    
    .el-dialog__title {
      font-weight: 600;
      color: #303133;
    }
  }
  
  .el-dialog__body {
    padding: 25px 20px;
  }
  
  .el-dialog__footer {
    padding: 15px 20px;
    border-top: 1px solid #ebeef5;
    
    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }
  }
}

// 分页样式
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

// 日期范围选择器样式
:deep(.el-date-editor) {
  .el-range-input {
    font-size: 12px;
  }
}
</style>
