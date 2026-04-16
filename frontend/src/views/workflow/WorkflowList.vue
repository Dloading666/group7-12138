<template>
  <div class="workflow-list-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>流程列表</h2>
        <p>统一查看流程定义、状态与入口，减少表单和表格布局割裂。</p>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新增流程
        </el-button>
      </div>
    </div>

    <div class="search-area page-section page-filter-bar">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="流程名称">
          <el-input v-model="searchForm.name" placeholder="请输入流程名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="流程编码">
          <el-input v-model="searchForm.code" placeholder="请输入流程编码" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
            <el-option label="启用" value="enabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="page-actions">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-section page-table-card">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="workflowCode" label="流程编码" width="150">
          <template #default="{ row }">
            <span class="code-text">{{ row.workflowCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="流程名称" width="180" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="stepCount" label="步骤数" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="info">{{ row.stepCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="primary" @click="handleDesign(row)">设计</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增流程" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="workflowForm" :rules="formRules" label-width="100px">
        <el-form-item label="流程编码" prop="workflowCode">
          <el-input v-model="workflowForm.workflowCode" placeholder="例如 PROCESS_001" />
        </el-form-item>
        <el-form-item label="流程名称" prop="name">
          <el-input v-model="workflowForm.name" placeholder="例如 税务发票采集流程" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="workflowForm.description" type="textarea" :rows="4" placeholder="请输入流程描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="workflowForm.status">
            <el-radio label="enabled">启用</el-radio>
            <el-radio label="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAllWorkflows, deleteWorkflow, createWorkflow } from '../../api/workflow.js'

const router = useRouter()

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
const formRef = ref(null)
const submitLoading = ref(false)

const workflowForm = ref({
  workflowCode: '',
  name: '',
  description: '',
  status: 'enabled'
})

const formRules = {
  workflowCode: [
    { required: true, message: '请输入流程编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }]
}

const loadWorkflowList = async () => {
  loading.value = true
  try {
    const res = await getAllWorkflows()
    if (res.code === 200) {
      let data = res.data || []

      if (searchForm.value.name) {
        data = data.filter((item) => item.name && item.name.toLowerCase().includes(searchForm.value.name.toLowerCase()))
      }
      if (searchForm.value.code) {
        data = data.filter((item) => item.workflowCode && item.workflowCode.toLowerCase().includes(searchForm.value.code.toLowerCase()))
      }
      if (searchForm.value.status) {
        data = data.filter((item) => item.status === searchForm.value.status)
      }

      data = data.map((item) => ({
        ...item,
        stepCount: calculateStepCount(item.config)
      }))

      total.value = data.length
      const start = (pageNum.value - 1) * pageSize.value
      tableData.value = data.slice(start, start + pageSize.value)
    }
  } catch (error) {
    console.error('获取流程列表失败:', error)
    ElMessage.error('获取流程列表失败')
  } finally {
    loading.value = false
  }
}

const calculateStepCount = (config) => {
  if (!config) {
    return 0
  }
  try {
    const configObj = JSON.parse(config)
    return configObj.nodes ? configObj.nodes.length : 0
  } catch {
    return 0
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadWorkflowList()
}

const handleReset = () => {
  searchForm.value = {
    name: '',
    code: '',
    status: ''
  }
  pageNum.value = 1
  loadWorkflowList()
}

const handleCreate = () => {
  workflowForm.value = {
    workflowCode: '',
    name: '',
    description: '',
    status: 'enabled'
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) {
    return
  }

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

const handleView = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'view' } })
}

const handleEdit = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'edit' } })
}

const handleDesign = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'design' } })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除流程“${row.name}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

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

const handleSizeChange = (val) => {
  pageSize.value = val
  loadWorkflowList()
}

const handleCurrentChange = (val) => {
  pageNum.value = val
  loadWorkflowList()
}

const getStatusType = (status) => {
  const map = {
    draft: 'info',
    published: 'success',
    archived: 'warning',
    enabled: 'success',
    disabled: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    draft: '草稿',
    published: '已发布',
    archived: '已归档',
    enabled: '启用',
    disabled: '禁用'
  }
  return map[status] || status
}

const formatDateTime = (dateStr) => {
  if (!dateStr) {
    return '-'
  }
  return String(dateStr).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadWorkflowList()
})
</script>

<style scoped lang="scss">
.workflow-list-page {
  padding: 4px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.code-text {
  font-family: 'JetBrains Mono', 'Consolas', monospace;
  color: var(--app-text-muted);
}
</style>