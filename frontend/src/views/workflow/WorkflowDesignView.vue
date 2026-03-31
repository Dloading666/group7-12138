<template>
  <div class="workflow-page">
    <section class="hero surface-panel">
      <div class="hero-copy">
        <div class="hero-kicker">流程编排</div>
        <h1>流程设计器</h1>
        <p>这是一个可落地的设计器原型：能编辑流程元数据、增删节点、保存到本地，并预留后端同步入口。</p>
      </div>
      <div class="hero-actions">
        <el-tag type="success" effect="dark">本地持久化</el-tag>
        <el-button @click="createDraft">新建流程</el-button>
        <el-button type="primary" :loading="saving" @click="saveDraft">保存</el-button>
        <el-button type="warning" :disabled="!draft.id" @click="publishDraft">发布</el-button>
      </div>
    </section>

    <section class="toolbar surface-panel">
      <div class="toolbar-left">
        <el-select v-model="currentWorkflowId" placeholder="选择流程" style="width: 280px" @change="switchWorkflow">
          <el-option v-for="item in workflowOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button plain :icon="CopyDocument" :disabled="!draft.id" @click="cloneCurrentWorkflow">复制流程</el-button>
        <el-button plain :icon="RefreshRight" @click="normalizeCurrentWorkflow">整理节点</el-button>
      </div>
      <div class="toolbar-right">
        <el-tag effect="plain">节点 {{ draft.nodes.length }}</el-tag>
        <el-tag effect="plain">版本 v{{ draft.version }}</el-tag>
        <el-tag :type="statusTag(draft.status)">{{ formatWorkflowStatus(draft.status) }}</el-tag>
        <el-tag type="info">{{ formatWorkflowTrigger(draft.triggerType) }}</el-tag>
      </div>
    </section>

    <section class="editor-grid">
      <aside class="surface-panel rail-panel">
        <div class="panel-head compact">
          <div>
            <h2>节点库</h2>
            <p>点击即可把节点插入到当前选中节点后方。</p>
          </div>
        </div>

        <div class="palette">
          <button
            v-for="item in nodePalette"
            :key="item.type"
            class="palette-card"
            :style="{ '--accent': item.color }"
            @click="addNode(item.type)"
          >
            <span class="palette-name">{{ item.label }}</span>
            <span class="palette-hint">{{ item.hint }}</span>
          </button>
        </div>

        <div class="quick-create">
          <div class="panel-head compact">
            <div>
              <h2>快速起稿</h2>
              <p>用当前任务与机器人数据直接生成流程模板。</p>
            </div>
          </div>
          <el-space wrap>
            <el-button plain @click="createTemplate('schedule')">定时巡检</el-button>
            <el-button plain @click="createTemplate('event')">事件同步</el-button>
            <el-button plain @click="createTemplate('manual')">人工审批</el-button>
          </el-space>
        </div>

        <div class="rail-summary">
          <div class="summary-row"><span>任务总数</span><strong>{{ taskCount }}</strong></div>
          <div class="summary-row"><span>运行中任务</span><strong>{{ runningTasks }}</strong></div>
          <div class="summary-row"><span>机器人数量</span><strong>{{ robotCount }}</strong></div>
          <div class="summary-row"><span>在线机器人</span><strong>{{ onlineRobots }}</strong></div>
        </div>
      </aside>

      <main class="surface-panel canvas-panel">
        <div class="panel-head">
          <div>
            <h2>{{ draft.name || '未命名流程' }}</h2>
            <p>{{ draft.description }}</p>
          </div>
          <div class="canvas-actions">
            <el-button link type="primary" :icon="Plus" @click="addNode('task')">任务节点</el-button>
            <el-button link type="primary" @click="addNode('robot')">机器人节点</el-button>
            <el-button link type="primary" @click="addNode('condition')">条件分支</el-button>
          </div>
        </div>

        <el-alert
          title="设计器说明"
          type="info"
          :closable="false"
          show-icon
          description="当前版本使用浏览器本地存储作为持久化层；如果后端工作流接口上线，只需要替换 api/workflow.ts 即可。"
        />

        <div class="workflow-canvas">
          <div class="canvas-track">
            <template v-for="(node, index) in draft.nodes" :key="node.id">
              <div
                class="workflow-node"
                :class="{ selected: node.id === selectedNodeId, terminal: node.type === 'start' || node.type === 'end' }"
                @click="selectNode(node.id)"
              >
                <div class="node-top">
                  <el-tag size="small" :type="nodeTypeTag(node.type)">{{ formatWorkflowNodeType(node.type) }}</el-tag>
                  <span class="node-index">#{{ index + 1 }}</span>
                </div>
                <div class="node-name">{{ node.name }}</div>
                <div class="node-meta">
                  <span v-if="node.taskType">{{ node.taskType }}</span>
                  <span v-else-if="node.robotName">{{ node.robotName }}</span>
                  <span v-else>{{ node.note || '等待配置' }}</span>
                </div>
                <div class="node-actions">
                  <el-button link size="small" @click.stop="moveNode(node.id, 'left')">左移</el-button>
                  <el-button link size="small" @click.stop="moveNode(node.id, 'right')">右移</el-button>
                  <el-button link size="small" type="danger" @click.stop="removeNode(node.id)">删除</el-button>
                </div>
              </div>
              <div v-if="index < draft.nodes.length - 1" class="node-link">
                <div class="link-line"></div>
                <el-icon class="link-icon"><ArrowRight /></el-icon>
              </div>
            </template>
          </div>
        </div>
      </main>

      <aside class="surface-panel property-panel">
        <div class="panel-head compact">
          <div>
            <h2>属性面板</h2>
            <p>编辑当前流程和选中节点的详细配置。</p>
          </div>
        </div>

        <el-scrollbar height="760px">
          <div class="property-section">
            <h3>流程信息</h3>
            <el-form label-width="88px" class="property-form">
              <el-form-item label="名称"><el-input v-model="draft.name" /></el-form-item>
              <el-form-item label="编码"><el-input v-model="draft.code" /></el-form-item>
              <el-form-item label="分类"><el-input v-model="draft.category" /></el-form-item>
              <el-form-item label="状态">
                <el-select v-model="draft.status" style="width: 100%">
                  <el-option label="草稿" value="draft" />
                  <el-option label="已启用" value="active" />
                  <el-option label="已暂停" value="paused" />
                </el-select>
              </el-form-item>
              <el-form-item label="触发方式">
                <el-select v-model="draft.triggerType" style="width: 100%">
                  <el-option label="手动触发" value="manual" />
                  <el-option label="定时触发" value="schedule" />
                  <el-option label="事件触发" value="event" />
                </el-select>
              </el-form-item>
              <el-form-item label="计划">
                <el-input v-model="draft.cron" placeholder="如 0 9 * * *" />
              </el-form-item>
              <el-form-item label="负责人"><el-input v-model="draft.owner" /></el-form-item>
              <el-form-item label="标签">
                <el-select v-model="tagDraft" multiple filterable allow-create default-first-option style="width: 100%" placeholder="输入并回车添加标签">
                  <el-option v-for="item in draft.tags" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="说明"><el-input v-model="draft.description" type="textarea" :rows="4" /></el-form-item>
            </el-form>
          </div>

          <div class="property-section">
            <h3>节点信息</h3>
            <template v-if="selectedNode">
              <el-form label-width="88px" class="property-form">
                <el-form-item label="节点名称"><el-input v-model="selectedNode.name" /></el-form-item>
                <el-form-item label="节点类型">
                  <el-select v-model="selectedNode.type" style="width: 100%" @change="onNodeTypeChange(selectedNode)">
                    <el-option v-for="item in editableNodeTypes" :key="item.type" :label="item.label" :value="item.type" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="selectedNode.type === 'task'" label="任务类型"><el-input v-model="selectedNode.taskType" placeholder="例如 数据同步" /></el-form-item>
                <el-form-item v-if="selectedNode.type === 'robot'" label="机器人">
                  <el-select v-model="selectedNode.robotId" style="width: 100%" placeholder="选择机器人" @change="syncRobotName">
                    <el-option v-for="robot in robots" :key="robot.id" :label="`${robot.name} · ${robot.ip || '无 IP'}`" :value="robot.id" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="selectedNode.type === 'condition'" label="条件"><el-input v-model="selectedNode.condition" placeholder="如 成功 / 失败分流" /></el-form-item>
                <el-form-item v-if="selectedNode.type === 'delay'" label="等待分钟"><el-input-number v-model="selectedNode.durationMinutes" :min="1" :max="720" style="width: 100%" /></el-form-item>
                <el-form-item label="超时(分)"><el-input-number v-model="selectedNode.timeoutMinutes" :min="1" :max="720" style="width: 100%" /></el-form-item>
                <el-form-item label="重试次数"><el-input-number v-model="selectedNode.retryCount" :min="0" :max="10" style="width: 100%" /></el-form-item>
                <el-form-item label="备注"><el-input v-model="selectedNode.note" type="textarea" :rows="3" /></el-form-item>
              </el-form>
              <div class="node-actions-panel">
                <el-button plain @click="duplicateSelectedNode">复制当前节点</el-button>
                <el-button plain @click="normalizeCurrentWorkflow">重新整理链路</el-button>
              </div>
            </template>
            <el-empty v-else description="请选择一个节点开始编辑" />
          </div>

          <div class="property-section">
            <h3>保存提示</h3>
            <div class="preview-card">
              <div>当前节点数：{{ draft.nodes.length }}</div>
              <div>预计执行目标：{{ nodePreview }}</div>
              <div>最后保存：{{ draft.updatedAt }}</div>
            </div>
          </div>
        </el-scrollbar>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, CopyDocument, Plus, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getRobots } from '@/api/robots'
