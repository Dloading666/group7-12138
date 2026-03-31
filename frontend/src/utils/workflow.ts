import { demoRobots, demoTasks } from '@/mock/demo-data'
import type { RobotItem, TaskItem } from '@/types/domain'

export type WorkflowStatus = 'draft' | 'active' | 'paused'
export type WorkflowTriggerType = 'manual' | 'schedule' | 'event'
export type WorkflowNodeType = 'start' | 'task' | 'robot' | 'condition' | 'delay' | 'end'

export interface WorkflowNode {
  id: string
  type: WorkflowNodeType
  name: string
  taskType?: string
  robotId?: number
  robotName?: string
  timeoutMinutes?: number
  retryCount?: number
  durationMinutes?: number
  condition?: string
  note?: string
  nextId?: string
}

export interface WorkflowDefinition {
  id: number
  workflowId: string
  name: string
  code: string
  description: string
  category: string
  status: WorkflowStatus
  triggerType: WorkflowTriggerType
  cron: string
  owner: string
  version: number
  tags: string[]
  createdAt: string
  updatedAt: string
  nodes: WorkflowNode[]
  lastRunAt?: string
  runCount?: number
  successRate?: number
  boundTaskCount?: number
}

export interface WorkflowSummary {
  total: number
  active: number
  paused: number
  draft: number
  scheduleCount: number
  averageNodes: number
}

const STORAGE_KEY = 'rpa_workflows'

const NODE_LABELS: Record<WorkflowNodeType, string> = {
  start: '开始',
  task: '任务节点',
  robot: '机器人节点',
  condition: '条件分支',
  delay: '等待节点',
  end: '结束'
}

const EMPTY_CLOCK = '09:00'

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

function nowText() {
  return new Date().toLocaleString('zh-CN', { hour12: false })
}

function readStorage(): WorkflowDefinition[] | null {
  if (typeof localStorage === 'undefined') return null
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as unknown
    return Array.isArray(parsed) ? parsed.map((item) => normalizeWorkflow(item as Partial<WorkflowDefinition>)) : null
  } catch {
    return null
  }
}

function writeStorage(value: WorkflowDefinition[]) {
  if (typeof localStorage === 'undefined') return
  localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
}

function createId(prefix: string, index: number) {
  return `${prefix}-${String(index).padStart(3, '0')}`
}

function createWorkflowId(index: number) {
  return `WF-${String(index).padStart(4, '0')}`
}

function pickRobotName(robotId?: number) {
  if (!robotId) return ''
  return demoRobots.find((item) => item.id === robotId)?.name || `Robot-${String(robotId).padStart(2, '0')}`
}

function buildBaseNodes(triggerType: WorkflowTriggerType, robotId?: number): WorkflowNode[] {
  const robotName = pickRobotName(robotId)
  const taskType = demoTasks[0]?.type || '数据处理'
  const nodes: WorkflowNode[] = [
    { id: createId('node', 1), type: 'start', name: '开始' },
    {
      id: createId('node', 2),
      type: triggerType === 'event' ? 'condition' : 'task',
      name: triggerType === 'event' ? '事件校验' : '任务执行',
      taskType,
      retryCount: 2,
      timeoutMinutes: 15
    },
    {
      id: createId('node', 3),
      type: 'robot',
      name: '机器人处理',
      robotId,
      robotName,
      timeoutMinutes: 30
    },
    {
      id: createId('node', 4),
      type: 'delay',
      name: '等待/汇总',
      durationMinutes: 10,
      note: '等待外部系统返回或执行结果汇总'
    },
    { id: createId('node', 5), type: 'end', name: '结束' }
  ]

  return linkNodes(nodes)
}

function linkNodes(nodes: WorkflowNode[]) {
  return nodes.map((node, index) => ({
    ...node,
    nextId: index < nodes.length - 1 ? nodes[index + 1].id : undefined
  }))
}

