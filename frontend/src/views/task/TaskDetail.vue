<template>
  <div class="task-detail app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>任务详情</h2>
        <p>查看任务状态、采集结果以及 AI 分析来源信息。</p>
      </div>

      <div class="page-header-actions">
        <el-tag v-if="task" :type="statusTypeMap[task.status] || 'info'">
          {{ statusTextMap[task.status] || task.status }}
        </el-tag>
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <div class="page-section padded" v-loading="loading">
      <template v-if="task">
        <div class="status-grid detail-metrics">
          <div class="status-metric">
            <div class="metric-label">任务编号</div>
            <div class="metric-value">{{ task.taskId }}</div>
          </div>
          <div class="status-metric">
            <div class="metric-label">任务类型</div>
            <div class="metric-value">{{ getTypeText(task.type) }}</div>
          </div>
          <div class="status-metric">
            <div class="metric-label">执行机器人</div>
            <div class="metric-value">{{ task.robotName || '-' }}</div>
          </div>
          <div class="status-metric">
            <div class="metric-label">执行耗时</div>
            <div class="metric-value">{{ formatDuration(task.duration) }}</div>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="任务名称">{{ task.name }}</el-descriptions-item>
          <el-descriptions-item label="优先级">{{ task.priority || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(task.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDateTime(task.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ formatDateTime(task.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTypeMap[task.status] || 'info'">
              {{ statusTextMap[task.status] || task.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务描述" :span="2">{{ task.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执行结果" :span="2">
            <pre class="detail-pre">{{ task.result || '-' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item v-if="task.errorMessage" label="错误信息" :span="2">
            <span class="error-text">{{ task.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </div>

    <div v-if="task?.crawlUrl" class="page-section padded">
      <div class="section-heading">
        <div>
          <h3>采集配置</h3>
          <p>当前任务会在执行时把页面内容抓取并回传到采集结果中。</p>
        </div>

        <div class="page-actions">
          <el-button
            v-if="task.status === 'completed' && crawlResult"
            type="primary"
            @click="openAiAnalysis(task.id, task.taskId)"
          >
            发起 AI 分析
          </el-button>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="目标 URL" :span="2">{{ task.crawlUrl }}</el-descriptions-item>
        <el-descriptions-item label="超时">{{ task.crawlTimeout || '-' }} ms</el-descriptions-item>
        <el-descriptions-item label="分页抓取">{{ task.hasPagination ? '已开启' : '未开启' }}</el-descriptions-item>
        <el-descriptions-item label="请求头">{{ task.hasHeaders ? '已配置' : '默认' }}</el-descriptions-item>
        <el-descriptions-item label="Cookie">{{ task.hasCookies ? '已配置' : '未配置' }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div v-if="task?.crawlUrl && !crawlResult" class="page-section padded">
      <div class="result-state-banner">
        <strong>{{ crawlStateTitle }}</strong>
        <div>{{ crawlStateDescription }}</div>
        <div v-if="resultLoading || retryingResult" class="result-meta">
          系统正在自动刷新采集结果，不需要重复触发。
        </div>
      </div>
    </div>

    <div v-if="crawlResult" class="page-section padded">
      <div class="section-heading">
        <div>
          <h3>采集结果</h3>
          <p>这里展示 Spider 回调后的采集产出，可以直接进入 AI 分析。</p>
        </div>

        <div class="page-actions">
          <el-button type="primary" @click="openAiAnalysis(task.id, task.taskId)">发起 AI 分析</el-button>
          <el-button @click="openResultPage">查看结果页</el-button>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="最终 URL" :span="2">{{ crawlResult.finalUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="页面标题" :span="2">{{ crawlResult.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结果条数">{{ crawlResult.totalCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="抓取页数">{{ crawlResult.crawledPages || 0 }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          <pre class="detail-pre">{{ crawlResult.summaryText || '-' }}</pre>
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs class="result-tabs">
        <el-tab-pane label="结构化结果">
          <el-empty
            v-if="!crawlResult.structuredData || crawlResult.structuredData.length === 0"
            description="当前任务没有结构化抽取结果"
          />
          <el-table v-else :data="crawlResult.structuredData" border stripe max-height="360">
            <el-table-column
              v-for="(value, key) in crawlResult.structuredData[0]"
              :key="key"
              :prop="key"
              :label="key"
              min-width="160"
              show-overflow-tooltip
            />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="原始 HTML">
          <pre class="detail-pre html-block">{{ crawlResult.rawHtml || '-' }}</pre>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div v-if="task?.type === 'ai_workflow'" class="page-section padded">
      <div class="section-heading">
        <div>
          <h3>AI 分析来源</h3>
          <p>AI 分析任务固定分析已采集内容，并支持后续继续问答。</p>
        </div>

        <div class="page-actions">
          <el-button type="primary" @click="openAiWorkbench">打开 AI 分析工作台</el-button>
        </div>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="来源任务">{{ task.sourceTaskId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源标题">{{ task.sourceTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源 URL" :span="2">{{ task.sourceFinalUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分析流程">{{ task.workflowName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="初始问题">{{ task.query || '默认分析提示' }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCrawlResultDetail } from '../../api/crawl.js'
import { getTaskDetail } from '../../api/task.js'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const resultLoading = ref(false)
const retryingResult = ref(false)
const task = ref(null)
const crawlResult = ref(null)
const retryTimer = ref(null)
const retryCount = ref(0)

const maxRetryCount = 24

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

const crawlStateTitle = computed(() => {
  if (!task.value?.crawlUrl) return '暂无采集结果'
  if (task.value.status === 'failed') return '任务执行失败'
  if (task.value.status === 'completed') return '采集结果正在同步'
  return '正在等待采集结果'
})

const crawlStateDescription = computed(() => {
  if (!task.value?.crawlUrl) return '当前任务不包含采集结果。'
  if (task.value.status === 'failed') {
    return task.value.errorMessage || '任务执行失败，因此没有生成采集结果。'
  }
  if (task.value.status === 'completed') {
    return '任务已经完成，但采集结果还在同步入库，页面会自动重试。'
  }
  return '任务已创建成功，正在等待 Spider 回调采集结果，请稍候。'
})

const clearRetryTimer = () => {
  if (retryTimer.value) {
    clearTimeout(retryTimer.value)
    retryTimer.value = null
  }
  retryingResult.value = false
}

const canRetryResult = () => {
  if (!task.value?.crawlUrl) return false
  if (crawlResult.value || task.value.status === 'failed') return false
  return retryCount.value < maxRetryCount
}

const loadCrawlResult = async () => {
  if (!task.value?.taskId) return false

  resultLoading.value = true
  try {
    const res = await getCrawlResultDetail(task.value.taskId, { silent: true })
    crawlResult.value = res.data
    clearRetryTimer()
    return true
  } catch {
    crawlResult.value = null
    return false
  } finally {
    resultLoading.value = false
  }
}

const scheduleResultRetry = () => {
  clearRetryTimer()
  if (!canRetryResult()) return

  retryingResult.value = true
  retryTimer.value = window.setTimeout(async () => {
    retryCount.value += 1
    await loadTask(true)
  }, 2500)
}

const loadTask = async (silent = false) => {
  if (!silent) {
    loading.value = true
  }

  try {
    const res = await getTaskDetail(route.params.id, { silent: true })
    task.value = res.data

    if (!task.value?.crawlUrl) {
      crawlResult.value = null
      clearRetryTimer()
      return
    }

    const loaded = await loadCrawlResult()
    if (!loaded) {
      scheduleResultRetry()
    }
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

const getTypeText = (type) => {
  const typeMap = {
    'data-collection': '数据采集',
    ai_workflow: 'AI分析',
    workflow: 'AI分析',
    report: '报表生成',
    'data-sync': '数据同步',
    'web-crawl': '网站抓取'
  }
  return typeMap[type] || type || '-'
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

const openAiAnalysis = (sourceTaskRecordId, sourceTaskId) => {
  router.push(`/task/ai?sourceTaskRecordId=${sourceTaskRecordId}&sourceTaskId=${sourceTaskId || ''}`)
}

const openAiWorkbench = () => {
  router.push('/task/ai')
}

const openResultPage = () => {
  router.push('/statistics/query')
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadTask()
})

onBeforeUnmount(() => {
  clearRetryTimer()
})
</script>

<style scoped lang="scss">
.task-detail {
  padding: 4px;
}

.detail-metrics {
  margin-bottom: 16px;
}

.detail-descriptions {
  margin-top: 4px;
}

.result-tabs {
  margin-top: 16px;
}

.result-meta {
  margin-top: 8px;
}

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

.error-text {
  color: var(--app-danger);
}
</style>
