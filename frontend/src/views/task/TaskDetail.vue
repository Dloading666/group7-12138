<template>
  <div class="task-detail app-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>任务详情</h2>
        <p>这里按运行记录查看任务结果、执行参数、执行日志，以及已完成运行的内置 AI 分析。</p>
      </div>

      <div class="page-header-actions">
        <el-tag v-if="task" :type="statusTypeMap[task.latestRunStatus || task.status] || 'info'">
          {{ statusTextMap[task.latestRunStatus || task.status] || task.latestRunStatus || task.status || '-' }}
        </el-tag>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <div v-if="task" class="detail-layout">
      <div class="detail-main">
        <div class="page-section padded">
          <div class="status-grid detail-metrics">
            <div class="status-metric">
              <div class="metric-label">任务编号</div>
              <div class="metric-value">{{ task.taskId }}</div>
            </div>
            <div class="status-metric">
              <div class="metric-label">流程名称</div>
              <div class="metric-value">{{ task.workflowName || '-' }}</div>
            </div>
            <div class="status-metric">
              <div class="metric-label">流程分类</div>
              <div class="metric-value">{{ getCategoryText(task.workflowCategory) }}</div>
            </div>
            <div class="status-metric">
              <div class="metric-label">运行次数</div>
              <div class="metric-value">{{ task.runCount || runs.length }}</div>
            </div>
          </div>

          <el-descriptions :column="2" border class="detail-descriptions">
            <el-descriptions-item label="任务名称">{{ task.name }}</el-descriptions-item>
            <el-descriptions-item label="执行方式">
              {{ getScheduleSummary(task) }}
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(task.createTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="shouldShowNextTrigger(task)" label="下次触发">
              {{ formatDateTime(task.nextRunTime || task.scheduledTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="最新状态">
              <el-tag :type="statusTypeMap[task.latestRunStatus || task.status] || 'info'">
                {{ statusTextMap[task.latestRunStatus || task.status] || task.latestRunStatus || task.status || '-' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最近运行">
              {{ formatDateTime(task.lastRunTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="任务描述" :span="2">{{ task.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="page-section padded">
          <div class="section-heading">
            <div>
              <h3>运行详情</h3>
              <p>选择一条运行记录后，可以查看这次运行的结果、参数、日志和分析内容。</p>
            </div>
          </div>

          <el-empty v-if="runs.length === 0" description="当前任务还没有运行记录" />

          <template v-else>
            <el-tabs v-model="activeTab" @tab-change="syncRouteQuery">
              <el-tab-pane label="运行结果" name="result">
                <div v-if="selectedRun" class="run-result">
                  <el-descriptions :column="2" border>
                    <el-descriptions-item label="运行编号">{{ selectedRun.runId }}</el-descriptions-item>
                    <el-descriptions-item label="触发方式">{{ getTriggerText(selectedRun.triggerType) }}</el-descriptions-item>
                    <el-descriptions-item label="运行状态">
                      <el-tag :type="statusTypeMap[selectedRun.status] || 'info'">
                        {{ statusTextMap[selectedRun.status] || selectedRun.status || '-' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="运行耗时">{{ formatDuration(selectedRun.duration) }}</el-descriptions-item>
                    <el-descriptions-item label="开始时间">{{ formatDateTime(selectedRun.startTime) }}</el-descriptions-item>
                    <el-descriptions-item label="结束时间">{{ formatDateTime(selectedRun.endTime) }}</el-descriptions-item>
                    <el-descriptions-item label="结果摘要" :span="2">
                      <pre class="detail-pre">{{ selectedRun.result || '-' }}</pre>
                    </el-descriptions-item>
                    <el-descriptions-item v-if="selectedRun.errorMessage" label="错误信息" :span="2">
                      <span class="error-text">{{ selectedRun.errorMessage }}</span>
                    </el-descriptions-item>
                  </el-descriptions>

                  <div v-if="selectedRun.stepRuns?.length" class="step-result-card">
                    <div class="crawl-title">步骤执行</div>
                    <div class="step-run-list">
                      <div v-for="step in selectedRun.stepRuns" :key="step.stepRunId" class="step-run-card">
                        <div class="run-card-head">
                          <strong>{{ step.nodeLabel || step.nodeId }}</strong>
                          <el-tag size="small" :type="statusTypeMap[step.status] || 'info'">
                            {{ statusTextMap[step.status] || step.status || '-' }}
                          </el-tag>
                        </div>
                        <div class="run-card-meta">{{ step.nodeType }} · {{ step.branchKey || 'main' }}</div>
                        <div v-if="step.robotName" class="run-card-meta">
                          机器人：{{ step.robotName }}<span v-if="step.robotType"> ({{ step.robotType }})</span>
                        </div>
                        <div v-if="step.errorMessage" class="error-text">{{ step.errorMessage }}</div>
                      </div>
                    </div>
                  </div>

                  <div v-if="crawlResult" class="crawl-result-card">
                    <div class="crawl-title">抓取结果补充</div>
                    <el-descriptions :column="2" border>
                      <el-descriptions-item label="最终 URL" :span="2">{{ crawlResult.finalUrl || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="页面标题" :span="2">{{ crawlResult.title || '-' }}</el-descriptions-item>
                      <el-descriptions-item label="结果条数">{{ crawlResult.totalCount || 0 }}</el-descriptions-item>
                      <el-descriptions-item label="抓取页数">{{ crawlResult.crawledPages || 0 }}</el-descriptions-item>
                      <el-descriptions-item label="摘要" :span="2">
                        <pre class="detail-pre">{{ crawlResult.summaryText || '-' }}</pre>
                      </el-descriptions-item>
                    </el-descriptions>

                    <div v-if="crawlResult.structuredData?.length" class="structured-block">
                      <div class="crawl-title">结构化结果</div>
                      <el-table :data="crawlResult.structuredData" border stripe max-height="320">
                        <el-table-column
                          v-for="(_, key) in crawlResult.structuredData[0]"
                          :key="key"
                          :prop="key"
                          :label="key"
                          min-width="160"
                          show-overflow-tooltip
                        />
                      </el-table>
                    </div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="执行参数" name="input">
                <pre class="detail-pre">{{ formatJson(selectedRun?.inputConfig) }}</pre>
              </el-tab-pane>

              <el-tab-pane label="执行日志" name="logs">
                <el-empty v-if="runLogs.length === 0" description="当前运行暂无日志" />
                <div v-else class="log-list">
                  <div v-for="item in runLogs" :key="item.id" class="log-item">
                    <div class="log-head">
                      <el-tag size="small" :type="logTagType(item.level)">{{ item.level }}</el-tag>
                      <span>{{ formatDateTime(item.createTime) }}</span>
                    </div>
                    <div class="log-message">{{ item.message }}</div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="AI 分析" name="analysis">
                <div v-if="!selectedRun" class="analysis-empty">
                  <el-empty description="请先选择一条运行记录" />
                </div>

                <div v-else-if="selectedRun.status !== 'completed'" class="analysis-empty">
                  <el-empty description="只有已完成运行才支持正式分析，当前可以继续观察运行状态。" />
                </div>

                <div v-else class="analysis-panel">
                  <div class="analysis-actions">
                    <el-button @click="sendPreset('请基于这次运行生成一份结构化摘要。')">生成摘要</el-button>
                    <el-button @click="sendPreset('请提取这次运行的风险点、不确定项和排查建议。')">提取风险点</el-button>
                    <el-button @click="sendPreset('请基于任务结果、执行参数和执行日志给出最终结论。')">生成结论</el-button>
                  </div>

                  <div class="analysis-context-tip">
                    分析上下文固定包含任务结果、执行参数、执行日志，以及可用的抓取补充结果。
                  </div>

                  <div class="message-list">
                    <el-empty v-if="analysisMessages.length === 0" description="还没有分析消息，可以先点上方快捷操作或直接提问。" />
                    <div
                      v-for="message in analysisMessages"
                      :key="message.id"
                      class="message-item"
                      :class="message.role === 'assistant' ? 'is-assistant' : 'is-user'"
                    >
                      <div class="message-role">{{ message.role === 'assistant' ? '分析助手' : '用户' }}</div>
                      <div class="message-content">{{ message.content }}</div>
                      <div class="message-time">{{ formatDateTime(message.createTime) }}</div>
                    </div>
                  </div>

                  <div class="analysis-composer">
                    <el-input
                      v-model="question"
                      type="textarea"
                      :rows="3"
                      placeholder="围绕这次运行继续提问..."
                      @keydown="handleQuestionKeydown"
                    />
                    <div class="page-actions">
                      <el-button type="primary" :loading="asking" :disabled="!question.trim()" @click="sendQuestion()">
                        发送
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
        </div>
      </div>

      <div class="detail-side">
        <div class="page-section padded">
          <div class="side-title">运行历史</div>
          <el-empty v-if="runs.length === 0" description="暂无运行记录" />
          <div v-else class="run-list">
            <div
              v-for="item in runs"
              :key="item.id"
              class="run-card"
              :class="{ active: selectedRunId === item.id }"
              @click="selectRun(item.id)"
            >
              <div class="run-card-head">
                <strong>{{ item.runId }}</strong>
                <el-tag size="small" :type="statusTypeMap[item.status] || 'info'">
                  {{ statusTextMap[item.status] || item.status || '-' }}
                </el-tag>
              </div>
              <div class="run-card-meta">{{ getTriggerText(item.triggerType) }}</div>
              <div class="run-card-time">{{ formatDateTime(item.createTime) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTaskDetail,
  getTaskRuns,
  getTaskRunAnalysisMessages,
  getTaskRunCrawlResult,
  getTaskRunDetail,
  getTaskRunLogs,
  sendTaskRunAnalysisMessage
} from '../../api/task.js'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const asking = ref(false)
const task = ref(null)
const runs = ref([])
const selectedRun = ref(null)
const selectedRunId = ref(null)
const runLogs = ref([])
const crawlResult = ref(null)
const analysisMessages = ref([])
const activeTab = ref(String(route.query.tab || 'result'))
const question = ref('')

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

const triggerTextMap = {
  manual: '手动触发',
  immediate: '立即执行',
  cron: 'Cron 触发',
  scheduled: '定时触发',
  legacy_backfill: '历史补齐'
}

const weekdayOptions = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' }
]

const loadTask = async () => {
  loading.value = true
  try {
    const [taskRes, runsRes] = await Promise.all([
      getTaskDetail(route.params.id, { silent: true }),
      getTaskRuns(route.params.id, { silent: true })
    ])
    task.value = taskRes.data
    runs.value = runsRes.data || []

    if (!runs.value.length) {
      selectedRun.value = null
      selectedRunId.value = null
      runLogs.value = []
      crawlResult.value = null
      analysisMessages.value = []
      return
    }

    const preferredRunId = route.query.runId ? Number(route.query.runId) : null
    const matchedRun = preferredRunId && runs.value.some((item) => item.id === preferredRunId)
      ? preferredRunId
      : runs.value[0].id

    await selectRun(matchedRun, false)
  } finally {
    loading.value = false
  }
}

const selectRun = async (runId, updateRoute = true) => {
  if (!runId) {
    return
  }

  selectedRunId.value = runId
  const [runRes, logsRes] = await Promise.all([
    getTaskRunDetail(runId, { silent: true }),
    getTaskRunLogs(runId, { silent: true })
  ])

  selectedRun.value = runRes.data
  runLogs.value = logsRes.data || []

  try {
    const crawlRes = await getTaskRunCrawlResult(runId, { silent: true })
    crawlResult.value = crawlRes.data
  } catch (error) {
    crawlResult.value = null
  }

  if (selectedRun.value?.status === 'completed') {
    const messagesRes = await getTaskRunAnalysisMessages(runId, { silent: true })
    analysisMessages.value = messagesRes.data || []
  } else {
    analysisMessages.value = []
  }

  if (updateRoute) {
    syncRouteQuery()
  }
}

const syncRouteQuery = () => {
  router.replace({
    path: `/task/detail/${route.params.id}`,
    query: {
      ...(selectedRunId.value ? { runId: String(selectedRunId.value) } : {}),
      ...(activeTab.value ? { tab: activeTab.value } : {})
    }
  })
}

const sendQuestion = async (presetQuestion = '') => {
  if (!selectedRun.value || selectedRun.value.status !== 'completed') {
    return
  }

  const content = String(presetQuestion || question.value).trim()
  if (!content) {
    return
  }

  asking.value = true
  try {
    const res = await sendTaskRunAnalysisMessage(selectedRun.value.id, content, { silent: true })
    analysisMessages.value = res.data || []
    question.value = ''
    activeTab.value = 'analysis'
    syncRouteQuery()
  } catch (error) {
    ElMessage.error(error?.message || '发送分析消息失败')
  } finally {
    asking.value = false
  }
}

const sendPreset = (content) => sendQuestion(content)

const handleQuestionKeydown = (event) => {
  if (event.isComposing) return
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void sendQuestion()
  }
}

const getCategoryText = (value) => categoryTextMap[value] || value || '-'
const getTriggerText = (value) => triggerTextMap[value] || value || '-'

const parseScheduleConfig = (raw) => {
  if (!raw) return {}
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (error) {
    return {}
  }
}

const formatWeekdays = (days = []) => {
  const labels = days
    .map((day) => weekdayOptions.find((item) => item.value === day)?.label || day)
    .filter(Boolean)
  return labels.join('、')
}

const shouldShowNextTrigger = (row) => {
  const schedule = parseScheduleConfig(row?.scheduleConfig)
  const mode = schedule.mode || row?.executeType
  return ['scheduled', 'cron'].includes(mode) && Boolean(row?.nextRunTime || row?.scheduledTime)
}

const getScheduleSummary = (row) => {
  const schedule = parseScheduleConfig(row?.scheduleConfig)
  const presetType = schedule.presetType

  if ((schedule.mode || row?.executeType) === 'immediate') {
    return '手动执行'
  }
  if (presetType === 'daily') {
    return `每天 ${schedule.time || '--:--'}`
  }
  if (presetType === 'weekly') {
    return `每周 ${formatWeekdays(schedule.days || [])} ${schedule.time || '--:--'}`
  }
  if (row?.executeType === 'cron') {
    return 'Cron 调度'
  }
  return '手动执行'
}

const logTagType = (level) => {
  if (level === 'ERROR') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatDuration = (value) => {
  if (value !== 0 && !value) return '-'
  if (value < 60) return `${value} 秒`
  const minute = Math.floor(value / 60)
  const second = value % 60
  return `${minute} 分 ${second} 秒`
}

const formatJson = (value) => {
  if (!value) return '-'
  try {
    return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
  } catch (error) {
    return String(value)
  }
}

const goBack = () => router.back()

watch(
  () => route.params.id,
  () => {
    loadTask()
  }
)

watch(
  () => route.query.tab,
  (value) => {
    activeTab.value = String(value || 'result')
  }
)

watch(
  () => route.query.runId,
  async (value) => {
    const runId = value ? Number(value) : null
    if (runId && runId !== selectedRunId.value && runs.value.some((item) => item.id === runId)) {
      await selectRun(runId, false)
    }
  }
)

onMounted(() => {
  loadTask()
})
</script>

<style scoped lang="scss">
.task-detail {
  padding: 4px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.detail-main,
.detail-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-metrics {
  margin-bottom: 16px;
}

.detail-descriptions {
  margin-top: 4px;
}

.detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.error-text {
  color: var(--app-danger);
}

.side-title,
.crawl-title {
  margin-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
}

.run-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.run-card {
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.run-card.active {
  border-color: rgba(15, 118, 110, 0.35);
  background: rgba(15, 118, 110, 0.06);
}

.run-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.run-card-meta,
.run-card-time {
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.crawl-result-card,
.structured-block {
  margin-top: 18px;
}

.step-run-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.step-run-card {
  padding: 14px;
  border-radius: 14px;
  background: rgba(148, 163, 184, 0.08);
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.log-item {
  padding: 14px;
  border-radius: 14px;
  background: rgba(148, 163, 184, 0.08);
}

.log-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.log-message {
  line-height: 1.7;
  white-space: pre-wrap;
}

.analysis-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analysis-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.analysis-context-tip {
  padding: 12px 14px;
  border-radius: 14px;
  color: var(--app-text-muted);
  background: rgba(15, 118, 110, 0.06);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-item {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(148, 163, 184, 0.08);
}

.message-item.is-user {
  background: rgba(37, 99, 235, 0.08);
}

.message-role {
  margin-bottom: 6px;
  font-weight: 700;
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.7;
}

.message-time {
  margin-top: 8px;
  color: var(--app-text-muted);
  font-size: 12px;
}

@media (max-width: 1080px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}
</style>
