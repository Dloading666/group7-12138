<template>
  <div class="workflow-list-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>流程列表</h2>
        <p>流程先保存为草稿，再发布成不可变版本。任务创建只面向已发布版本，不再保留写死的数据采集入口。</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="handleAiCreate">AI 生成流程</el-button>
        <el-button type="primary" @click="handleCreate">手工新建流程</el-button>
      </div>
    </div>

    <div class="page-section page-filter-bar">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="流程名称">
          <el-input v-model="searchForm.name" placeholder="请输入流程名称" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
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
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="workflowCode" label="流程编码" width="160" />
        <el-table-column prop="name" label="流程名称" min-width="220" />
        <el-table-column prop="category" label="分类" width="140">
          <template #default="{ row }">
            {{ getCategoryText(row.category) }}
          </template>
        </el-table-column>
        <el-table-column prop="stepCount" label="节点数" width="90" align="center" />
        <el-table-column prop="version" label="当前版本" width="100" align="center">
          <template #default="{ row }">
            {{ row.latestVersionId ? `v${row.version}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteWorkflow, getWorkflowList } from '../../api/workflow.js'

const router = useRouter()

const searchForm = ref({
  name: '',
  status: ''
})

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const categoryTextMap = {
  data_collection: '数据采集',
  analysis: '任务分析',
  report: '报表生成',
  sync: '数据同步',
  approval: '审批流转',
  monitor: '巡检监控',
  notification: '通知推送',
  file: '文件处理',
  transform: '清洗加工',
  other: '其他'
}

const loadWorkflowList = async () => {
  loading.value = true
  try {
    const res = await getWorkflowList({
      page: pageNum.value,
      size: pageSize.value,
      name: searchForm.value.name || undefined,
      status: searchForm.value.status || undefined
    })
    tableData.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadWorkflowList()
}

const handleReset = () => {
  searchForm.value = {
    name: '',
    status: ''
  }
  pageNum.value = 1
  loadWorkflowList()
}

const handleCreate = () => {
  router.push({ path: '/workflow/design', query: { mode: 'create' } })
}

const handleAiCreate = () => {
  router.push({ path: '/workflow/design', query: { mode: 'ai' } })
}

const handleView = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'view' } })
}

const handleEdit = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'edit' } })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除流程“${row.name}”吗？`, '删除流程', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWorkflow(row.id)
    ElMessage.success('删除成功')
    loadWorkflowList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleSizeChange = (value) => {
  pageSize.value = value
  loadWorkflowList()
}

const handleCurrentChange = (value) => {
  pageNum.value = value
  loadWorkflowList()
}

const getStatusType = (status) => {
  const map = {
    draft: 'info',
    published: 'success',
    archived: 'warning'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    draft: '草稿',
    published: '已发布',
    archived: '已归档'
  }
  return map[status] || status || '-'
}

const getCategoryText = (value) => categoryTextMap[value] || value || '-'

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
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
</style>
