<template>
  <div class="data-query">
    <!-- 页面标题 -->
    <div class="page-title">数据查询</div>

    <!-- 查询区域 -->
    <div class="search-area">
      <div class="search-item">
        <span class="search-label">关键字:</span>
        <el-input 
          v-model="queryForm.keyword" 
          placeholder="纳税人识别号/企业名称"
          style="width: 200px;"
          clearable
        />
      </div>
      <div class="search-item">
        <span class="search-label">任务ID:</span>
        <el-input 
          v-model="queryForm.taskId" 
          placeholder="请输入"
          style="width: 150px;"
          clearable
        />
      </div>
      <div class="search-item">
        <span class="search-label">税区ID:</span>
        <el-input 
          v-model="queryForm.taxAreaId" 
          placeholder="请输入"
          style="width: 150px;"
          clearable
        />
      </div>
      <div class="search-item">
        <span class="search-label">数据状态:</span>
        <el-select 
          v-model="queryForm.status" 
          placeholder="请选择"
          style="width: 150px;"
          clearable
        >
          <el-option label="可用" value="available" />
          <el-option label="等待中" value="pending" />
          <el-option label="执行中" value="running" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
      </div>
      <div class="search-item">
        <span class="search-label">创建时间:</span>
        <el-date-picker
          v-model="queryForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD"
          style="width: 240px;"
        />
      </div>
      <div class="search-buttons">
        <el-button type="primary" @click="handleQuery">查询</el-button>
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
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        
        <el-table-column type="index" label="序号" width="70" align="center" />
        
        <el-table-column prop="taskId" label="任务ID" width="120">
          <template #default="{ row }">
            <span class="task-id">{{ row.taskId || '-' }}</span>
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
        
        <el-table-column prop="taxAreaId" label="税区ID" width="120">
          <template #default="{ row }">
            <span class="tax-area-id">{{ row.taxAreaId || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="数据状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success" size="small" effect="light">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog 
      v-model="detailVisible" 
      title="数据详情" 
      width="700px"
      class="detail-dialog"
    >
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="任务ID">{{ currentRow.taskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="纳税人识别号">{{ currentRow.taxId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="企业名称">{{ currentRow.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="税区ID">{{ currentRow.taxAreaId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据状态">
          <el-tag type="success" size="small">
            {{ getStatusText(currentRow.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="数据内容" :span="2">
          <pre class="data-content">{{ currentRow.dataContent || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

// 查询表单
const queryForm = ref({
  keyword: '',
  taskId: '',
  taxAreaId: '',
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

// 多选数据
const selectedRows = ref([])

// 详情弹窗
const detailVisible = ref(false)
const currentRow = ref(null)

// 加载数据列表
const loadDataList = async () => {
  loading.value = true
  try {
    const params = {
      page: pageNum.value,
      size: pageSize.value
    }
    
    if (queryForm.value.keyword) {
      params.keyword = queryForm.value.keyword
    }
    if (queryForm.value.taskId) {
      params.taskId = queryForm.value.taskId
    }
    if (queryForm.value.taxAreaId) {
      params.taxAreaId = queryForm.value.taxAreaId
    }
    if (queryForm.value.status) {
      params.status = queryForm.value.status
    }
    if (queryForm.value.dateRange && queryForm.value.dateRange.length === 2) {
      params.startDate = queryForm.value.dateRange[0]
      params.endDate = queryForm.value.dateRange[1]
    }
    
    const res = await request.get('/data/query', { params })
    
    if (res.code === 200) {
      tableData.value = res.data.content || []
      total.value = res.data.totalElements || 0
    } else {
      ElMessage.error(res.message || '获取数据列表失败')
    }
  } catch (error) {
    console.error('获取数据列表失败:', error)
    ElMessage.error('获取数据列表失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  pageNum.value = 1
  loadDataList()
}

// 重置
const handleReset = () => {
  queryForm.value = {
    keyword: '',
    taskId: '',
    taxAreaId: '',
    status: '',
    dateRange: null
  }
  pageNum.value = 1
  loadDataList()
}

// 多选变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
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
      `确定要删除该数据吗？`, 
      '提示', 
      { type: 'warning' }
    )
    
    const res = await request.delete(`/data/${row.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadDataList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 状态文本
const getStatusText = (status) => {
  const map = { 
    'available': '可用',
    'pending': '等待中',
    'running': '执行中',
    'completed': '已完成',
    'failed': '失败'
  }
  return map[status] || status || '可用'
}

// 格式化日期时间
const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  return datetime.replace('T', ' ').slice(0, 19)
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  loadDataList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  pageNum.value = val
  loadDataList()
}

// 页面加载
onMounted(() => {
  loadDataList()
})
</script>

<style scoped lang="scss">
.data-query {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  .page-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
    background: #fff;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  // 查询区域样式
  .search-area {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: center;
    padding: 16px 20px;
    background: #fff;
    border-radius: 8px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    border: 1px solid #ebeef5;

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

    .search-buttons {
      margin-left: auto;
      display: flex;
      gap: 10px;
    }
  }

  // 表格样式
  .table-wrapper {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    margin-bottom: 16px;

    .task-id {
      color: #409eff;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }

    .tax-id {
      color: #409eff;
    }

    .company-name {
      color: #303133;
    }

    .tax-area-id {
      color: #909399;
    }

    .time-text {
      color: #606266;
    }
  }

  // 分页样式
  .pagination-wrapper {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fff;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .pagination-total {
      color: #606266;
      font-size: 14px;
    }
  }
}

// 详情弹窗样式
.detail-dialog {
  .data-content {
    background: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 13px;
    max-height: 300px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-break: break-all;
    margin: 0;
  }
}
</style>
