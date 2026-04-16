<template>
  <div class="ai-analysis-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>AI分析</h2>
        <p>从已完成的数据采集结果发起 AI 分析，并围绕这次分析持续追问。</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <div class="ai-layout">
      <div class="page-section padded ai-panel create-panel">
        <div class="panel-title">新建分析</div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="采集结果" prop="sourceTaskRecordId">
            <el-select
              v-model="form.sourceTaskRecordId"
              placeholder="选择已完成的采集结果"
              filterable
              style="width: 100%"
              @change="handleSourceChange"
            >
              <el-option
                v-for="item in sourceOptions"
                :key="item.taskRecordId"
                :label="formatSourceLabel(item)"
                :value="item.taskRecordId"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="分析流程" prop="workflowId">
            <el-select v-model="form.workflowId" placeholder="选择已发布流程" filterable style="width: 100%">
              <el-option
                v-for="workflow in workflowOptions"
                :key="workflow.id"
                :label="workflow.name"
                :value="workflow.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="执行机器人" prop="robotId">
            <el-select v-model="form.robotId" placeholder="选择执行机器人" style="width: 100%">
              <el-option
                v-for="robot in robotOptions"
                :key="robot.id"
                :label="`${robot.name} (${robot.typeDisplayName || robot.type || '-'})`"
                :value="robot.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="初始问题">
            <el-input
              v-model="form.query"
              type="textarea"
              :rows="4"
              placeholder="可选：比如“这篇内容主要在讲什么？”留空则走默认分析。"
            />
          </el-form-item>

          <div class="page-actions">
            <el-button type="primary" :loading="creating" @click="handleCreate">创建分析任务</el-button>
          </div>
        </el-form>

        <div v-if="selectedSourcePreview" class="source-preview">
          <div class="panel-title small-title">当前来源预览</div>
          <div class="source-preview-title">{{ selectedSourcePreview.title || selectedSourcePreview.taskName || '-' }}</div>
          <div class="source-preview-url">{{ selectedSourcePreview.finalUrl || '-' }}</div>
          <div class="source-preview-summary">{{ shortText(selectedSourcePreview.summaryText) }}</div>
        </div>
      </div>

      <div class="page-section padded ai-panel task-panel">
        <div class="panel-title">分析任务</div>
        <el-table
          :data="aiTasks"
          v-loading="tasksLoading"
          border
          stripe
          height="620"
          @row-click="handleSelectTask"
        >
          <el-table-column prop="name" label="任务名称" min-width="220" show-overflow-tooltip />
          <el-table-column prop="sourceTaskId" label="来源任务" width="160" show-overflow-tooltip />
          <el-table-column prop="workflowName" label="流程" width="140" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'">{{ statusTextMap[row.status] || row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="handleSelectTask(row)">查看</el-button>
              <el-button link type="success" :disabled="row.status !== 'pending'" @click.stop="handleExecute(row)">执行</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-section padded ai-panel chat-panel">
        <template v-if="currentTask">
          <div class="panel-title">分析与问答</div>
          <el-descriptions :column="1" border class="task-meta">
            <el-descriptions-item label="任务名称">{{ currentTask.name }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTypeMap[currentTask.status] || 'info'">
                {{ statusTextMap[currentTask.status] || currentTask.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="来源任务">{{ currentTask.sourceTaskId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源标题">{{ currentTask.sourceTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="流程">{{ currentTask.workflowName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="初始问题">{{ currentTask.query || '使用默认分析提示' }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-heading compact-heading">
            <div>
              <h3>首次分析结果</h3>
              <p>任务执行完成后，这里会展示首次分析输出。</p>
            </div>
          </div>
          <pre class="detail-pre analysis-output">{{ currentTask.result || '任务尚未产出分析结果。' }}</pre>

          <div class="section-heading compact-heading">
            <div>
              <h3>问答记录</h3>
              <p>围绕当前这次分析继续追问，历史会绑定在当前任务下保存。</p>
            </div>
          </div>

          <div class="message-list">
            <el-empty v-if="messages.length === 0" description="当前还没有问答记录" />
            <div v-for="message in messages" :key="message.id" class="message-item" :class="message.role">
              <div class="message-role">{{ message.role === 'assistant' ? 'AI' : '你' }}</div>
              <div class="message-content">{{ message.content }}</div>
              <div class="message-time">{{ formatDateTime(message.createTime) }}</div>
            </div>
          </div>

          <div class="ask-box">
            <el-input
              v-model="question"
              type="textarea"
              :rows="4"
              :disabled="currentTask.status !== 'completed' || asking"
              placeholder="基于这次分析继续提问，例如：这段内容里最关键的风险点是什么？"
            />
            <div class="page-actions">
              <el-button
                type="primary"
                :disabled="currentTask.status !== 'completed'"
                :loading="asking"
                @click="handleAsk"
              >
                发送问题
              </el-button>
            </div>
          </div>
        </template>
        <el-empty v-else description="请选择一个 AI 分析任务" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAiAnalysisTask, getAiAnalysisMessages, sendAiAnalysisMessage } from '../../api/aiAnalysis.js'
import { getCrawlResultDetail, getCrawlResultList } from '../../api/crawl.js'
import { getAllRobots } from '../../api/robot.js'
import { getTaskDetail, getTaskList, startTask } from '../../api/task.js'
import { getAllWorkflows } from '../../api/workflow.js'

const route = useRoute()

const formRef = ref(null)
const creating = ref(false)
const asking = ref(false)
const tasksLoading = ref(false)
const sourceLoading = ref(false)
const sourceOptions = ref([])
const workflowOptions = ref([])
const robotOptions = ref([])
const aiTasks = ref([])
const messages = ref([])
const currentTask = ref(null)
const selectedSourcePreview = ref(null)
const selectedTaskId = ref(null)
const question = ref('')

const form = reactive({
  sourceTaskRecordId: null,
  workflowId: null,
  robotId: null,
  query: ''
})

const rules = {
  sourceTaskRecordId: [{ required: true, message: '请选择采集结果', trigger: 'change' }],
  workflowId: [{ required: true, message: '请选择分析流程', trigger: 'change' }],
  robotId: [{ required: true, message: '请选择执行机器人', trigger: 'change' }]
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

const sourceQueryTaskId = computed(() => route.query.sourceTaskId || '')
const sourceQueryTaskRecordId = computed(() => {
  const value = route.query.sourceTaskRecordId
  return value ? Number(value) : null
})

const formatSourceLabel = (item) => {
  return item.title || item.taskName || item.finalUrl || item.taskId
}

const shortText = (value) => {
  if (!value) return '暂无摘要'
  return value.length > 180 ? `${value.slice(0, 180)}...` : value
}

const loadSourceOptions = async () => {
  sourceLoading.value = true
  try {
    const res = await getCrawlResultList({
      page: 1,
      size: 100,
      status: 'completed'
    })
    sourceOptions.value = res.data?.content || []

    const preferred = sourceQueryTaskRecordId.value
      || sourceOptions.value.find((item) => item.taskId === sourceQueryTaskId.value)?.taskRecordId
    if (preferred) {
      form.sourceTaskRecordId = preferred
      await handleSourceChange(preferred)
    }
  } finally {
    sourceLoading.value = false
  }
}

const loadWorkflowOptions = async () => {
  const res = await getAllWorkflows()
  workflowOptions.value = (res.data || []).filter((item) => item.status === 'published')
}

const loadRobotOptions = async () => {
  const res = await getAllRobots()
  robotOptions.value = (res.data || []).filter((item) => ['online', 'running'].includes(item.status))
}

const loadAiTasks = async (preferredTaskId = null) => {
  tasksLoading.value = true
  try {
    const res = await getTaskList({
      page: 1,
      size: 50,
      type: 'ai_workflow'
    })
    aiTasks.value = res.data?.content || []

    if (preferredTaskId) {
      selectedTaskId.value = preferredTaskId
    } else if (!selectedTaskId.value && aiTasks.value.length > 0) {
      selectedTaskId.value = aiTasks.value[0].id
    } else if (selectedTaskId.value && !aiTasks.value.some((item) => item.id === selectedTaskId.value)) {
      selectedTaskId.value = aiTasks.value[0]?.id || null
    }

    if (selectedTaskId.value) {
      await loadCurrentTask(selectedTaskId.value)
    } else {
      currentTask.value = null
      messages.value = []
    }
  } finally {
    tasksLoading.value = false
  }
}

const loadCurrentTask = async (taskId) => {
  const res = await getTaskDetail(taskId, { silent: true })
  currentTask.value = res.data
  selectedTaskId.value = taskId
  await Promise.all([
    loadMessages(taskId),
    loadCurrentSourcePreview()
  ])
}

const loadMessages = async (taskId) => {
  const res = await getAiAnalysisMessages(taskId, { silent: true })
  messages.value = res.data || []
}

const loadCurrentSourcePreview = async () => {
  if (!currentTask.value?.sourceTaskId) {
    return
  }
  try {
    const res = await getCrawlResultDetail(currentTask.value.sourceTaskId, { silent: true })
    selectedSourcePreview.value = res.data
  } catch (error) {
    selectedSourcePreview.value = null
  }
}

const handleSourceChange = async (taskRecordId) => {
  const selected = sourceOptions.value.find((item) => item.taskRecordId === taskRecordId)
  if (selected?.taskId) {
    const res = await getCrawlResultDetail(selected.taskId, { silent: true })
    selectedSourcePreview.value = res.data
  } else {
    selectedSourcePreview.value = selected || null
  }
}

const handleCreate = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const res = await createAiAnalysisTask({
      sourceTaskRecordId: form.sourceTaskRecordId,
      workflowId: form.workflowId,
      robotId: form.robotId,
      query: form.query || null
    })
    ElMessage.success(res.message || 'AI 分析任务创建成功')
    question.value = ''
    await loadAiTasks(res.data?.id)
  } finally {
    creating.value = false
  }
}

const handleExecute = async (row) => {
  await startTask(row.id)
  ElMessage.success('分析任务已启动')
  await loadAiTasks(row.id)
}

const handleSelectTask = async (row) => {
  if (!row?.id) return
  await loadCurrentTask(row.id)
}

const handleAsk = async () => {
  const text = question.value.trim()
  if (!text || !currentTask.value?.id) {
    return
  }
  asking.value = true
  try {
    const res = await sendAiAnalysisMessage(currentTask.value.id, text)
    messages.value = res.data || []
    question.value = ''
  } finally {
    asking.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([
    loadSourceOptions(),
    loadWorkflowOptions(),
    loadRobotOptions()
  ])
  await loadAiTasks(selectedTaskId.value)
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

watch(
  () => route.query.sourceTaskRecordId,
  async (value) => {
    if (value) {
      form.sourceTaskRecordId = Number(value)
      await handleSourceChange(form.sourceTaskRecordId)
    }
  }
)

onMounted(async () => {
  await refreshAll()
})
</script>

<style scoped lang="scss">
.ai-analysis-page {
  padding: 4px;
}

.ai-layout {
  display: grid;
  grid-template-columns: minmax(280px, 320px) minmax(360px, 1fr) minmax(420px, 1.1fr);
  gap: 16px;
  align-items: start;
}

.ai-panel {
  min-height: 680px;
}

.panel-title {
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 700;
}

.small-title {
  margin-top: 24px;
  font-size: 15px;
}

.source-preview {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
}

.source-preview-title {
  font-size: 16px;
  font-weight: 600;
}

.source-preview-url {
  margin-top: 6px;
  color: var(--app-primary);
  word-break: break-all;
}

.source-preview-summary {
  margin-top: 10px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.task-meta {
  margin-bottom: 18px;
}

.analysis-output {
  min-height: 180px;
  max-height: 260px;
  overflow: auto;
}

.message-list {
  display: grid;
  gap: 12px;
  min-height: 220px;
  max-height: 360px;
  overflow: auto;
  margin-bottom: 16px;
}

.message-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.message-item.assistant {
  background: rgba(14, 165, 233, 0.08);
  border-color: rgba(14, 165, 233, 0.18);
}

.message-role {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--app-text-muted);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: var(--app-text);
}

.message-time {
  margin-top: 10px;
  font-size: 12px;
  color: var(--app-text-muted);
}

.ask-box {
  margin-top: 16px;
}

@media (max-width: 1440px) {
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .ai-panel {
    min-height: auto;
  }
}
</style>
