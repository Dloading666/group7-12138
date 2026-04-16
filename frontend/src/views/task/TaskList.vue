<template>
  <div class="task-list-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>任务列表</h2>
        <p>统一查看任务状态、执行进度，并从这里创建和执行数据采集任务。</p>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="openCreateDrawer">新增数据采集</el-button>
        <el-button @click="loadTaskList">刷新</el-button>
      </div>
    </div>

    <div class="search-area page-section page-filter-bar">
      <div class="search-item form-field-inline">
        <span class="label">任务关键字</span>
        <el-input v-model="searchForm.keyword" placeholder="任务名称或编号" clearable style="width: 220px" />
      </div>
      <div class="search-item form-field-inline">
        <span class="label">状态</span>
        <el-select v-model="searchForm.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="待执行" value="pending" />
          <el-option label="执行中" value="running" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
      </div>
      <div class="search-item form-field-inline">
        <span class="label">创建时间</span>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
      </div>
      <div class="search-actions page-filter-actions">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="table-wrapper page-section page-table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="taskId" label="任务编号" width="180" />
        <el-table-column prop="name" label="任务名称" min-width="220" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            {{ getTypeText(row.type) }}
          </template>
        </el-table-column>
        <el-table-column prop="robotName" label="机器人" width="150" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.status] || 'info'">{{ statusTextMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="170">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column prop="crawlUrl" label="目标 URL" min-width="240" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleView(row)">详情</el-button>
              <el-button link type="success" :disabled="row.status !== 'pending'" @click="handleExecute(row)">执行</el-button>
              <el-button link type="danger" :disabled="row.status === 'running'" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-wrapper page-section page-pagination-bar">
      <div class="total">Total {{ total }}</div>
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

    <el-drawer v-model="drawerVisible" title="新增数据采集" size="460px" destroy-on-close>
      <el-form ref="crawlFormRef" :model="crawlForm" :rules="crawlRules" label-position="top">
        <el-form-item label="任务名称">
          <el-input v-model="crawlForm.name" placeholder="可选，留空则按 URL 自动生成" />
        </el-form-item>

        <el-form-item label="执行机器人" prop="robotId">
          <el-select v-model="crawlForm.robotId" placeholder="选择在线采集机器人" style="width: 100%" :loading="robotLoading">
            <el-option
              v-for="robot in crawlRobotOptions"
              :key="robot.id"
              :label="`${robot.name} (${robot.robotCode || robot.id})`"
              :value="robot.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="目标 URL" prop="url">
          <el-input v-model="crawlForm.url" placeholder="https://example.com/article" />
        </el-form-item>

        <el-form-item label="Cookie">
          <el-input
            v-model="crawlForm.cookieText"
            type="textarea"
            :rows="5"
            placeholder="可选，直接填写原始 Cookie 字符串，例如：sid=abc; token=xyz"
          />
        </el-form-item>

        <div class="drawer-help">
          系统会自动使用默认请求头、默认超时、默认通用抽取规则，并创建为“待执行”任务，随后由你在任务列表里手动启动。
        </div>

        <div class="page-actions">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleCreateCrawlTask">创建任务</el-button>
        </div>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createCrawlTask } from '../../api/crawl.js'
import { getAllRobots } from '../../api/robot.js'
import { deleteTask, getTaskList, startTask } from '../../api/task.js'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const robotLoading = ref(false)
const submitting = ref(false)
const drawerVisible = ref(false)
const tableData = ref([])
const crawlRobotOptions = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const crawlFormRef = ref(null)

const searchForm = ref({
  keyword: '',
  status: '',
  dateRange: null
})

const crawlForm = reactive({
  name: '',
  robotId: null,
  url: '',
  cookieText: ''
})

const crawlRules = {
  robotId: [{ required: true, message: '请选择执行机器人', trigger: 'change' }],
  url: [{ required: true, message: '请输入目标 URL', trigger: 'blur' }]
}

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

const loadTaskList = async () => {
  loading.value = true
  try {
    const params = {
      page: pageNum.value,
      size: pageSize.value,
      keyword: searchForm.value.keyword || undefined,
      status: searchForm.value.status || undefined
    }
    if (searchForm.value.dateRange?.length === 2) {
      params.startDate = formatDate(searchForm.value.dateRange[0])
      params.endDate = formatDate(searchForm.value.dateRange[1])
    }
    const res = await getTaskList(params)
    tableData.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

const loadRobots = async () => {
  robotLoading.value = true
  try {
    const res = await getAllRobots()
    crawlRobotOptions.value = (res.data || []).filter((robot) => {
      return robot.type === 'data_collector' && ['online', 'running'].includes(robot.status)
    })
  } finally {
    robotLoading.value = false
  }
}

const openCreateDrawer = async () => {
  drawerVisible.value = true
  await loadRobots()
}

const handleSearch = () => {
  pageNum.value = 1
  loadTaskList()
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    status: '',
    dateRange: null
  }
  pageNum.value = 1
  loadTaskList()
}

const handleView = (row) => {
  router.push(`/task/detail/${row.id}`)
}

const handleExecute = async (row) => {
  try {
    await ElMessageBox.confirm(`确定执行任务“${row.name}”吗？`, '提示', { type: 'warning' })
    await startTask(row.id)
    ElMessage.success('任务已启动')
    loadTaskList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除任务“${row.name}”吗？`, '提示', { type: 'warning' })
    await deleteTask(row.id)
    ElMessage.success('删除成功')
    loadTaskList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleCreateCrawlTask = async () => {
  const valid = await crawlFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: crawlForm.name?.trim() || buildTaskName(crawlForm.url),
      robotId: crawlForm.robotId,
      url: crawlForm.url.trim(),
      cookies: parseCookieString(crawlForm.cookieText),
      executeType: 'manual'
    }
    await createCrawlTask(payload)
    ElMessage.success('采集任务创建成功')
    drawerVisible.value = false
    resetCrawlForm()
    await loadTaskList()
  } finally {
    submitting.value = false
  }
}

const resetCrawlForm = () => {
  crawlForm.name = ''
  crawlForm.robotId = null
  crawlForm.url = ''
  crawlForm.cookieText = ''
  crawlFormRef.value?.clearValidate()
}

const parseCookieString = (value) => {
  if (!value || !value.trim()) {
    return undefined
  }

  return value
    .split(';')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((pair) => {
      const separatorIndex = pair.indexOf('=')
      if (separatorIndex < 0) {
        return null
      }
      return {
        name: pair.slice(0, separatorIndex).trim(),
        value: pair.slice(separatorIndex + 1).trim()
      }
    })
    .filter((item) => item?.name)
}

const buildTaskName = (url) => {
  try {
    const parsed = new URL(url)
    return `数据采集 - ${parsed.hostname}`
  } catch (error) {
    return '数据采集任务'
  }
}

const handleSizeChange = (value) => {
  pageSize.value = value
  loadTaskList()
}

const handleCurrentChange = (value) => {
  pageNum.value = value
  loadTaskList()
}

const getTypeText = (type) => {
  const map = {
    'data-collection': '数据采集',
    ai_workflow: 'AI分析',
    workflow: 'AI分析',
    report: '报表生成',
    'data-sync': '数据同步',
    'web-crawl': '网站抓取'
  }
  return map[type] || type || '-'
}

const formatDate = (value) => {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  await loadTaskList()
  if (route.query.createCrawl) {
    await openCreateDrawer()
  }
})
</script>

<style scoped lang="scss">
.task-list-page {
  padding: 4px;
}

.drawer-help {
  margin-bottom: 16px;
  color: var(--app-text-muted);
  line-height: 1.7;
}
</style>
