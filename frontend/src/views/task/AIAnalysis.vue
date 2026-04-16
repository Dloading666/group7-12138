<template>
  <div class="ai-analysis-page app-page">
    <div class="ai-page-head">
      <div class="page-title-block">
        <h2>AI 分析</h2>
        <p>上方用于新建分析和切换任务，下方直接进入统一的问答区。</p>
      </div>
      <el-button class="page-refresh" @click="refreshAll">
        <el-icon><RefreshRight /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="top-grid">
      <section class="page-section padded surface-card create-card" v-loading="sourceLoading">
        <div class="card-head">
          <div class="card-icon mint">
            <el-icon><DocumentAdd /></el-icon>
          </div>
          <div>
            <h3>新建分析</h3>
            <p>选择采集结果和分析流程后创建任务，首次分析结果会自动进入下面的对话区。</p>
          </div>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="create-form">
          <div class="create-form-grid">
            <el-form-item label="数据来源" prop="sourceTaskRecordId">
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
              <el-select
                v-model="form.workflowId"
                placeholder="选择已发布流程"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="workflow in workflowOptions"
                  :key="workflow.id"
                  :label="workflow.name"
                  :value="workflow.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item class="span-2" label="初始问题">
              <el-input
                v-model="form.query"
                type="textarea"
                :rows="4"
                placeholder="可选：比如“这段内容最关键的风险点是什么？”留空则走默认分析。"
              />
            </el-form-item>
          </div>

          <div v-if="selectedSourcePreview" class="source-preview-card">
            <div class="source-preview-label">当前来源预览</div>
            <div class="source-preview-title">{{ selectedSourcePreview.title || selectedSourcePreview.taskName || '-' }}</div>
            <div class="source-preview-url">{{ selectedSourcePreview.finalUrl || '-' }}</div>
            <div class="source-preview-summary">{{ shortText(selectedSourcePreview.summaryText) }}</div>
          </div>

          <el-button class="create-submit" type="primary" :loading="creating" @click="handleCreate">
            创建分析任务
          </el-button>
        </el-form>
      </section>

      <section class="page-section padded surface-card task-card">
        <div class="card-head spread">
          <div class="card-head-main">
            <div class="card-icon ice">
              <el-icon><Tickets /></el-icon>
            </div>
            <div>
              <h3>分析任务</h3>
              <p>选中任务后，历史分析内容和后续追问都会汇总到同一个问答区里。</p>
            </div>
          </div>
          <el-button text type="primary" @click="refreshAll">刷新列表</el-button>
        </div>

        <div class="task-list-shell" v-loading="tasksLoading">
          <div class="task-list-head">
            <span>任务名称</span>
            <span>来源</span>
            <span>状态</span>
            <span>创建时间</span>
          </div>

          <el-empty v-if="!tasksLoading && aiTasks.length === 0" description="暂无 AI 分析任务" />

          <div v-else class="task-list-body">
            <div
              v-for="task in aiTasks"
              :key="task.id"
              class="task-row"
              :class="{ active: selectedTaskId === task.id }"
              @click="handleSelectTask(task)"
            >
              <div class="task-primary">
                <div class="task-name-line">
                  <div class="task-name">{{ task.name }}</div>
                  <el-button
                    v-if="task.status === 'pending'"
                    link
                    type="success"
                    class="task-run-link"
                    @click.stop="handleExecute(task)"
                  >
                    执行
                  </el-button>
                </div>
                <div class="task-subline">
                  <span>{{ getTaskWorkflowName(task) }}</span>
                  <span>ID {{ task.id }}</span>
                </div>
              </div>

              <div class="task-source">{{ getTaskSourceLabel(task) }}</div>

              <div class="task-state">
                <span class="status-chip" :class="`is-${task.status || 'unknown'}`">
                  {{ statusTextMap[task.status] || task.status || '未知' }}
                </span>
              </div>

              <div class="task-time">{{ formatRelativeTime(task.createTime || task.updateTime || task.startTime) }}</div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="page-section padded surface-card workspace-card">
      <template v-if="currentTask">
        <div class="workspace-head">
          <div class="card-head-main">
            <div class="card-icon peach">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div>
              <h3>问答区</h3>
              <p>当前上下文：{{ currentTask.name }}</p>
            </div>
          </div>

          <div class="workspace-tags">
            <span class="workspace-tag">{{ getTaskWorkflowName(currentTask) }}</span>
            <span class="workspace-tag">{{ getTaskSourceLabel(currentTask) }}</span>
            <span class="workspace-tag strong">{{ statusTextMap[currentTask.status] || currentTask.status || '未知' }}</span>
          </div>
        </div>

        <div ref="messageListRef" class="conversation-list workspace-body">
          <el-empty
            v-if="conversationItems.length === 0"
            :description="currentTask.status === 'completed' ? '还没有问答记录，直接在下面继续提问。' : '任务执行完成后，首次分析结果会直接显示在这里。'"
          />

          <div
            v-for="message in conversationItems"
            :key="message.id"
            class="conversation-row"
            :class="[
              message.role === 'assistant' ? 'is-assistant' : 'is-user',
              message.kind === 'initial-analysis' ? 'is-analysis' : '',
              message.kind === 'thinking' ? 'is-thinking' : ''
            ]"
          >
            <div class="message-bubble-wrap">
              <div class="message-corner-avatar" :class="message.role === 'assistant' ? 'is-assistant' : 'is-user'">
                {{ message.role === 'assistant' ? '分析助手' : '用户' }}
              </div>

              <div class="message-meta">
                <span class="message-time">
                  {{ message.kind === 'thinking' ? '思考中...' : formatDateTime(message.createTime) }}
                </span>
              </div>

              <div class="message-bubble">
                <template v-if="message.kind === 'thinking'">
                  <div class="thinking-block" aria-label="思考中">
                    <span class="thinking-text">分析助手正在思考</span>
                    <span class="thinking-dots">
                      <i></i>
                      <i></i>
                      <i></i>
                    </span>
                  </div>
                </template>
                <div v-else class="message-content">{{ message.content }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="composer-box">
          <div class="composer-hint">
            <span v-if="canAsk">按 Enter 发送，Shift + Enter 换行。</span>
            <span v-else>当前任务尚未完成，首次分析结果生成后会自动出现在这里。</span>
          </div>

          <div class="composer-main">
            <el-input
              v-model="question"
              type="textarea"
              :autosize="{ minRows: 2, maxRows: 5 }"
              :disabled="!canAsk || asking"
              class="ask-input"
              placeholder="基于这次分析继续提问..."
              @keydown="handleQuestionKeydown"
            />

            <el-button
              class="send-button"
              type="primary"
              :disabled="!canAsk || !hasQuestion || asking"
              :loading="asking"
              @click="handleAsk"
            >
              <el-icon v-if="!asking" class="send-button-icon"><Promotion /></el-icon>
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-else description="请先在上方选择一个 AI 分析任务" />
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ChatDotRound, DocumentAdd, Promotion, RefreshRight, Tickets } from '@element-plus/icons-vue'
import { createAiAnalysisTask, getAiAnalysisMessages, sendAiAnalysisMessage } from '../../api/aiAnalysis.js'
import { getCrawlResultDetail, getCrawlResultList } from '../../api/crawl.js'
import { getAllRobots } from '../../api/robot.js'
import { getTaskDetail, getTaskList, startTask } from '../../api/task.js'
import { getAllWorkflows } from '../../api/workflow.js'