function buildSeedWorkflows(tasks: TaskItem[] = demoTasks, robots: RobotItem[] = demoRobots): WorkflowDefinition[] {
  const taskTypes = Array.from(new Set(tasks.map((item) => item.type).filter(Boolean)))
  const robot = robots[0]
  const baseTime = nowText()

  return [
    {
      id: 1,
      workflowId: createWorkflowId(1),
      name: '日报采集与通知',
      code: 'daily-report-flow',
      description: '按天采集任务结果，整理后推送到指定渠道。',
      category: '运营',
      status: 'active',
      triggerType: 'schedule',
      cron: '0 9 * * *',
      owner: 'admin',
      version: 4,
      tags: ['日报', '通知', '自动化'],
      createdAt: baseTime,
      updatedAt: baseTime,
      nodes: buildBaseNodes('schedule', robot?.id),
      lastRunAt: baseTime,
      runCount: 128,
      successRate: 98.7,
      boundTaskCount: tasks.length
    },
    {
      id: 2,
      workflowId: createWorkflowId(2),
      name: '订单同步流程',
      code: 'order-sync-flow',
      description: '接收外部事件后同步订单状态，并按机器人执行批量核对。',
      category: '业务',
      status: 'draft',
      triggerType: 'event',
      cron: EMPTY_CLOCK,
      owner: 'operator',
      version: 2,
      tags: ['同步', '事件驱动'],
      createdAt: baseTime,
      updatedAt: baseTime,
      nodes: buildBaseNodes('event', robot?.id),
      lastRunAt: baseTime,
      runCount: 35,
      successRate: 94.2,
      boundTaskCount: Math.max(1, Math.min(tasks.length, 3))
    },
    {
      id: 3,
      workflowId: createWorkflowId(3),
      name: '机器人健康巡检',
      code: 'robot-health-check',
      description: '定时扫描机器人心跳和任务堆积，异常时自动发出告警。',
      category: '运维',
      status: 'paused',
      triggerType: 'schedule',
      cron: '*/30 * * * *',
      owner: 'system',
      version: 1,
      tags: ['巡检', '告警', '监控'],
      createdAt: baseTime,
      updatedAt: baseTime,
      nodes: buildBaseNodes('schedule', robot?.id),
      lastRunAt: baseTime,
      runCount: 62,
      successRate: 99.1,
      boundTaskCount: robots.length
    },
    {
      id: 4,
      workflowId: createWorkflowId(4),
      name: '文件处理审批',
      code: 'file-approval-flow',
      description: '文件进入后先做预处理，再交给审批节点确认。',
      category: '审批',
      status: 'active',
      triggerType: 'manual',
      cron: EMPTY_CLOCK,
      owner: 'admin',
      version: 3,
      tags: taskTypes.slice(0, 3),
      createdAt: baseTime,
      updatedAt: baseTime,
      nodes: buildBaseNodes('manual', robot?.id),
      lastRunAt: baseTime,
      runCount: 18,
      successRate: 97.3,
      boundTaskCount: 1
    }
  ]
}

function normalizeNode(node: Partial<WorkflowNode>, index: number, total: number): WorkflowNode {
  const fallbackType = index === 0 ? 'start' : index === total - 1 ? 'end' : 'task'
  const type = (node.type || fallbackType) as WorkflowNodeType
  const robotId = node.robotId == null ? undefined : Number(node.robotId)
  return {
    id: node.id ? String(node.id) : createId('node', index + 1),
    type,
    name: node.name ? String(node.name) : NODE_LABELS[type],
    taskType: node.taskType ? String(node.taskType) : undefined,
    robotId,
    robotName: node.robotName ? String(node.robotName) : pickRobotName(robotId),
    timeoutMinutes: node.timeoutMinutes == null ? undefined : Number(node.timeoutMinutes),
    retryCount: node.retryCount == null ? undefined : Number(node.retryCount),
    durationMinutes: node.durationMinutes == null ? undefined : Number(node.durationMinutes),
    condition: node.condition ? String(node.condition) : undefined,
    note: node.note ? String(node.note) : undefined,
    nextId: node.nextId ? String(node.nextId) : undefined
  }
}

