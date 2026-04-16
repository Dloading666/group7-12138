<template>
  <div class="execution-logs-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>执行日志</h2>
        <p>按任务编号、日志级别和时间范围快速排查采集与执行问题，减少在长表格里来回翻找。</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="handleRefresh">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
        <el-button @click="handleExport">
          <el-icon><Download /></el-icon>
          导出当前页
        </el-button>
        <el-button type="danger" plain @click="handleClear">
          <el-icon><Delete /></el-icon>
          清空日志
        </el-button>
      </div>
    </div>

    <div class="logs-layout">
      <div class="logs-main-stack">
        <div class="stats-grid">
          <div class="stat-tile accent-blue">
            <span class="stat-label">日志总数</span>
            <strong>{{ overview.total }}</strong>
            <small>最近统计样本自动汇总</small>
          </div>
          <div class="stat-tile accent-green">
            <span class="stat-label">INFO</span>
            <strong>{{ overview.info }}</strong>
            <small>正常执行与流程提示</small>
          </div>
          <div class="stat-tile accent-amber">
            <span class="stat-label">WARN</span>
            <strong>{{ overview.warn }}</strong>
            <small>需要关注但未中断执行</small>
          </div>
          <div class="stat-tile accent-red">
            <span class="stat-label">ERROR</span>
            <strong>{{ overview.error }}</strong>
            <small>执行失败或关键异常</small>
          </div>
        </div>

        <div class="page-section padded">
          <el-form :model="searchForm" label-position="top" class="filter-form">
            <div class="filter-grid">
              <el-form-item label="任务编号">
                <el-input v-model="searchForm.taskCode" placeholder="例如 T20260415..." clearable />
              </el-form-item>
              <el-form-item label="日志级别">
                <el-select v-model="searchForm.level" placeholder="全部级别" clearable>
                  <el-option label="INFO" value="INFO" />
                  <el-option label="WARN" value="WARN" />
                  <el-option label="ERROR" value="ERROR" />
                </el-select>
              </el-form-item>
              <el-form-item label="发生时间">
                <el-date-picker
                  v-model="searchForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </div>
            <div class="page-filter-actions compact-actions">
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </div>
          </el-form>
        </div>

        <div class="page-section page-table-card">
          <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
            <el-table-column type="index" label="#" width="56" align="center" />
            <el-table-column prop="taskCode" label="任务编号" min-width="180" show-overflow-tooltip />
            <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="robotName" label="机器人" min-width="160" show-overflow-tooltip />
            <el-table-column prop="level" label="级别" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getLevelTagType(row.level)" effect="plain">{{ row.level || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="日志内容" min-width="340" show-overflow-tooltip />
            <el-table-column prop="createTime" label="记录时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleView(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="page-section page-pagination-bar">
          <div class="pagination-total">共 {{ total }} 条日志</div>
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

      <div class="logs-side-stack">
        <div class="page-section padded side-card">
          <div class="side-card-title">排查建议</div>
          <div class="side-card-text">推荐先按任务编号定位单次执行，再结合时间范围缩小问题窗口。</div>
          <ul class="hint-list">
            <li>INFO 多但没有结果时，优先检查任务详情页中的抓取回调。</li>
            <li>WARN 连续出现时，通常是选择器、等待时间或页面跳转策略需要调整。</li>
            <li>ERROR 出现后，可以先打开详情查看完整日志内容再回到配置页修正。</li>
          </ul>
        </div>

        <div class="page-section padded side-card status-card">
          <div class="side-card-title">当前筛选</div>
          <div class="status-inline-list">
            <div>
              <span>当前页条数</span>
              <strong>{{ tableData.length }}</strong>
            </div>
            <div>
              <span>筛选任务</span>
              <strong>{{ searchForm.taskCode || '全部' }}</strong>
            </div>
            <div>
              <span>筛选级别</span>
              <strong>{{ searchForm.level || '全部' }}</strong>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="日志详情" width="720px" destroy-on-close>
      <template v-if="currentRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务编号">{{ currentRow.taskCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="日志级别">
            <el-tag :type="getLevelTagType(currentRow.level)" effect="plain">{{ currentRow.level || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ currentRow.taskName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="机器人">{{ currentRow.robotName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="记录时间">{{ formatDateTime(currentRow.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="阶段">{{ currentRow.stage || '-' }}</el-descriptions-item>
          <el-descriptions-item label="日志内容" :span="2">
            <pre class="detail-pre">{{ currentRow.message || '-' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="currentRow.extraData" label="附加数据" :span="2">
            <pre class="detail-pre">{{ formatExtraData(currentRow.extraData) }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, RefreshRight } from '@element-plus/icons-vue'
import { clearAllLogs, getLogList } from '../../api/log.js'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const currentRow = ref(null)

const searchForm = reactive({
  taskCode: '',
  level: '',
  dateRange: null
})

const overview = reactive({
  total: 0,
  info: 0,
  warn: 0,
  error: 0
})

const summarizeLevels = (rows = []) => {
  const stats = { info: 0, warn: 0, error: 0 }
  rows.forEach((row) => {
    const level = String(row.level || '').toUpperCase()
    if (level === 'INFO') stats.info += 1
    if (level === 'WARN') stats.warn += 1
    if (level === 'ERROR') stats.error += 1
  })
  return stats
}

const loadOverview = async () => {
  try {
    const res = await getLogList({ page: 1, size: 500 })
    const content = res.data?.content || []
    const levelStats = summarizeLevels(content)
    overview.total = res.data?.totalElements || content.length
    overview.info = levelStats.info
    overview.warn = levelStats.warn
    overview.error = levelStats.error
  } catch (error) {
    console.error('加载日志统计失败:', error)
  }
}

const loadLogList = async () => {
  loading.value = true
  try {
    const params = {
      page: pageNum.value,
      size: pageSize.value
    }
    if (searchForm.taskCode) {
      params.taskCode = searchForm.taskCode
    }
    if (searchForm.level) {
      params.level = searchForm.level
    }
    if (searchForm.dateRange?.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }

    const res = await getLogList(params)
    tableData.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch (error) {
    console.error('加载日志列表失败:', error)
    ElMessage.error('加载日志列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  pageNum.value = 1
  await loadLogList()
}

const handleReset = async () => {
  searchForm.taskCode = ''
  searchForm.level = ''
  searchForm.dateRange = null
  pageNum.value = 1
  await loadLogList()
}

const handleSizeChange = async (value) => {
  pageSize.value = value
  await loadLogList()
}

const handleCurrentChange = async (value) => {
  pageNum.value = value
  await loadLogList()
}

const handleRefresh = async () => {
  await Promise.all([loadOverview(), loadLogList()])
  ElMessage.success('日志已刷新')
}

const handleView = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确认清空全部执行日志吗？此操作不可恢复。', '清空日志', {
      type: 'warning',
      confirmButtonText: '确认清空',
      cancelButtonText: '取消'
    })
    await clearAllLogs()
    ElMessage.success('日志已清空')
    await Promise.all([loadOverview(), loadLogList()])
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空日志失败:', error)
      ElMessage.error('清空日志失败')
    }
  }
}

const handleExport = () => {
  if (tableData.value.length === 0) {
    ElMessage.warning('当前页没有可导出的日志')
    return
  }

  const blob = new Blob([JSON.stringify(tableData.value, null, 2)], {
    type: 'application/json;charset=utf-8'
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `execution-logs-page-${pageNum.value}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前页日志')
}

const getLevelTagType = (level) => {
  const normalized = String(level || '').toUpperCase()
  if (normalized === 'ERROR') return 'danger'
  if (normalized === 'WARN') return 'warning'
  if (normalized === 'INFO') return 'success'
  return 'info'
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatExtraData = (value) => {
  if (!value) {
    return '-'
  }
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value, null, 2)
}

onMounted(async () => {
  await Promise.all([loadOverview(), loadLogList()])
})
</script>

<style scoped lang="scss">
.execution-logs-page {
  padding: 4px;
}

.logs-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
}

.logs-main-stack,
.logs-side-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.filter-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.compact-actions {
  justify-content: flex-end;
}

.side-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

.side-card-text {
  line-height: 1.7;
  color: var(--app-text-muted);
}

.hint-list {
  margin: 0;
  padding-left: 18px;
  color: var(--app-text);
  line-height: 1.8;
}

.status-inline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-inline-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-inline-list span {
  color: var(--app-text-muted);
}

.status-inline-list strong {
  color: var(--app-text);
  font-size: 15px;
}

@media (max-width: 1200px) {
  .logs-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
