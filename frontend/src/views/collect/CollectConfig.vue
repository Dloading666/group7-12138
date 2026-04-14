<template>
  <div class="collect-container">
    <!-- 左侧主内容区 -->
    <div class="main-content">
      <!-- 页面标题 -->
      <div class="page-title">数据采集</div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总采集数</div>
        </div>
        <div class="stat-card success">
          <div class="stat-value">{{ stats.success }}</div>
          <div class="stat-label">有效</div>
        </div>
        <div class="stat-card running">
          <div class="stat-value">{{ stats.running }}</div>
          <div class="stat-label">重复</div>
        </div>
        <div class="stat-card failed">
          <div class="stat-value">{{ stats.failed }}</div>
          <div class="stat-label">无效</div>
        </div>
      </div>

      <!-- 查询区域 -->
      <div class="search-area">
        <div class="search-item">
          <span class="search-label">任务ID:</span>
          <el-input 
            v-model="searchForm.taskId" 
            placeholder="请输入"
            style="width: 150px;"
            clearable
          />
        </div>
        <div class="search-item">
          <span class="search-label">关键字:</span>
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="纳税人识别号/企业名称"
            style="width: 200px;"
            clearable
          />
        </div>
        <div class="search-item">
          <span class="search-label">状态:</span>
          <el-select 
            v-model="searchForm.status" 
            placeholder="请选择"
            style="width: 120px;"
            clearable
          >
            <el-option label="有效" value="valid" />
            <el-option label="无效" value="invalid" />
            <el-option label="重复" value="duplicate" />
          </el-select>
        </div>
        <div class="search-item">
          <span class="search-label">采集时间:</span>
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
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

          <el-table-column prop="sourceUrl" label="数据来源URL" min-width="200">
            <template #default="{ row }">
              <span class="source-text" :title="row.sourceUrl">{{ row.sourceUrl || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="dataContent" label="数据内容" min-width="180">
            <template #default="{ row }">
              <span class="company-name">{{ row.dataContent ? row.dataContent.substring(0, 50) + (row.dataContent.length > 50 ? '...' : '') : '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="remark" label="备注" width="120">
            <template #default="{ row }">
              <span class="source-text">{{ row.remark || '-' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="collectTime" label="采集时间" width="170">
            <template #default="{ row }">
              <span class="time-text">{{ row.collectTime || '-' }}</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="140" fixed="right" align="center">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button type="primary" link size="small" @click="handleView(row)">
                  查看
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
        <div class="pagination-total">Total {{ total }}</div>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next, jumper"
          :total="total"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 右侧机器人状态面板 -->
    <div class="robot-panel">
      <div class="panel-title">机器人状态</div>
      <div class="robot-list">
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
          <el-tag :type="robot.status === 'online' ? 'success' : 'info'" size="small">
            {{ robot.status === 'online' ? '在线' : '离线' }}
          </el-tag>
        </div>
        <el-empty v-if="robotList.length === 0" description="暂无机器人" />
      </div>
    </div>

    <!-- 查看详情弹窗 -->
    <el-dialog 
      v-model="detailVisible" 
      title="采集详情" 
      width="600px"
      class="detail-dialog"
    >
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="数据ID">{{ currentRow.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务ID">{{ currentRow.taskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRow.status)" size="small">
            {{ getStatusText(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="机器人ID">{{ currentRow.robotId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据来源URL" :span="2">{{ currentRow.sourceUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="采集时间" :span="2">{{ currentRow.collectTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据内容" :span="2">
          <pre class="result-content">{{ currentRow.dataContent || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="formVisible" 
      :title="isEdit ? '编辑采集' : '新增采集'" 
      width="700px"
      class="form-dialog"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef"
        :model="formData" 
        :rules="rules" 
        label-width="120px"
      >
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
        
        <el-form-item label="数据来源" prop="source">
          <el-input 
            v-model="formData.source" 
            placeholder="请输入数据来源"
          />
        </el-form-item>
        
        <el-form-item label="备注" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea"
            placeholder="请输入备注信息"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formVisible = false">取消</el-button>
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
import { Plus, User } from '@element-plus/icons-vue'
import { getRobotList } from '../../api/robot.js'
import request from '../../utils/request'

// 统计卡片数据
const stats = ref({
  total: 0,
  success: 0,
  running: 0,
  failed: 0
})

// 查询表单
const searchForm = ref({
  taskId: '',
  keyword: '',
  status: '',
  dateRange: null
})

// 表格数据
const tableData = ref([])
const loading = ref(false)

// 分页
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 机器人列表
const robotList = ref([])

// 详情弹窗
const detailVisible = ref(false)
const currentRow = ref(null)

// 表单弹窗
const formVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)

// 表单数据
const formData = reactive({
  id: null,
  taxId: '',
  companyName: '',
  source: '',
  description: ''
})

// 表单验证规则
const rules = {
  taxId: [
    { required: true, message: '请输入纳税人识别号', trigger: 'blur' }
  ],
  companyName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' }
  ]
}

// 加载采集列表
const loadCollectList = async () => {
  loading.value = true
  try {
    const params = {
      page: pageNum.value,
      size: pageSize.value
    }
    
    if (searchForm.value.taskId) {
      params.taskId = searchForm.value.taskId
    }
    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword
    }
    if (searchForm.value.status) {
      params.status = searchForm.value.status
    }
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = formatDate(searchForm.value.dateRange[0])
      params.endDate = formatDate(searchForm.value.dateRange[1])
    }
    
    const res = await request.get('/collect/data', { params })
    
    if (res.code === 200) {
      tableData.value = res.data.content || []
      total.value = res.data.totalElements || 0
      
      // 计算统计数据
      stats.value = {
        total: res.data.totalElements || 0,
        success: tableData.value.filter(t => t.status === 'valid').length,
        running: tableData.value.filter(t => t.status === 'duplicate').length,
        failed: tableData.value.filter(t => t.status === 'invalid').length
      }
    } else {
      ElMessage.error(res.message || '获取采集列表失败')
    }
  } catch (error) {
    console.error('获取采集列表失败:', error)
    ElMessage.error('获取采集列表失败')
  } finally {
    loading.value = false
  }
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
  loadCollectList()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    taskId: '',
    keyword: '',
    status: '',
    dateRange: null
  }
  pageNum.value = 1
  loadCollectList()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  formVisible.value = true
}

// 查看
const handleView = (row) => {
  currentRow.value = { ...row }
  detailVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除该采集任务吗？`, 
      '提示', 
      { type: 'warning' }
    )
    
    const res = await request.delete(`/collect/data/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadCollectList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 重置表单
const resetForm = () => {
  formData.id = null
  formData.taxId = ''
  formData.companyName = ''
  formData.source = ''
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
      ? await request.put(`/collect/${formData.id}`, formData)
      : await request.post('/collect', formData)
    
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
      formVisible.value = false
      loadCollectList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    if (error !== false) {
      console.error('提交失败:', error)
    }
  } finally {
    submitLoading.value = false
  }
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  loadCollectList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  pageNum.value = val
  loadCollectList()
}

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'valid': 'success',
    'invalid': 'danger',
    'duplicate': 'warning'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'valid': '有效',
    'invalid': '无效',
    'duplicate': '重复'
  }
  return map[status] || status
}

// 页面加载
onMounted(() => {
  loadCollectList()
  loadRobotList()
})
</script>

<style scoped lang="scss">
.collect-container {
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

  .robot-panel {
    width: 280px;
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    height: fit-content;
    max-height: calc(100vh - 100px);
    overflow-y: auto;

    .panel-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #ebeef5;
    }

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
            font-size: 28px;
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
  }
}

// 统计卡片
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-bottom: 16px;

  .stat-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 20px;
    text-align: center;

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

    &.success .stat-value {
      color: #67c23a;
    }

    &.running .stat-value {
      color: #e6a23c;
    }

    &.failed .stat-value {
      color: #f56c6c;
    }
  }
}

// 查询区域
.search-area {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
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

// 表格
.table-wrapper {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 16px;
}

// 分页
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .pagination-total {
    color: #606266;
    font-size: 14px;
  }
}

// 表格样式
:deep(.el-table) {
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

.task-id {
  font-family: 'Monaco', 'Menlo', monospace;
  color: #606266;
}

.tax-id {
  font-family: 'Monaco', 'Menlo', monospace;
  color: #606266;
}

.company-name {
  color: #303133;
}

.source-text {
  color: #606266;
}

.time-text {
  color: #909399;
  font-size: 13px;
}

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

// 弹窗样式
:deep(.detail-dialog),
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
    padding: 20px;
  }
  
  .el-dialog__footer {
    padding: 15px 20px;
    border-top: 1px solid #ebeef5;
  }
}

.result-content {
  max-height: 200px;
  overflow: auto;
  margin: 0;
  font-size: 12px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
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
</style>