export function normalizeWorkflow(input: Partial<WorkflowDefinition>): WorkflowDefinition {
  const nodes = Array.isArray(input.nodes) ? input.nodes.map((node, index, list) => normalizeNode(node, index, list.length)) : buildBaseNodes('manual')
  const linkedNodes = linkNodes(nodes)
  const id = input.id == null ? Date.now() : Number(input.id)
  return {
    id,
    workflowId: input.workflowId ? String(input.workflowId) : createWorkflowId(id),
    name: input.name ? String(input.name) : '未命名流程',
    code: input.code ? String(input.code) : `workflow-${id}`,
    description: input.description ? String(input.description) : '点击右侧属性面板完善流程说明。',
    category: input.category ? String(input.category) : '默认',
    status: (input.status as WorkflowStatus) || 'draft',
    triggerType: (input.triggerType as WorkflowTriggerType) || 'manual',
    cron: input.cron ? String(input.cron) : EMPTY_CLOCK,
    owner: input.owner ? String(input.owner) : 'admin',
    version: input.version == null ? 1 : Number(input.version),
    tags: Array.isArray(input.tags) ? input.tags.map((item) => String(item)).filter(Boolean) : ['流程'],
    createdAt: input.createdAt ? String(input.createdAt) : nowText(),
    updatedAt: input.updatedAt ? String(input.updatedAt) : nowText(),
    nodes: linkedNodes,
    lastRunAt: input.lastRunAt ? String(input.lastRunAt) : undefined,
    runCount: input.runCount == null ? 0 : Number(input.runCount),
    successRate: input.successRate == null ? 0 : Number(input.successRate),
    boundTaskCount: input.boundTaskCount == null ? 0 : Number(input.boundTaskCount)
  }
}

export function loadWorkflowCatalog(tasks: TaskItem[] = demoTasks, robots: RobotItem[] = demoRobots) {
  return readStorage() || buildSeedWorkflows(tasks, robots)
}

export function saveWorkflowCatalog(workflows: WorkflowDefinition[]) {
  const normalized = workflows.map((item) => normalizeWorkflow(item))
  writeStorage(normalized)
  return normalized
}

export function getWorkflowById(id: number, workflows: WorkflowDefinition[] = loadWorkflowCatalog()) {
  return workflows.find((item) => item.id === id)
}

export function createWorkflowDraft(partial: Partial<WorkflowDefinition> = {}, workflows: WorkflowDefinition[] = loadWorkflowCatalog()) {
  const nextId = workflows.reduce((max, item) => Math.max(max, item.id), 0) + 1
  const robot = demoRobots[0]
  const draft = normalizeWorkflow({
    id: nextId,
    workflowId: createWorkflowId(nextId),
    name: partial.name || '新流程',
    code: partial.code || `workflow-${nextId}`,
    description: partial.description || '从这里开始编排流程节点。',
    category: partial.category || '默认',
    status: partial.status || 'draft',
    triggerType: partial.triggerType || 'manual',
    cron: partial.cron || EMPTY_CLOCK,
    owner: partial.owner || 'admin',
    version: partial.version || 1,
    tags: partial.tags || ['新建'],
    createdAt: nowText(),
    updatedAt: nowText(),
    nodes: partial.nodes || buildBaseNodes(partial.triggerType || 'manual', robot?.id),
    lastRunAt: partial.lastRunAt,
    runCount: partial.runCount || 0,
    successRate: partial.successRate || 0,
    boundTaskCount: partial.boundTaskCount || 0
  })
  return draft
}

export function upsertWorkflowDefinition(workflow: WorkflowDefinition, workflows: WorkflowDefinition[] = loadWorkflowCatalog()) {
  const normalized = normalizeWorkflow({
    ...workflow,
    updatedAt: nowText()
  })
  const next = workflows.filter((item) => item.id !== normalized.id)
  next.unshift(normalized)
  saveWorkflowCatalog(next)
  return normalized
}

export function removeWorkflowDefinition(id: number, workflows: WorkflowDefinition[] = loadWorkflowCatalog()) {
  const next = workflows.filter((item) => item.id !== id)
  saveWorkflowCatalog(next)
  return next
}

