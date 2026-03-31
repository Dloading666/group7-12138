<template>
  <div class="workflow-page">
    <section class="hero surface-panel">
      <div class="hero-copy">
        <div class="hero-kicker">流程编排</div>
        <h1>流程列表</h1>
        <p>基于当前任务与机器人数据生成可编辑的流程卡片，支持本地持久化与后续接口接入。</p>
      </div>
      <div class="hero-actions">
        <el-tag type="info" effect="dark">本地持久化原型</el-tag>
        <el-button type="primary" :icon="Plus" @click="createDraft">新建流程</el-button>
      </div>
    </section>

    <section class="stats-grid">
      <StatCard :icon="Operation" :value="formatNumber(summary.total)" label="流程总数" color="#3b82f6" />
      <StatCard :icon="SwitchButton" :value="formatNumber(summary.active)" label="已启用" color="#22c55e" />
      <StatCard :icon="Clock" :value="formatNumber(summary.scheduleCount)" label="定时流程" color="#f59e0b" />
      <StatCard :icon="TrendCharts" :value="`${summary.averageNodes} 节点/条`" label="平均节点数" color="#7c3aed" />
    </section>

    <section class="content-grid">
      <div class="surface-panel main-panel">
        <div class="panel-head">
          <div>
            <h2>流程目录</h2>
            <p>点进任意流程可进入设计器，数据会自动同步到浏览器本地存储。</p>
          </div>
          <el-space wrap>
            <el-select v-model="filters.status" clearable placeholder="状态" style="width: 130px">
              <el-option label="草稿" value="draft" />
              <el-option label="已启用" value="active" />
              <el-option label="已暂停" value="paused" />
            </el-select>
            <el-select v-model="filters.triggerType" clearable placeholder="触发方式" style="width: 130px">
              <el-option label="手动触发" value="manual" />
              <el-option label="定时触发" value="schedule" />
              <el-option label="事件触发" value="event" />
            </el-select>
            <el-input v-model="filters.keyword" clearable placeholder="名称、编码、分类、标签" style="width: 220px" />
            <el-button @click="resetFilters">重置</el-button>
          </el-space>
        </div>

        <el-table :data="filteredWorkflows" :loading="loading" height="520" @row-dblclick="openDesigner">
          <el-table-column prop="name" label="流程名称" min-width="180" />
          <el-table-column prop="workflowId" label="流程编号" width="140" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)">{{ formatWorkflowStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="triggerType" label="触发方式" width="120">
            <template #default="{ row }">{{ formatWorkflowTrigger(row.triggerType) }}</template>
          </el-table-column>
          <el-table-column prop="nodes" label="节点数" width="90">
            <template #default="{ row }">{{ row.nodes.length }}</template>
          </el-table-column>
          <el-table-column prop="successRate" label="成功率" width="100">
            <template #default="{ row }">{{ row.successRate ? `${row.successRate}%` : '-' }}</template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="最近更新" width="180" />
          <el-table-column fixed="right" label="操作" width="260">
            <template #default="{ row }">
              <el-space wrap>
                <el-button link type="primary" @click="openDesigner(row)">设计</el-button>
                <el-button link @click="cloneWorkflow(row)">复制</el-button>
                <el-button link @click="toggleStatus(row)">{{ row.status === 'active' ? '暂停' : '启用' }}</el-button>
                <el-button link type="danger" @click="removeWorkflow(row)">删除</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <aside class="side-panel">
        <section class="surface-panel side-card">
          <div class="panel-head compact">
            <div>
              <h2>任务与机器人概览</h2>
              <p>用现有任务、机器人数据补齐流程编排入口。</p>
            </div>
          </div>
          <div class="insight-list">
            <div class="insight-item">
              <span>任务总数</span>
              <strong>{{ formatNumber(taskCount) }}</strong>
            </div>
            <div class="insight-item">
              <span>运行中任务</span>
              <strong>{{ formatNumber(runningTasks) }}</strong>
            </div>
            <div class="insight-item">
              <span>机器人数量</span>
              <strong>{{ formatNumber(robotCount) }}</strong>
            </div>
            <div class="insight-item">
              <span>在线机器人</span>
              <strong>{{ formatNumber(onlineRobots) }}</strong>
            </div>
          </div>
        </section>

        <section class="surface-panel side-card">
          <div class="panel-head compact">
            <div>
              <h2>快速模板</h2>
              <p>一键生成常见流程骨架。</p>
            </div>
          </div>
          <div class="template-grid">
            <el-button plain class="template-btn" @click="createTemplate('schedule')">定时巡检</el-button>
            <el-button plain class="template-btn" @click="createTemplate('event')">事件同步</el-button>
            <el-button plain class="template-btn" @click="createTemplate('manual')">人工审批</el-button>
          </div>
        </section>

        <section class="surface-panel side-card">
          <div class="panel-head compact">
            <div>
              <h2>最近任务</h2>
              <p>这些数据来自当前任务列表，可作为流程参考。</p>
            </div>
          </div>
          <el-timeline class="task-timeline">
            <el-timeline-item v-for="task in recentTasks" :key="task.id" :timestamp="task.createTime">
              <div class="timeline-item">
                <div class="timeline-title">{{ task.name }}</div>
                <div class="timeline-meta">{{ task.type }} · {{ task.robotName || '未分配机器人' }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, Operation, Plus, SwitchButton, TrendCharts } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { getRobots } from '@/api/robots'
