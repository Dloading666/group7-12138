<template>
  <div class="workflow-list-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>流程列表</h2>
        <p>草稿会统一保存在草稿箱，支持继续编辑、查看和删除。发布后的流程会保留在正式列表中。</p>
      </div>
      <div class="page-header-actions">
        <el-button plain @click="openDraftBox">打开草稿箱</el-button>
        <el-button type="primary" @click="handleCreate">手工新建流程</el-button>
      </div>
    </div>

    <div class="page-section page-view-switch">
      <div class="view-copy">
        <h3>{{ currentView.label }}</h3>
        <p>{{ currentView.description }}</p>
      </div>
      <el-radio-group v-model="activeView" size="large" @change="handleViewChange">
        <el-radio-button
          v-for="item in viewOptions"
          :key="item.value"
          :label="item.value"
        >
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <div class="page-section page-filter-bar">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="流程名称">
          <el-input
            v-model="searchForm.name"
            clearable
            placeholder="请输入流程名称"
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
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
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :empty-text="emptyText"
      >
        <el-table-column prop="workflowCode" label="流程编码" width="160" />
        <el-table-column prop="name" label="流程名称" min-width="240" />
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
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button link type="primary" @click="handleEdit(row)">
                {{ getEditLabel(row) }}
              </el-button>
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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteWorkflow, getWorkflowList } from '../../api/workflow.js'

const router = useRouter()
const route = useRoute()

const workflowViews = {
  all: {
    label: '全部流程',
    status: '',
    description: '查看全部流程，适合统一浏览当前系统里的草稿、已发布和已归档流程。'
  },
  drafts: {
    label: '草稿箱',
    status: 'draft',
    description: '这里集中保存还没发布的流程草稿，适合继续编辑、调试或删除无效草稿。'
  },
  published: {
    label: '已发布',
    status: 'published',
    description: '这里只显示已经发布的正式流程，可用于任务创建和后续版本管理。'
  },
  archived: {
    label: '已归档',
    status: 'archived',
    description: '归档后的流程会保留在这里，方便回查，但不会再作为当前主流程使用。'
  }
}

const viewOptions = Object.entries(workflowViews).map(([value, item]) => ({
  value,
  label: item.label
}))

function resolveViewKey(value) {
  return workflowViews[value] ? value : 'all'
}

const activeView = ref(resolveViewKey(route.query.view))
const currentView = computed(() => workflowViews[activeView.value] || workflowViews.all)

const searchForm = ref({
  name: ''
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

const emptyText = computed(() => {
  if (activeView.value === 'drafts') {
    return '草稿箱里还没有流程草稿'
  }
  if (activeView.value === 'published') {
    return '还没有已发布的流程'
  }
  if (activeView.value === 'archived') {
    return '还没有已归档的流程'
  }
  return '暂无流程数据'
})

function syncViewQuery(viewKey) {
  const nextQuery = { ...route.query }
  if (viewKey === 'all') {
    delete nextQuery.view
  } else {
    nextQuery.view = viewKey
  }
  router.replace({
    path: route.path,
    query: nextQuery
  })
}

const loadWorkflowList = async () => {
  loading.value = true
  try {
    const res = await getWorkflowList({
      page: pageNum.value,
      size: pageSize.value,
      name: searchForm.value.name || undefined,
      status: currentView.value.status || undefined
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
    name: ''
  }
  pageNum.value = 1
  loadWorkflowList()
}

const handleViewChange = (value) => {
  const nextView = resolveViewKey(value)
  activeView.value = nextView
  pageNum.value = 1
  syncViewQuery(nextView)
  loadWorkflowList()
}

const openDraftBox = () => {
  handleViewChange('drafts')
}

const handleCreate = () => {
  router.push({ path: '/workflow/design', query: { mode: 'create' } })
}

const handleView = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'view' } })
}

const handleEdit = (row) => {
  router.push({ path: '/workflow/design', query: { id: row.id, mode: 'edit' } })
}

const getEditLabel = (row) => {
  if (row.status === 'draft') {
    return '继续编辑'
  }
  return '编辑'
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除流程“${row.name}”吗？删除后将无法恢复。`,
      '删除流程',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deleteWorkflow(row.id)
    ElMessage.success('删除成功')
    loadWorkflowList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
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

watch(
  () => route.query.view,
  (value) => {
    const nextView = resolveViewKey(value)
    if (nextView === activeView.value) {
      return
    }
    activeView.value = nextView
    pageNum.value = 1
    loadWorkflowList()
  }
)

onMounted(() => {
  loadWorkflowList()
})
</script>

<style scoped lang="scss">
.workflow-list-page {
  padding: 4px;
}

.page-view-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.view-copy h3 {
  margin: 0 0 6px;
  font-size: 18px;
  color: #1f2937;
}

.view-copy p {
  margin: 0;
  color: #667085;
  line-height: 1.6;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 960px) {
  .page-view-switch {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-view-switch :deep(.el-radio-group) {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