export function cloneWorkflowDefinition(workflow: WorkflowDefinition, workflows: WorkflowDefinition[] = loadWorkflowCatalog()) {
  const nextId = workflows.reduce((max, item) => Math.max(max, item.id), 0) + 1
  const cloneItem = normalizeWorkflow({
    ...clone(workflow),
    id: nextId,
    workflowId: createWorkflowId(nextId),
    name: `${workflow.name} 副本`,
    code: `${workflow.code}-copy`,
    status: 'draft',
    version: 1,
    updatedAt: nowText(),
    createdAt: nowText(),
    runCount: 0,
    successRate: 0,
    lastRunAt: undefined
  })
  const next = [cloneItem, ...workflows]
  saveWorkflowCatalog(next)
  return cloneItem
}

export function buildWorkflowSummary(workflows: WorkflowDefinition[]) {
  const total = workflows.length
  const active = workflows.filter((item) => item.status === 'active').length
  const paused = workflows.filter((item) => item.status === 'paused').length
  const draft = workflows.filter((item) => item.status === 'draft').length
  const scheduleCount = workflows.filter((item) => item.triggerType === 'schedule').length
  const averageNodes = total ? Math.round(workflows.reduce((sum, item) => sum + item.nodes.length, 0) / total) : 0

  return { total, active, paused, draft, scheduleCount, averageNodes } satisfies WorkflowSummary
}

export function availableWorkflowNodeTypes() {
  return [
    { type: 'start' as const, label: '开始节点', hint: '流程入口', color: '#3b82f6' },
    { type: 'task' as const, label: '任务节点', hint: '调用任务执行', color: '#22c55e' },
    { type: 'robot' as const, label: '机器人节点', hint: '绑定机器人', color: '#7c3aed' },
    { type: 'condition' as const, label: '条件分支', hint: '按结果分流', color: '#f59e0b' },
    { type: 'delay' as const, label: '等待节点', hint: '等待外部响应', color: '#06b6d4' },
    { type: 'end' as const, label: '结束节点', hint: '流程收口', color: '#ef4444' }
  ]
}

export function createNodeByType(type: WorkflowNodeType, robot?: RobotItem, task?: TaskItem) {
  const base: WorkflowNode = {
    id: `${type}-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
    type,
    name: NODE_LABELS[type],
    nextId: undefined
  }

  if (type === 'task') {
    base.taskType = task?.type || demoTasks[0]?.type || '数据处理'
    base.retryCount = 2
    base.timeoutMinutes = 15
  }

  if (type === 'robot') {
    base.robotId = robot?.id
    base.robotName = robot?.name || pickRobotName(robot?.id)
    base.timeoutMinutes = 30
  }

  if (type === 'delay') {
    base.durationMinutes = 10
    base.note = '等待外部系统返回结果'
  }

  if (type === 'condition') {
    base.condition = '成功 / 失败分流'
  }

  return base
}

export function relinkWorkflowNodes(nodes: WorkflowNode[]) {
  return nodes.map((node, index) => ({
    ...node,
    nextId: index < nodes.length - 1 ? nodes[index + 1].id : undefined
  }))
}

export function insertWorkflowNode(nodes: WorkflowNode[], index: number, node: WorkflowNode) {
  const next = [...nodes]
  next.splice(index, 0, node)
  return relinkWorkflowNodes(next)
}

export function removeWorkflowNode(nodes: WorkflowNode[], nodeId: string) {
  const next = nodes.filter((item) => item.id !== nodeId)
  return relinkWorkflowNodes(next)
}

export function moveWorkflowNode(nodes: WorkflowNode[], nodeId: string, direction: 'left' | 'right') {
  const index = nodes.findIndex((item) => item.id === nodeId)
  if (index < 0) return nodes
  const target = direction === 'left' ? index - 1 : index + 1
  if (target < 0 || target >= nodes.length) return nodes
  const next = [...nodes]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  return relinkWorkflowNodes(next)
}

export function formatWorkflowStatus(status: WorkflowStatus) {
  return { draft: '草稿', active: '已启用', paused: '已暂停' }[status]
}

export function formatWorkflowTrigger(triggerType: WorkflowTriggerType) {
  return { manual: '手动触发', schedule: '定时触发', event: '事件触发' }[triggerType]
}

export function formatWorkflowNodeType(type: WorkflowNodeType) {
  return NODE_LABELS[type]
}