const route = useRoute()

const formRef = ref(null)
const messageListRef = ref(null)

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
  workflowId: [{ required: true, message: '请选择分析流程', trigger: 'change' }]
}

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
const canAsk = computed(() => currentTask.value?.status === 'completed')
const hasQuestion = computed(() => question.value.trim().length > 0)

let pollTimer = null

const parseTaskParams = (task) => {
  const raw = task?.params
  if (!raw || typeof raw !== 'string') {
    return {}
  }

  try {
    return JSON.parse(raw)
  } catch (error) {
    return {}
  }
}

const getTaskWorkflowName = (task) => {
  const params = parseTaskParams(task)
  return task?.workflowName || params.workflowName || '未配置流程'
}

const getTaskSourceLabel = (task) => {
  const params = parseTaskParams(task)
  return task?.sourceTitle || params.sourceTitle || params.sourceTaskName || params.sourceTaskId || '-'
}

const conversationItems = computed(() => {
  const task = currentTask.value
  const list = Array.isArray(messages.value) ? messages.value.map((item) => ({ ...item })) : []
  const initialContent = String(task?.result || '').trim()
  const hasInitialMessage = list.some((item) => item.role === 'assistant' && item.content === initialContent)

  if (initialContent && !hasInitialMessage) {
    list.unshift({
      id: `initial-analysis-${task?.id || 'unknown'}`,
      analysisTaskId: task?.id || null,
      role: 'assistant',
      content: initialContent,
      createTime: task?.updateTime || task?.completeTime || task?.createTime || null,
      kind: 'initial-analysis'
    })
  }

  return list.map((item, index) => ({
    ...item,
    kind: item.kind || (index === 0 && item.role === 'assistant' ? 'initial-analysis' : 'message')
  }))
})