import { getTasks } from '@/api/tasks'
import { cloneWorkflowById, getWorkflowCatalog, saveWorkflowDefinition } from '@/api/workflow'
import { demoRobots, demoTasks } from '@/mock/demo-data'
import {
  availableWorkflowNodeTypes,
  createNodeByType,
  createWorkflowDraft,
  formatWorkflowNodeType,
  formatWorkflowStatus,
  formatWorkflowTrigger,
  insertWorkflowNode,
  loadWorkflowCatalog,
  moveWorkflowNode,
  relinkWorkflowNodes,
  removeWorkflowNode,
  type WorkflowDefinition,
  type WorkflowNode,
  type WorkflowNodeType
} from '@/utils/workflow'
import type { RobotItem, TaskItem } from '@/types/domain'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const workflowCatalog = ref<WorkflowDefinition[]>(loadWorkflowCatalog())
const tasks = ref<TaskItem[]>(demoTasks)
const robots = ref<RobotItem[]>(demoRobots)
const draft = ref<WorkflowDefinition>(createWorkflowDraft())
const selectedNodeId = ref('')
const currentWorkflowId = ref<number | string>('')
const tagDraft = ref<string[]>([])

const nodePalette = availableWorkflowNodeTypes().filter((item) => item.type !== 'start' && item.type !== 'end')
const editableNodeTypes = availableWorkflowNodeTypes().filter((item) => item.type !== 'start' && item.type !== 'end')

