<template>
  <div class="data-query app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>采集结果</h2>
        <p>集中查看真实网站采集结果，并从结果直接发起 AI 分析。</p>
      </div>
    </div>

    <div class="search-area page-section page-filter-bar">
      <div class="search-item form-field-inline">
        <span class="search-label">关键字</span>
        <el-input v-model="queryForm.keyword" placeholder="标题、摘要或 URL" clearable style="width: 220px" />
      </div>
      <div class="search-item form-field-inline">
        <span class="search-label">任务编号</span>
        <el-input v-model="queryForm.taskId" placeholder="输入任务编号" clearable style="width: 180px" />
      </div>
      <div class="search-item form-field-inline">
        <span class="search-label">状态</span>
        <el-select v-model="queryForm.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="待执行" value="pending" />
          <el-option label="执行中" value="running" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
      </div>
      <div class="search-item form-field-inline">
        <span class="search-label">创建时间</span>
        <el-date-picker
          v-model="queryForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 250px"
        />
      </div>
      <div class="search-buttons page-filter-actions">
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="table-wrapper page-section page-table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="taskId" label="任务编号" width="180" />
        <el-table-column prop="title" label="页面标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="finalUrl" label="最终 URL" min-width="240" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status] || 'info'">{{ statusTextMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="结果条数" width="100" align="center" />
        <el-table-column prop="crawledPages" label="页数" width="90" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleView(row)">查看</el-button>
              <el-button link type="success" :disabled="row.status !== 'completed' || !row.taskRecordId" @click="handleAnalyze(row)">AI分析</el-button>
              <el-button link type="danger" :disabled="!row.taskRecordId" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper page-section page-pagination-bar">
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

    <el-dialog v-model="detailVisible" title="采集结果详情" width="960px">
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务编号">{{ currentRow.taskId }}</el-descriptions-item>
          <el-descriptions-item label="页面标题">{{ currentRow.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最终 URL" :span="2">{{ currentRow.finalUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTypeMap[currentRow.status] || 'info'">{{ statusTextMap[currentRow.status] || currentRow.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="结果条数">{{ currentRow.totalCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="页数">{{ currentRow.crawledPages || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentRow.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="正文摘要" :span="2">
            <pre class="detail-pre">{{ currentRow.summaryText || '-' }}</pre>
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs style="margin-top: 16px">
          <el-tab-pane label="结构化结果">
            <el-empty v-if="!currentRow.structuredData || currentRow.structuredData.length === 0" description="当前记录没有结构化抽取结果" />
            <el-table v-else :data="currentRow.structuredData" border stripe max-height="360">
              <el-table-column
                v-for="(value, key) in currentRow.structuredData[0]"
                :key="key"
                :prop="key"
                :label="key"
                min-width="160"
                show-overflow-tooltip
              />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="原始 HTML">
            <pre class="detail-pre html-block">{{ currentRow.rawHtml || '-' }}</pre>
          </el-tab-pane>
        </el-tabs>
      </template>
      <template #footer>
        <el-button v-if="currentRow?.taskRecordId" type="primary" @click="handleAnalyze(currentRow)">发起 AI 分析</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCrawlResultDetail, getCrawlResultList } from '../../api/crawl.js'
import { deleteTask } from '../../api/task.js'

const router = useRouter()

const queryForm = ref({
  keyword: '',
  taskId: '',
  status: '',
  dateRange: null
})

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentRow = ref(null)

const statusTypeMap = reactive({
  pending: 'info',
  running: 'warning',
  completed: 'success',
  failed: 'danger'
})

const statusTextMap = reactive({
  pending: '待执行',
  running: '执行中',
  completed: '已完成',
  failed: '失败'
})

const loadDataList = async () => {
  loading.value = true
  try {
    const params = {
      page: pageNum.value,
      size: pageSize.value,
      keyword: queryForm.value.keyword || undefined,
      taskId: queryForm.value.taskId || undefined,
      status: queryForm.value.status || undefined
    }
    if (queryForm.value.dateRange?.length === 2) {
      params.startDate = queryForm.value.dateRange[0]
      params.endDate = queryForm.value.dateRange[1]
    }
    const res = await getCrawlResultList(params)
    tableData.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pageNum.value = 1
  loadDataList()
}

const handleReset = () => {
  queryForm.value = {
    keyword: '',
    taskId: '',
    status: '',
    dateRange: null
  }
  pageNum.value = 1
  loadDataList()
}

const handleView = async (row) => {
  const res = await getCrawlResultDetail(row.taskId)
  currentRow.value = res.data
  detailVisible.value = true
}

const handleAnalyze = (row) => {
  if (!row.taskRecordId) return
  detailVisible.value = false
  router.push(`/task/ai?sourceTaskRecordId=${row.taskRecordId}&sourceTaskId=${row.taskId}`)
}

const handleDelete = async (row) => {
  if (!row.taskRecordId) return
  try {
    await ElMessageBox.confirm('确定删除这条采集任务和结果吗？', '提示', { type: 'warning' })
    await deleteTask(row.taskRecordId)
    ElMessage.success('删除成功')
    loadDataList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleSizeChange = (value) => {
  pageSize.value = value
  loadDataList()
}

const handleCurrentChange = (value) => {
  pageNum.value = value
  loadDataList()
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadDataList()
})
</script>

<style scoped lang="scss">
.detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.html-block {
  max-height: 360px;
  overflow: auto;
}
</style>