const formatSourceLabel = (item) => {
  return item.title || item.taskName || item.finalUrl || item.taskId
}

const shortText = (value) => {
  if (!value) return '暂无摘要'
  return value.length > 180 ? `${value.slice(0, 180)}...` : value
}

const scrollMessagesToBottom = (behavior = 'auto') => {
  const container = messageListRef.value
  if (!container) return
  container.scrollTo({
    top: container.scrollHeight,
    behavior
  })
}

const stopPolling = () => {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = null
  }
}

const schedulePolling = () => {
  stopPolling()

  if (!selectedTaskId.value || !['pending', 'running'].includes(currentTask.value?.status)) {
    return
  }

  pollTimer = window.setTimeout(async () => {
    try {
      const res = await getTaskDetail(selectedTaskId.value, { silent: true })
      const nextTask = res.data
      const previousStatus = currentTask.value?.status

      currentTask.value = nextTask

      if (nextTask?.status === 'completed' && previousStatus !== 'completed') {
        await loadMessages(nextTask.id, 'smooth')
      }
    } catch (error) {
      console.error('轮询分析任务失败:', error)
    } finally {
      schedulePolling()
    }
  }, 4000)
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
    } else if (!form.sourceTaskRecordId) {
      selectedSourcePreview.value = null
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
  if (!form.robotId || !robotOptions.value.some((item) => item.id === form.robotId)) {
    form.robotId = robotOptions.value[0]?.id || null
  }
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
  stopPolling()
  const res = await getTaskDetail(taskId, { silent: true })
  currentTask.value = res.data
  selectedTaskId.value = taskId
  await loadMessages(taskId)
  schedulePolling()
}