const workflowOptions = computed(() =>
  workflowCatalog.value.map((item) => ({
    label: `${item.name} · ${formatWorkflowStatus(item.status)}`,
    value: item.id
  }))
)

const selectedNode = computed<WorkflowNode | undefined>(() => draft.value.nodes.find((node) => node.id === selectedNodeId.value))
const taskCount = computed(() => tasks.value.length)
const runningTasks = computed(() => tasks.value.filter((item) => item.status === 'running').length)
const robotCount = computed(() => robots.value.length)
const onlineRobots = computed(() => robots.value.filter((item) => item.status === 'online').length)
const nodePreview = computed(() => {
  const robotNode = draft.value.nodes.find((node) => node.robotName)
  if (robotNode?.robotName) return robotNode.robotName
  const taskNode = draft.value.nodes.find((node) => node.taskType)
  if (taskNode?.taskType) return taskNode.taskType
  return '流程骨架'
})

function cloneWorkflow(item: WorkflowDefinition) {
  return JSON.parse(JSON.stringify(item)) as WorkflowDefinition
}

function nodeTypeTag(type: WorkflowNodeType) {
  return { start: 'info', task: 'success', robot: 'warning', condition: 'primary', delay: 'danger', end: 'info' }[type]
}

function statusTag(status: WorkflowDefinition['status']) {
  return { draft: 'info', active: 'success', paused: 'warning' }[status]
}