import { getTasks } from '@/api/tasks'
import { cloneWorkflowById, deleteWorkflowDefinition, getWorkflowCatalog, saveWorkflowDefinition } from '@/api/workflow'
import { demoRobots, demoTasks } from '@/mock/demo-data'
import { buildWorkflowSummary, createWorkflowDraft, formatWorkflowStatus, formatWorkflowTrigger, loadWorkflowCatalog, type WorkflowDefinition } from '@/utils/workflow'
import { formatNumber } from '@/utils/format'
import type { RobotItem, TaskItem } from '@/types/domain'

const router = useRouter()
const loading = ref(false)
const workflows = ref<WorkflowDefinition[]>(loadWorkflowCatalog())
const tasks = ref<TaskItem[]>(demoTasks)
const robots = ref<RobotItem[]>(demoRobots)
const filters = reactive({
  keyword: '',
  status: '',
  triggerType: ''
})

const summary = computed(() => buildWorkflowSummary(workflows.value))
const filteredWorkflows = computed(() =>
  workflows.value.filter((workflow) => {
    const keyword = filters.keyword.trim().toLowerCase()
    const haystack = [workflow.name, workflow.workflowId, workflow.code, workflow.category, workflow.tags.join(' ')].join(' ').toLowerCase()
    const matchKeyword = !keyword || haystack.includes(keyword)
    const matchStatus = !filters.status || workflow.status === filters.status
    const matchTrigger = !filters.triggerType || workflow.triggerType === filters.triggerType
    return matchKeyword && matchStatus && matchTrigger
  })
)

const taskCount = computed(() => tasks.value.length)
const runningTasks = computed(() => tasks.value.filter((item) => item.status === 'running').length)
const robotCount = computed(() => robots.value.length)
const onlineRobots = computed(() => robots.value.filter((item) => item.status === 'online').length)
const recentTasks = computed(() => tasks.value.slice(0, 4))

function statusTag(status: WorkflowDefinition['status']) {
  return { draft: 'info', active: 'success', paused: 'warning' }[status]
}

async function loadData() {
  loading.value = true
  try {
    const [workflowRes, taskRes, robotRes] = await Promise.all([getWorkflowCatalog(), getTasks(), getRobots()])
    workflows.value = workflowRes.data?.length ? workflowRes.data : loadWorkflowCatalog()
    tasks.value = taskRes.data?.list?.length ? taskRes.data.list : demoTasks
    robots.value = robotRes.data?.list?.length ? robotRes.data.list : demoRobots
  } catch {
    workflows.value = loadWorkflowCatalog()
    tasks.value = demoTasks
    robots.value = demoRobots
  } finally {
    loading.value = false
  }
}

function openDesigner(row: WorkflowDefinition) {
  router.push({ path: '/workflow/design', query: { workflowId: String(row.id) } })
}

async function createDraft() {
  const draft = createWorkflowDraft()
  const res = await saveWorkflowDefinition(draft)
  const saved = res.data || draft
  ElMessage.success('已创建流程草稿')
  await loadData()
  await router.push({ path: '/workflow/design', query: { workflowId: String(saved.id) } })
}

async function createTemplate(triggerType: 'manual' | 'schedule' | 'event') {
  const draft = createWorkflowDraft({
    triggerType,
    name: triggerType === 'schedule' ? '定时巡检流程' : triggerType === 'event' ? '事件同步流程' : '人工审批流程',
    description: '基于当前任务与机器人概览生成的流程模板，可直接进入设计器继续编辑。',
    category: triggerType === 'schedule' ? '运维' : triggerType === 'event' ? '业务' : '审批',
    tags: [triggerType === 'schedule' ? '巡检' : triggerType === 'event' ? '同步' : '审批', '模板']
  })
  const res = await saveWorkflowDefinition(draft)
  const saved = res.data || draft
  ElMessage.success('模板流程已生成')
  await loadData()
  await router.push({ path: '/workflow/design', query: { workflowId: String(saved.id) } })
}

async function cloneWorkflow(row: WorkflowDefinition) {
  await cloneWorkflowById(row.id)
  ElMessage.success('已复制流程')
  await loadData()
}

async function toggleStatus(row: WorkflowDefinition) {
  const nextStatus = row.status === 'active' ? 'paused' : 'active'
  await saveWorkflowDefinition({ ...row, status: nextStatus, updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }) })
  ElMessage.success(nextStatus === 'active' ? '流程已启用' : '流程已暂停')
  await loadData()
}

async function removeWorkflow(row: WorkflowDefinition) {
  await ElMessageBox.confirm(`确认删除流程「${row.name}」吗？`, '删除流程', { type: 'warning' })
  await deleteWorkflowDefinition(row.id)
  ElMessage.success('流程已删除')
  await loadData()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.triggerType = ''
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped lang="scss">
.workflow-page {
  display: grid;
  gap: 16px;
}

.hero,
.main-panel,
.side-card {
  padding: 20px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hero-kicker {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--app-text-muted);
}

.hero h1,
.hero p {
  margin: 0;
}

.hero p {
  margin-top: 8px;
  color: var(--app-text-muted);
  max-width: 680px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head.compact {
  margin-bottom: 16px;
}

.panel-head h2,
.panel-head p {
  margin: 0;
}

.panel-head p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.side-panel {
  display: grid;
  gap: 16px;
}

.insight-list {
  display: grid;
  gap: 12px;
}

.insight-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(59, 130, 246, 0.05);
}

.insight-item span {
  color: var(--app-text-muted);
}

.insight-item strong {
  font-size: 18px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.template-btn {
  min-height: 48px;
}

.task-timeline {
  padding-left: 2px;
}

.timeline-item {
  padding-bottom: 4px;
}

.timeline-title {
  font-weight: 700;
}

.timeline-meta {
  margin-top: 4px;
  color: var(--app-text-muted);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .stats-grid,
  .content-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .hero,
  .stats-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
