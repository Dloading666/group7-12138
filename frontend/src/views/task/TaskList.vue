<template>
  <div class="task-list-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>任务列表</h2>
        <p>任务统一从已发布流程版本创建，可选择网站采集、重点告警、双站点汇总等已发布流程模板。</p>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="openCreateDialog">新增任务</el-button>
        <el-button @click="loadTaskList">刷新</el-button>
      </div>
    </div>

    <div class="page-section page-filter-bar search-area">
      <div class="search-item form-field-inline">
        <span class="label">关键词</span>
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
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
      </div>
      <div class="page-filter-actions">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="page-section page-table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="taskId" label="任务编号" width="180" />
        <el-table-column prop="name" label="任务名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="流程" min-width="220">
          <template #default="{ row }">
            <div class="workflow-cell">
              <strong>{{ row.workflowName || '-' }}</strong>
              <span>{{ getCategoryText(row.workflowCategory) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最新状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.latestRunStatus || row.status] || 'info'">
              {{ statusTextMap[row.latestRunStatus || row.status] || row.latestRunStatus || row.status || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="runCount" label="运行次数" width="110" align="center" />
        <el-table-column label="调度方式" min-width="180">
          <template #default="{ row }">
            {{ getScheduleSummary(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="handleView(row)">详情</el-button>
              <el-button
                link
                type="success"
                :disabled="(row.latestRunStatus || row.status) === 'running'"
                @click="handleExecute(row)"
              >
                立即执行
              </el-button>
              <el-button
                link
                type="danger"
                :disabled="(row.latestRunStatus || row.status) === 'running'"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
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
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="新增任务" width="860px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <div class="create-grid">
          <el-form-item label="任务名称">
            <el-input v-model="createForm.name" placeholder="可选，留空时默认使用流程名称" />
          </el-form-item>

          <el-form-item label="流程版本" prop="workflowVersionId">
            <el-select
              v-model="createForm.workflowVersionId"
              placeholder="选择已发布流程版本"
              filterable
              style="width: 100%"
              :loading="workflowLoading"
              @change="handleVersionChange"
            >
              <el-option
                v-for="item in workflowVersions"
                :key="item.id"
                :label="`${item.name} · v${item.versionNumber}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="schedule-card">
          <div class="schedule-head">
            <h3>执行方式</h3>
            <p>当前 v1 支持先创建后手动执行，也支持每天固定时间和每周固定时间自动触发。</p>
          </div>
          <el-radio-group v-model="createForm.scheduleMode" class="schedule-mode-group">
            <el-radio-button value="immediate">手动执行</el-radio-button>
            <el-radio-button value="daily">每天固定时间</el-radio-button>
            <el-radio-button value="weekly">每周固定时间</el-radio-button>
          </el-radio-group>

          <div v-if="createForm.scheduleMode !== 'immediate'" class="schedule-grid">
            <el-form-item label="执行时间" required>
              <el-time-picker
                v-model="createForm.scheduleTime"
                value-format="HH:mm"
                format="HH:mm"
                :clearable="false"
                style="width: 100%"
                placeholder="选择时间"
              />
            </el-form-item>

            <el-form-item v-if="createForm.scheduleMode === 'weekly'" label="执行周几" required>
              <el-checkbox-group v-model="createForm.scheduleWeekdays" class="weekday-group">
                <el-checkbox-button
                  v-for="day in weekdayOptions"
                  :key="day.value"
                  :label="day.value"
                >
                  {{ day.label }}
                </el-checkbox-button>
              </el-checkbox-group>
            </el-form-item>
          </div>
        </div>

        <div v-if="selectedWorkflowVersion" class="workflow-summary">
          <div class="summary-head">
            <div>
              <div class="summary-title">{{ selectedWorkflowVersion.name }}</div>
              <div class="summary-meta">
                <span>{{ getCategoryText(selectedWorkflowVersion.category) }}</span>
                <span>v{{ selectedWorkflowVersion.versionNumber }}</span>
              </div>
            </div>
            <el-tag type="success" effect="plain">已发布</el-tag>
          </div>
          <p>{{ selectedWorkflowDescription }}</p>
        </div>

        <div class="dynamic-form">
          <div class="dynamic-form-head">
            <h3>流程输入参数</h3>
            <p>表单项会根据流程版本的 inputSchema 自动生成。常用模板通常会要求目标网址、收件邮箱等参数。</p>
          </div>

          <el-empty v-if="schemaFields.length === 0" description="当前流程没有定义输入参数，创建后可直接执行。" />

          <div v-else class="dynamic-grid">
            <el-form-item
              v-for="field in schemaFields"
              :key="field.name"
              :label="field.label"
              :required="field.required"
            >
              <el-select
                v-if="field.schema.enum"
                v-model="createForm.input[field.name]"
                placeholder="请选择"
                clearable
              >
                <el-option v-for="option in field.schema.enum" :key="String(option)" :label="String(option)" :value="option" />
              </el-select>

              <el-switch
                v-else-if="field.schema.type === 'boolean'"
                v-model="createForm.input[field.name]"
              />

              <el-input-number
                v-else-if="field.schema.type === 'integer' || field.schema.type === 'number'"
                v-model="createForm.input[field.name]"
                :precision="field.schema.type === 'integer' ? 0 : 2"
                style="width: 100%"
              />

              <el-input
                v-else-if="field.schema.type === 'array' || field.schema.type === 'object'"
                v-model="createForm.input[field.name]"
                type="textarea"
                :rows="4"
                :placeholder="field.schema.type === 'array' ? '输入 JSON 数组，或每行一个值' : '输入 JSON 对象'"
              />

              <el-input
                v-else
                v-model="createForm.input[field.name]"
                :type="field.schema.format === 'textarea' ? 'textarea' : 'text'"
                :rows="field.schema.format === 'textarea' ? 4 : undefined"
                :placeholder="field.schema.description || `请输入 ${field.label}`"
              />

              <div v-if="field.schema.description" class="field-description">{{ field.schema.description }}</div>
            </el-form-item>
          </div>
        </div>

        <div class="page-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreateTask">创建任务</el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTask, deleteTask, getTaskList, startTask } from '../../api/task.js'
import { getPublishedWorkflowVersions } from '../../api/workflow.js'
import { findOfficialDemoTemplateByWorkflowCode } from '../workflow/officialDemoTemplate.js'

const APP_TIMEZONE = 'Asia/Shanghai'
const weekdayOptions = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' }
]

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const creating = ref(false)
const workflowLoading = ref(false)
const dialogVisible = ref(false)
const tableData = ref([])
const workflowVersions = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const createFormRef = ref(null)

const searchForm = ref({
  keyword: '',
  status: '',
  dateRange: null
})

const createForm = reactive({
  name: '',
  workflowVersionId: null,
  scheduleMode: 'immediate',
  scheduleTime: '09:00',
  scheduleWeekdays: ['MON'],
  input: {}
})

const createRules = {
  workflowVersionId: [{ required: true, message: '请选择已发布流程版本', trigger: 'change' }]
}

const statusTypeMap = {
  pending: 'info',
  running: 'warning',
  completed: 'success',
  failed: 'danger'
}

const statusTextMap = {
  pending: '待执行',
  running: '执行中',
  completed: '已完成',
  failed: '失败'
}

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

const selectedWorkflowVersion = computed(() => {
  return workflowVersions.value.find((item) => item.id === createForm.workflowVersionId) || null
})

function normalizeTemplateFieldSchema(name, schema = {}) {
  if (name === 'target_url') {
    return {
      ...schema,
      title: '目标网址',
      description: '输入要抓取的网站地址',
      format: schema.format || 'url'
    }
  }

  if (name === 'to_email') {
    return {
      ...schema,
      title: '收件邮箱',
      description: 'AI 整理后的详细内容会发送到这个邮箱地址',
      format: schema.format || 'email'
    }
  }

  return schema
}

const parsedInputSchema = computed(() => {
  const raw = selectedWorkflowVersion.value?.inputSchema
  if (!raw) {
    return { type: 'object', properties: {}, required: [] }
  }
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    const properties = Object.fromEntries(
      Object.entries(parsed?.properties || {}).map(([name, schema]) => [name, normalizeTemplateFieldSchema(name, schema)])
    )
    return {
      type: 'object',
      ...parsed,
      properties
    }
  } catch (error) {
    return { type: 'object', properties: {}, required: [] }
  }
})

const selectedWorkflowDescription = computed(() => {
  const version = selectedWorkflowVersion.value
  if (!version) return ''

  const template = findOfficialDemoTemplateByWorkflowCode(version.workflowCode)
  const description = template?.description || version.description || '暂无流程说明'
  return String(description)
    .replaceAll('QQ 邮箱', '邮箱地址')
    .replaceAll('QQ邮箱', '邮箱地址')
})

const schemaFields = computed(() => {
  const properties = parsedInputSchema.value?.properties || {}
  const required = parsedInputSchema.value?.required || []
  return Object.entries(properties).map(([name, schema]) => ({
    name,
    label: schema.title || name,
    schema,
    required: required.includes(name)
  }))
})

function parseScheduleConfig(raw) {
  if (!raw) return {}
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (error) {
    return {}
  }
}

function formatWeekdays(days = []) {
  const labels = days
    .map((day) => weekdayOptions.find((item) => item.value === day)?.label || day)
    .filter(Boolean)
  return labels.join('、')
}

function buildCronExpression(mode, time, weekdays = []) {
  const [hourText = '09', minuteText = '00'] = String(time || '09:00').split(':')
  const hour = Number(hourText)
  const minute = Number(minuteText)
  if (Number.isNaN(hour) || Number.isNaN(minute)) {
    throw new Error('执行时间格式无效，请使用 HH:mm')
  }

  if (mode === 'daily') {
    return `0 ${minute} ${hour} * * ?`
  }
  if (mode === 'weekly') {
    const selectedDays = Array.isArray(weekdays) ? weekdays.filter(Boolean) : []
    if (!selectedDays.length) {
      throw new Error('每周固定时间至少选择一个周几')
    }
    return `0 ${minute} ${hour} ? * ${selectedDays.join(',')}`
  }
  return ''
}

function buildSchedulePayload() {
  if (createForm.scheduleMode === 'immediate') {
    return {
      executeType: 'immediate',
      scheduleConfig: JSON.stringify({
        mode: 'immediate',
        presetType: 'immediate',
        timezone: APP_TIMEZONE
      })
    }
  }

  if (!createForm.scheduleTime) {
    throw new Error('请选择执行时间')
  }

  const presetType = createForm.scheduleMode === 'daily' ? 'daily' : 'weekly'
  const cronExpression = buildCronExpression(createForm.scheduleMode, createForm.scheduleTime, createForm.scheduleWeekdays)
  return {
    executeType: 'cron',
    scheduleConfig: JSON.stringify({
      mode: 'cron',
      presetType,
      time: createForm.scheduleTime,
      days: presetType === 'weekly' ? createForm.scheduleWeekdays : [],
      timezone: APP_TIMEZONE,
      cronExpression
    })
  }
}

function getScheduleSummary(row) {
  const schedule = parseScheduleConfig(row.scheduleConfig)
  const presetType = schedule.presetType
  if ((schedule.mode || row.executeType) === 'immediate') {
    return '手动执行'
  }
  if (presetType === 'daily') {
    return `每天 ${schedule.time || '--:--'}`
  }
  if (presetType === 'weekly') {
    return `每周 ${formatWeekdays(schedule.days || [])} ${schedule.time || '--:--'}`
  }
  if (row.executeType === 'cron') {
    return 'Cron 调度'
  }
  return '手动执行'
}

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
      params.startDate = searchForm.value.dateRange[0]
      params.endDate = searchForm.value.dateRange[1]
    }
    const res = await getTaskList(params)
    tableData.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

const loadWorkflowVersions = async () => {
  workflowLoading.value = true
  try {
    const res = await getPublishedWorkflowVersions()
    workflowVersions.value = res.data || []
  } finally {
    workflowLoading.value = false
  }
}

const resetCreateForm = () => {
  createForm.name = ''
  createForm.workflowVersionId = null
  createForm.scheduleMode = 'immediate'
  createForm.scheduleTime = '09:00'
  createForm.scheduleWeekdays = ['MON']
  createForm.input = {}
  createFormRef.value?.clearValidate()
}

const openCreateDialog = async () => {
  resetCreateForm()
  dialogVisible.value = true
  await loadWorkflowVersions()
}

const defaultValueForSchema = (schema = {}) => {
  if (schema.default !== undefined) return schema.default
  if (schema.enum?.length) return schema.enum[0]
  switch (schema.type) {
    case 'boolean':
      return false
    case 'integer':
    case 'number':
      return null
    case 'array':
    case 'object':
      return ''
    default:
      return ''
  }
}

const buildDefaultInput = (schema) => {
  const properties = schema?.properties || {}
  return Object.fromEntries(
    Object.entries(properties).map(([name, fieldSchema]) => [name, defaultValueForSchema(fieldSchema)])
  )
}

const handleVersionChange = () => {
  const version = selectedWorkflowVersion.value
  if (!version) {
    createForm.input = {}
    return
  }
  createForm.input = buildDefaultInput(parsedInputSchema.value)
  if (!createForm.name) {
    createForm.name = version.name
  }
}

const normalizeJsonInput = (schema, value) => {
  if (value === '' || value === null || value === undefined) return value

  if (schema.type === 'array') {
    if (Array.isArray(value)) return value
    if (typeof value !== 'string') return [value]
    const trimmed = value.trim()
    if (!trimmed) return []
    try {
      return JSON.parse(trimmed)
    } catch (error) {
      return trimmed.split('\n').map((item) => item.trim()).filter(Boolean)
    }
  }

  if (schema.type === 'object') {
    if (typeof value === 'object' && !Array.isArray(value)) return value
    if (typeof value === 'string' && value.trim()) return JSON.parse(value)
  }

  if (schema.type === 'integer' || schema.type === 'number') {
    return value === '' ? null : Number(value)
  }

  if (schema.type === 'boolean') {
    return Boolean(value)
  }

  return value
}

const buildInputPayload = () => {
  const payload = {}
  for (const field of schemaFields.value) {
    const rawValue = createForm.input[field.name]
    const hasValue = rawValue !== '' && rawValue !== null && rawValue !== undefined
    if (!hasValue) {
      if (field.required) {
        throw new Error(`请输入 ${field.label}`)
      }
      continue
    }

    try {
      payload[field.name] = normalizeJsonInput(field.schema, rawValue)
    } catch (error) {
      throw new Error(`${field.label} 格式不正确，请检查 JSON 内容`)
    }
  }
  return payload
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
    await ElMessageBox.confirm(`确认立即执行任务“${row.name}”吗？`, '立即执行', {
      type: 'warning',
      confirmButtonText: '执行',
      cancelButtonText: '取消'
    })
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
    await ElMessageBox.confirm(`确认删除任务“${row.name}”吗？`, '删除任务', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteTask(row.id)
    ElMessage.success('删除成功')
    loadTaskList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleCreateTask = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedWorkflowVersion.value) return

  creating.value = true
  try {
    const schedule = buildSchedulePayload()
    const payload = {
      name: createForm.name?.trim() || selectedWorkflowVersion.value.name,
      workflowVersionId: createForm.workflowVersionId,
      workflowName: selectedWorkflowVersion.value.name,
      workflowCategory: selectedWorkflowVersion.value.category,
      executeType: schedule.executeType,
      inputConfig: JSON.stringify(buildInputPayload()),
      scheduleConfig: schedule.scheduleConfig
    }

    const res = await createTask(payload)
    dialogVisible.value = false
    ElMessage.success(
      schedule.executeType === 'immediate'
        ? '任务创建成功，请手动执行'
        : (res.message || '任务创建成功')
    )
    await loadTaskList()

    if (res.data?.id) {
      router.push(`/task/detail/${res.data.id}`)
    }
  } catch (error) {
    ElMessage.error(error?.message || '任务创建失败')
  } finally {
    creating.value = false
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

const getCategoryText = (value) => categoryTextMap[value] || value || '-'

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

watch(
  () => route.query.create,
  async (value) => {
    if (value === '1') {
      await openCreateDialog()
      router.replace({ path: route.path, query: { ...route.query, create: undefined } })
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadTaskList()
})
</script>

<style scoped lang="scss">
.task-list-page {
  padding: 4px;
}

.workflow-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.workflow-cell strong {
  font-weight: 600;
}

.workflow-cell span {
  color: var(--app-text-muted);
  font-size: 12px;
}

.create-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.schedule-card {
  margin-bottom: 20px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(37, 99, 235, 0.05);
}

.schedule-head h3 {
  margin: 0 0 6px;
}

.schedule-head p {
  margin: 0 0 14px;
  color: var(--app-text-muted);
}

.schedule-mode-group {
  margin-bottom: 14px;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.workflow-summary {
  margin-bottom: 20px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(15, 118, 110, 0.06);
}

.summary-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.summary-title {
  font-size: 16px;
  font-weight: 700;
}

.summary-meta {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.dynamic-form-head h3 {
  margin: 0 0 6px;
}

.dynamic-form-head p {
  margin: 0 0 16px;
  color: var(--app-text-muted);
}

.dynamic-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-description {
  margin-top: 6px;
  color: var(--app-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 960px) {
  .create-grid,
  .schedule-grid,
  .dynamic-grid {
    grid-template-columns: 1fr;
  }
}
</style>