function syncDraft(next: WorkflowDefinition, replaceSelection = true) {
  draft.value = cloneWorkflow(next)
  tagDraft.value = [...draft.value.tags]
  if (replaceSelection) {
    selectedNodeId.value = draft.value.nodes[1]?.id || draft.value.nodes[0]?.id || ''
  }
}

function resolveWorkflowId(value: unknown) {
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : null
}

function selectWorkflowById(id: number | null) {
  if (!id) {
    const first = workflowCatalog.value[0] || createWorkflowDraft()
    syncDraft(first)
    currentWorkflowId.value = first.id
    return
  }
  const found = workflowCatalog.value.find((item) => item.id === id)
  if (found) {
    syncDraft(found)
    currentWorkflowId.value = found.id
    return
  }
  const first = workflowCatalog.value[0]
  if (first) {
    syncDraft(first)
    currentWorkflowId.value = first.id
  }
}

async function loadData() {
  loading.value = true
  try {
    const [workflowRes, taskRes, robotRes] = await Promise.all([getWorkflowCatalog(), getTasks(), getRobots()])
    workflowCatalog.value = workflowRes.data?.length ? workflowRes.data : loadWorkflowCatalog()
    tasks.value = taskRes.data?.list?.length ? taskRes.data.list : demoTasks
    robots.value = robotRes.data?.list?.length ? robotRes.data.list : demoRobots
  } catch {
    workflowCatalog.value = loadWorkflowCatalog()
    tasks.value = demoTasks
    robots.value = demoRobots
  } finally {
    loading.value = false
    const routeWorkflowId = resolveWorkflowId(route.query.workflowId)
    selectWorkflowById(routeWorkflowId || workflowCatalog.value[0]?.id || null)
  }
}

function switchWorkflow(value: number | string) {
  const nextId = resolveWorkflowId(value)
  if (!nextId) return
  const found = workflowCatalog.value.find((item) => item.id === nextId)
  if (!found) return
  syncDraft(found)
  void router.replace({ path: '/workflow/design', query: { workflowId: String(found.id) } })
}

function selectNode(id: string) {
  selectedNodeId.value = id
}

function addNode(type: WorkflowNodeType) {
  const node = createNodeByType(type, robots.value[0], tasks.value[0])
  const currentIndex = draft.value.nodes.findIndex((item) => item.id === selectedNodeId.value)
  const insertIndex = currentIndex >= 0 ? currentIndex + 1 : Math.max(1, draft.value.nodes.length - 1)
  draft.value.nodes = insertWorkflowNode(draft.value.nodes, insertIndex, node)
  selectedNodeId.value = node.id
}

function duplicateSelectedNode() {
  const node = selectedNode.value
  if (!node) return
  const clone = JSON.parse(JSON.stringify(node)) as WorkflowNode
  clone.id = `${node.id}-copy-${Date.now()}`
  draft.value.nodes = insertWorkflowNode(draft.value.nodes, draft.value.nodes.findIndex((item) => item.id === node.id) + 1, clone)
  selectedNodeId.value = clone.id
}

function moveNode(nodeId: string, direction: 'left' | 'right') {
  draft.value.nodes = moveWorkflowNode(draft.value.nodes, nodeId, direction)
}

