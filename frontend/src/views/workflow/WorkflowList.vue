<template>
  <div class="workflow-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">流程列表</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新增流程
      </el-button>
    </div>

    <!-- 查询筛选区 -->
    <div class="search-area">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="流程名称">
          <el-input 
            v-model="searchForm.name" 
            placeholder="请输入流程名称" 
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item label="流程编码">
          <el-input 
            v-model="searchForm.code" 
            placeholder="请输入流程编码" 
            clearable
            style="width: 200px;"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select 
            v-model="searchForm.status" 
            placeholder="请选择" 
            clearable
            style="width: 150px;"
          >
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
            <el-option label="启用" value="enabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
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
        
        <el-table-column prop="workflowCode" label="流程编码" width="150">
          <template #default="{ row }">
            <span class="code-text">{{ row.workflowCode || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="name" label="流程名称" width="180">
          <template #default="{ row }">
            <span class="name-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span class="desc-text">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="stepCount" label="步骤数" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.stepCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" link size="small" @click="handleView(row)">
                查看
              </el-button>
              <el-button type="primary" link size="small" @click="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="primary" link size="small" @click="handleDesign(row)">
                设计
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

    <!-- 新增流程弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      title="新增流程" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef"
        :model="workflowForm" 
        :rules="formRules" 
        label-width="100px"
      >
        <el-form-item label="流程编码" prop="workflowCode">
          <el-input 
            v-model="workflowForm.workflowCode" 
            placeholder="如 PROCESS_001"
          />
        </el-form-item>
        
        <el-form-item label="流程名称" prop="name">
          <el-input 
            v-model="workflowForm.name" 
            placeholder="如 税务发票采集流程"
          />
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="workflowForm.description" 
            type="textarea"
            :rows="4"
            placeholder="请输入流程描述"
          />
        </el-form-item>
        
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="workflowForm.status">
            <el-radio label="enabled">启用</el-radio>
            <el-radio label="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      
      <template #footer>
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
import { getAllWorkflows, deleteWorkflow, createWorkflow } from '../../api/workflow.js'

const router = useRouter()

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
const formRef = ref(null)
const submitLoading = ref(false)

// 新增流程表单
const workflowForm = ref({
  workflowCode: '',
  name: '',
  description: '',
  status: 'enabled'
})

// 表单验证规则
const formRules = {
  workflowCode: [
    { required: true, message: '请输入流程编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入流程名称', trigger: 'blur' }
  ]
}

// 加载流程列表
const loadWorkflowList = async () => {
  loading.value = true
  try {
    const res = await getAllWorkflows()
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
          item.workflowCode && item.workflowCode.toLowerCase().includes(searchForm.value.code.toLowerCase())
        )
      }
      if (searchForm.value.status) {
        data = data.filter(item => item.status === searchForm.value.status)
      }
      
      // 计算步骤数
      data = data.map(item => ({
        ...item,
        stepCount: calculateStepCount(item.config)
      }))
      
      // 前端分页
      total.value = data.length
      const start = (pageNum.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = data.slice(start, end)
    }
  } catch (error) {
    console.error('获取流程列表失败:', error)
    ElMessage.error('获取流程列表失败')
  } finally {
    loading.value = false
  }
}

// 计算步骤数
const calculateStepCount = (config) => {
  if (!config) return 0
  try {
    const configObj = JSON.parse(config)
    return configObj.nodes ? configObj.nodes.length : 0
  } catch {
    return 0
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadWorkflowList()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    name: '',
    code: '',
    status: ''
  }
  pageNum.value = 1
  loadWorkflowList()
}

// 新增流程 - 打开弹窗
const handleCreate = () => {
  workflowForm.value = {
    workflowCode: '',
    name: '',
    description: '',
    status: 'enabled'
  }
  dialogVisible.value = true
}

// 提交新增流程
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const res = await createWorkflow({
          name: workflowForm.value.name,
          description: workflowForm.value.description,
          workflowCode: workflowForm.value.workflowCode
        })
        
        if (res.code === 200) {
          ElMessage.success('新增成功')
          dialogVisible.value = false
          loadWorkflowList()
        }
      } catch (error) {
        console.error('新增流程失败:', error)
        ElMessage.error('新增流程失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 查看流程
const handleView = (row) => {
  router.push({ 
    path: '/workflow/design', 
    query: { id: row.id, mode: 'view' } 
  })
}

// 编辑流程
const handleEdit = (row) => {
  router.push({ 
    path: '/workflow/design', 
    query: { id: row.id, mode: 'edit' } 
  })
}

// 设计流程 - 跳转到设计页面
const handleDesign = (row) => {
  router.push({ 
    path: '/workflow/design', 
    query: { id: row.id, mode: 'design' } 
  })
}

// 删除流程
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除流程"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await deleteWorkflow(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadWorkflowList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 分页大小改变
const handleSizeChange = (val) => {
  pageSize.value = val
  loadWorkflowList()
}

// 当前页改变
const handleCurrentChange = (val) => {
  pageNum.value = val
  loadWorkflowList()
}

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'draft': 'info',
    'published': 'success',
    'archived': 'warning',
    'enabled': 'success',
    'disabled': 'danger'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'draft': '草稿',
    'published': '已发布',
    'archived': '已归档',
    'enabled': '启用',
    'disabled': '禁用'
  }
  return map[status] || status
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ')
}

// 页面加载
onMounted(() => {
  loadWorkflowList()
})
</script>

<style scoped lang="scss">
.workflow-list-page {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 120px);

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 15px 20px;
    background: #fff;
    border-radius: 4px;

    .page-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }

  .search-area {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    margin-bottom: 20px;

    .search-form {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;

      .el-form-item {
        margin-bottom: 0;
        margin-right: 10px;

        &:last-child {
          margin-right: 0;
        }
      }
    }
  }

  .table-area {
    background: #fff;
    padding: 20px;
    border-radius: 4px;

    .code-text {
      font-family: 'Courier New', monospace;
      color: #666;
    }

    .name-text {
      font-weight: 500;
      color: #333;
    }

    .desc-text {
      color: #666;
      font-size: 13px;
    }

    .time-text {
      color: #666;
      font-size: 13px;
      font-family: 'Courier New', monospace;
    }

    .action-buttons {
      display: flex;
      gap: 8px;
      justify-content: center;
    }
  }

  .pagination-area {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;

    .pagination-total {
      color: #666;
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
        color: #333;
        font-weight: 600;
      }
    }
  }

  .el-table__body-wrapper {
    .el-table__body {
      td {
        padding: 12px 0;
      }
    }
  }
}

// 弹窗样式
:deep(.el-dialog) {
  .el-dialog__header {
    padding: 15px 20px;
    border-bottom: 1px solid #f0f0f0;
    
    .el-dialog__title {
      font-weight: 600;
      color: #333;
    }
  }
  
  .el-dialog__body {
    padding: 20px;
  }
  
  .el-dialog__footer {
    padding: 15px 20px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