const loadMessages = async (taskId, behavior = 'auto') => {
  const res = await getAiAnalysisMessages(taskId, { silent: true })
  messages.value = res.data || []
  await nextTick()
  scrollMessagesToBottom(behavior)
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

  if (!form.robotId) {
    ElMessage.warning('当前没有可用执行节点，请稍后再试')
    return
  }

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

const handleExecute = async (task) => {
  await startTask(task.id)
  ElMessage.success('分析任务已启动')
  await loadAiTasks(task.id)
}

const handleSelectTask = async (task) => {
  if (!task?.id) return
  await loadCurrentTask(task.id)
}

const handleAsk = async () => {
  const text = question.value.trim()
  const taskId = currentTask.value?.id

  if (!text || !taskId || !canAsk.value || asking.value) {
    return
  }

  const snapshot = Array.isArray(messages.value) ? messages.value.map((item) => ({ ...item })) : []
  const now = new Date().toISOString()
  const optimisticMessages = [
    ...snapshot,
    {
      id: `temp-user-${Date.now()}`,
      analysisTaskId: taskId,
      role: 'user',
      content: text,
      createTime: now,
      kind: 'message'
    },
    {
      id: `temp-thinking-${Date.now()}`,
      analysisTaskId: taskId,
      role: 'assistant',
      content: '思考中',
      createTime: now,
      kind: 'thinking'
    }
  ]

  messages.value = optimisticMessages
  question.value = ''
  asking.value = true

  await nextTick()
  scrollMessagesToBottom('smooth')

  try {
    const res = await sendAiAnalysisMessage(taskId, text)
    messages.value = res.data || []
    await nextTick()
    scrollMessagesToBottom('smooth')
  } catch (error) {
    messages.value = snapshot
    question.value = text
    ElMessage.error('发送失败，请重试')
  } finally {
    asking.value = false
  }
}

const handleQuestionKeydown = (event) => {
  if (event.isComposing) {
    return
  }

  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void handleAsk()
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

const formatRelativeTime = (value) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return formatDateTime(value)
  }

  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.max(1, Math.floor(diff / hour))} 小时前`
  if (diff < day * 2) return '昨天'

  return formatDateTime(value).slice(0, 16)
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

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped lang="scss">
.ai-analysis-page {
  padding: 6px;
}

.ai-page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title-block h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.page-title-block p {
  margin: 8px 0 0;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.page-refresh {
  border-radius: 14px;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(360px, 440px) minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
}

.surface-card {
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 252, 0.95));
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.card-head,
.card-head-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.card-head {
  margin-bottom: 22px;
}

.card-head.spread {
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.card-head h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.card-head p {
  margin: 6px 0 0;
  color: var(--app-text-muted);
  line-height: 1.65;
}

.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border-radius: 16px;
  font-size: 24px;
}

.card-icon.mint {
  background: #d8f4ef;
  color: #0f766e;
}

.card-icon.ice {
  background: #dcedff;
  color: #2563eb;
}

.card-icon.peach {
  background: #ffe6d7;
  color: #c2410c;
}

.create-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.create-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.create-form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.create-form-grid :deep(.el-form-item__label) {
  color: var(--app-text);
  font-weight: 600;
}

.create-form :deep(.el-select__wrapper),
.create-form :deep(.el-textarea__inner) {
  border-radius: 16px;
  background: #f4f7fb;
  box-shadow: none;
}

.create-form :deep(.el-textarea__inner) {
  min-height: 132px;
  padding: 16px 18px;
  line-height: 1.7;
}

.source-preview-card {
  padding: 16px 18px;
  border: 1px solid rgba(15, 118, 110, 0.12);
  border-radius: 20px;
  background: rgba(240, 253, 250, 0.75);
}

.source-preview-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--app-text-muted);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.source-preview-title {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
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

.create-submit {
  width: 100%;
  height: 52px;
  border-radius: 18px;
  border-color: #0f766e;
  background: #0f766e;
  font-weight: 700;
}

.task-list-shell {
  min-height: 100%;
}

.task-list-head,
.task-row {
  display: grid;
  grid-template-columns: minmax(0, 1.9fr) minmax(120px, 1fr) 100px 110px;
  gap: 14px;
  align-items: center;
}

.task-list-head {
  padding: 0 12px 10px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 700;
}

.task-list-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 450px;
  overflow: auto;
  padding-right: 4px;
}

.task-row {
  padding: 16px 14px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.task-row:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.task-row.active {
  border-color: rgba(15, 118, 110, 0.26);
  background: linear-gradient(180deg, rgba(240, 253, 250, 0.92), rgba(255, 255, 255, 0.98));
  box-shadow: 0 18px 32px rgba(15, 118, 110, 0.08);
}

.task-primary {
  min-width: 0;
}

.task-name-line {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.task-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 700;
}

.task-run-link {
  padding: 0;
}

.task-subline {
  display: flex;
  gap: 12px;
  margin-top: 7px;
  color: var(--app-text-muted);
  font-size: 12px;
  flex-wrap: wrap;
}

.task-source,
.task-time {
  color: var(--app-text-muted);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.status-chip.is-completed {
  background: #d8f4ef;
  color: #0f766e;
}

.status-chip.is-running {
  background: #dff5ff;
  color: #2563eb;
}

.status-chip.is-pending {
  background: #eef2ff;
  color: #4f46e5;
}

.status-chip.is-failed {
  background: #fee2e2;
  color: #dc2626;
}

.workspace-card {
  display: flex;
  flex-direction: column;
  min-height: 720px;
  margin-top: 18px;
}

.workspace-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.workspace-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.workspace-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: #f4f7fb;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 600;
}

.workspace-tag.strong {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.workspace-body {
  flex: 1;
  min-height: 420px;
  max-height: 780px;
  overflow: auto;
  padding: 8px 6px 12px 2px;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.conversation-row {
  display: flex;
  align-items: flex-end;
  gap: 14px;
}

.conversation-row.is-user {
  justify-content: flex-end;
}

.message-bubble-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 18px;
  max-width: min(82%, 780px);
}

.conversation-row.is-user .message-bubble-wrap {
  align-items: flex-end;
}

.message-corner-avatar {
  position: absolute;
  top: 0;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.01em;
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.08);
}

.message-corner-avatar.is-assistant {
  left: 16px;
  background: #d8f4ef;
  color: #0f766e;
}

.message-corner-avatar.is-user {
  right: 16px;
  background: #1f2633;
  color: #f8fbff;
}

.message-meta {
  display: flex;
  width: 100%;
  box-sizing: border-box;
  align-items: center;
  min-height: 24px;
  padding: 0 18px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.conversation-row.is-assistant .message-meta {
  padding-left: 92px;
}

.conversation-row.is-user .message-meta {
  justify-content: flex-end;
  padding-right: 72px;
}

.message-bubble {
  padding: 26px 18px 16px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 22px;
  background: #f5f7fb;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.conversation-row.is-assistant .message-bubble {
  border-top-left-radius: 10px;
}

.conversation-row.is-analysis .message-bubble {
  background: linear-gradient(180deg, #f2f8ff, #edf6ff);
  border-color: rgba(37, 99, 235, 0.14);
}

.conversation-row.is-user .message-bubble {
  border-top-right-radius: 10px;
  background: #1f2633;
  border-color: rgba(31, 38, 51, 0.22);
}

.conversation-row.is-thinking .message-bubble {
  background: #eef6ff;
  border-color: rgba(37, 99, 235, 0.18);
}

.message-content {
  color: var(--app-text);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.78;
}

.conversation-row.is-user .message-content {
  color: #f8fbff;
}

.thinking-block {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #2563eb;
  font-weight: 600;
}

.thinking-text {
  line-height: 1.6;
}

.thinking-dots {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.thinking-dots i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #2563eb;
  opacity: 0.25;
  animation: thinkingPulse 1.2s infinite ease-in-out;
}

.thinking-dots i:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots i:nth-child(3) {
  animation-delay: 0.4s;
}

.composer-box {
  margin-top: 16px;
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 247, 251, 0.96));
}

.composer-hint {
  margin-bottom: 12px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.composer-main {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.ask-input {
  flex: 1;
}

.ask-input :deep(.el-textarea__inner) {
  min-height: 74px !important;
  padding: 16px 18px;
  border-radius: 18px;
  background: #f4f7fb;
  box-shadow: none;
  line-height: 1.7;
  resize: none;
}

.send-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  padding: 0;
  border-radius: 18px;
  border-color: #0f766e;
  background: #0f766e;
  font-size: 20px;
}

.send-button-icon {
  font-size: 18px;
  transform: rotate(-12deg) translate(1px, -1px);
}

@keyframes thinkingPulse {
  0%,
  80%,
  100% {
    transform: scale(0.8);
    opacity: 0.25;
  }

  40% {
    transform: scale(1);
    opacity: 0.85;
  }
}

@media (max-width: 1280px) {
  .top-grid {
    grid-template-columns: 1fr;
  }

  .workspace-head {
    flex-direction: column;
  }

  .workspace-tags {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .create-form-grid,
  .task-list-head,
  .task-row {
    grid-template-columns: 1fr;
  }

  .task-list-head {
    display: none;
  }

  .task-row {
    gap: 10px;
  }

  .task-source,
  .task-time {
    white-space: normal;
  }
}

@media (max-width: 768px) {
  .ai-page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .page-refresh {
    width: 100%;
  }

  .message-bubble-wrap {
    max-width: 100%;
  }

  .composer-main {
    flex-direction: column;
    align-items: stretch;
  }

  .send-button {
    width: 100%;
    flex: none;
  }
}
</style>