function removeNode(nodeId: string) {
  const node = draft.value.nodes.find((item) => item.id === nodeId)
  if (!node || node.type === 'start' || node.type === 'end') {
    ElMessage.warning('开始节点和结束节点不能删除')
    return
  }
  if (draft.value.nodes.length <= 3) {
    ElMessage.warning('至少保留开始、处理、结束三类节点')
    return
  }
  draft.value.nodes = removeWorkflowNode(draft.value.nodes, nodeId)
  selectedNodeId.value = draft.value.nodes[1]?.id || draft.value.nodes[0]?.id || ''
}

function onNodeTypeChange(node: WorkflowNode) {
  if (node.type === 'task' && !node.taskType) {
    node.taskType = tasks.value[0]?.type || '数据处理'
  }
  if (node.type === 'robot' && !node.robotId) {
    node.robotId = robots.value[0]?.id
    node.robotName = robots.value[0]?.name || ''
  }
  if (node.type === 'delay' && !node.durationMinutes) {
    node.durationMinutes = 10
  }
}

function syncRobotName() {
  if (!selectedNode.value || selectedNode.value.type !== 'robot') return
  selectedNode.value.robotName = robots.value.find((item) => item.id === selectedNode.value?.robotId)?.name || ''
}

function normalizeCurrentWorkflow() {
  draft.value.nodes = relinkWorkflowNodes(draft.value.nodes)
  ElMessage.success('节点链路已整理')
}

async function saveDraft() {
  saving.value = true
  try {
    draft.value.tags = [...tagDraft.value].filter(Boolean)
    draft.value.updatedAt = new Date().toLocaleString('zh-CN', { hour12: false })
    const res = await saveWorkflowDefinition({
      ...draft.value,
      nodes: relinkWorkflowNodes(draft.value.nodes)
    })
    if (res.code === 200 && res.data) {
      workflowCatalog.value = workflowCatalog.value.filter((item) => item.id !== res.data.id)
      workflowCatalog.value.unshift(res.data)
      syncDraft(res.data, false)
      currentWorkflowId.value = res.data.id
      await router.replace({ path: '/workflow/design', query: { workflowId: String(res.data.id) } })
      ElMessage.success('流程已保存')
    }
  } finally {
    saving.value = false
  }
}

async function publishDraft() {
  draft.value.status = 'active'
  draft.value.version += 1
  await saveDraft()
}

async function createDraft() {
  const next = createWorkflowDraft(
    {
      name: '新流程',
      description: '从这里开始编排新的自动化流程。',
      tags: ['新建']
    },
    workflowCatalog.value
  )
  const res = await saveWorkflowDefinition(next)
  const saved = res.data || next
  workflowCatalog.value.unshift(saved)
  syncDraft(saved)
  currentWorkflowId.value = saved.id
  await router.replace({ path: '/workflow/design', query: { workflowId: String(saved.id) } })
  ElMessage.success('已创建流程草稿')
}

async function createTemplate(triggerType: 'manual' | 'schedule' | 'event') {
  const template = createWorkflowDraft(
    {
      triggerType,
      name: triggerType === 'schedule' ? '定时巡检流程' : triggerType === 'event' ? '事件同步流程' : '人工审批流程',
      description: '由当前任务与机器人概览生成的模板，可直接进入设计器继续编辑。',
      category: triggerType === 'schedule' ? '运维' : triggerType === 'event' ? '业务' : '审批',
      tags: [triggerType === 'schedule' ? '巡检' : triggerType === 'event' ? '同步' : '审批', '模板']
    },
    workflowCatalog.value
  )
  const res = await saveWorkflowDefinition(template)
  const saved = res.data || template
  workflowCatalog.value.unshift(saved)
  syncDraft(saved)
  currentWorkflowId.value = saved.id
  await router.replace({ path: '/workflow/design', query: { workflowId: String(saved.id) } })
  ElMessage.success('模板已生成')
}

async function cloneCurrentWorkflow() {
  if (!draft.value.id) return
  await cloneWorkflowById(draft.value.id)
  await loadData()
  const cloned = workflowCatalog.value[0]
  if (cloned) {
    syncDraft(cloned)
    currentWorkflowId.value = cloned.id
    await router.replace({ path: '/workflow/design', query: { workflowId: String(cloned.id) } })
  }
  ElMessage.success('已复制流程')
}

watch(
  () => route.query.workflowId,
  (value) => {
    const id = resolveWorkflowId(value)
    if (!id) return
    const found = workflowCatalog.value.find((item) => item.id === id)
    if (found) {
      syncDraft(found)
      currentWorkflowId.value = found.id
    }
  }
)

watch(
  tagDraft,
  (value) => {
    draft.value.tags = [...value].filter(Boolean)
  },
  { deep: true }
)

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
.toolbar,
.rail-panel,
.canvas-panel,
.property-panel {
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
  max-width: 760px;
}

.hero-actions,
.toolbar,
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar {
  justify-content: space-between;
}

.editor-grid {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr) 360px;
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
.panel-head h3,
.panel-head p {
  margin: 0;
}

.panel-head p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.palette {
  display: grid;
  gap: 10px;
}

.palette-card {
  text-align: left;
  border: 1px solid rgba(219, 226, 239, 0.8);
  border-radius: 16px;
  padding: 14px 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.96));
  box-shadow: 0 10px 24px rgba(18, 32, 58, 0.06);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  display: grid;
  gap: 4px;
  position: relative;
}

.palette-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 16px 0 0 16px;
  background: var(--accent);
}

.palette-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 34px rgba(18, 32, 58, 0.12);
  border-color: rgba(59, 130, 246, 0.24);
}

.palette-name {
  font-weight: 700;
}

.palette-hint {
  color: var(--app-text-muted);
  font-size: 13px;
}

.quick-create {
  margin-top: 18px;
}

.rail-summary {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(59, 130, 246, 0.05);
}

.summary-row span {
  color: var(--app-text-muted);
}

.summary-row strong {
  font-size: 18px;
}

.canvas-panel {
  min-width: 0;
}

.workflow-canvas {
  margin-top: 16px;
  padding: 18px;
  border-radius: 20px;
  background:
    linear-gradient(rgba(59, 130, 246, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.04) 1px, transparent 1px);
  background-size: 36px 36px;
  border: 1px solid rgba(219, 226, 239, 0.8);
  overflow: hidden;
}

.canvas-track {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 420px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.workflow-node {
  width: 220px;
  flex: 0 0 auto;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.97);
  border: 1px solid rgba(219, 226, 239, 0.85);
  box-shadow: 0 14px 30px rgba(18, 32, 58, 0.08);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.workflow-node:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 38px rgba(18, 32, 58, 0.12);
}

.workflow-node.selected {
  border-color: rgba(59, 130, 246, 0.55);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12), 0 20px 38px rgba(18, 32, 58, 0.12);
}

.workflow-node.terminal {
  background: linear-gradient(180deg, rgba(255, 255, 255, 1), rgba(245, 247, 252, 0.98));
}

.node-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.node-index {
  font-size: 12px;
  color: var(--app-text-muted);
}

.node-name {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 800;
}

.node-meta {
  min-height: 38px;
  margin-top: 8px;
  color: var(--app-text-muted);
  line-height: 1.55;
}

.node-actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  gap: 6px;
}

.node-link {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  color: var(--app-text-muted);
}

.link-line {
  width: 48px;
  height: 2px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.2), rgba(59, 130, 246, 0.8));
}

.property-section {
  padding-bottom: 16px;
}

.property-section + .property-section {
  border-top: 1px solid rgba(219, 226, 239, 0.8);
  padding-top: 16px;
}

.property-section h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.property-form {
  --el-form-label-font-size: 13px;
}

.node-actions-panel {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.preview-card {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(59, 130, 246, 0.06);
  color: var(--app-text-muted);
}

@media (max-width: 1400px) {
  .editor-grid {
    grid-template-columns: 260px minmax(0, 1fr);
  }

  .property-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 960px) {
  .hero,
  .toolbar,
  .editor-grid {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }
}
</style>

