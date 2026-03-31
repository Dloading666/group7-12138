import quietRequest from '@/api/quiet'
import type { ApiResult } from '@/types/common'
import type { WorkflowDefinition, WorkflowStatus, WorkflowTriggerType } from '@/utils/workflow'
import {
  cloneWorkflowDefinition,
  createWorkflowDraft,
  loadWorkflowCatalog,
  normalizeWorkflow,
  removeWorkflowDefinition,
  saveWorkflowCatalog,
  upsertWorkflowDefinition
} from '@/utils/workflow'

interface BackendWorkflowDto {
  id?: number
  workflowNo?: string
  name?: string
  type?: string
  status?: string
  progress?: number
  priority?: string
  executeType?: string
  scheduleTime?: string
  robotId?: number
  robotName?: string
  createdByUserId?: number
  definitionJson?: string
  result?: string
  createdAt?: string
  startTime?: string
  endTime?: string
  duration?: number
}

function mapWorkflowStatus(status?: string, parsed?: Partial<WorkflowDefinition>): WorkflowStatus {
  if (parsed?.status === 'draft' || parsed?.status === 'active' || parsed?.status === 'paused') {
    return parsed.status
  }
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'RUNNING' || normalized === 'COMPLETED') return 'active'
  if (normalized === 'FAILED') return 'paused'
  return 'draft'
}

function mapTriggerType(executeType?: string, parsed?: Partial<WorkflowDefinition>): WorkflowTriggerType {
  if (parsed?.triggerType === 'manual' || parsed?.triggerType === 'schedule' || parsed?.triggerType === 'event') {
    return parsed.triggerType
  }
  return String(executeType || '').toUpperCase() === 'SCHEDULED' ? 'schedule' : 'manual'
}

function parseDefinition(definitionJson?: string) {
  if (!definitionJson) return {}
  try {
    const parsed = JSON.parse(definitionJson) as Partial<WorkflowDefinition>
    return typeof parsed === 'object' && parsed ? parsed : {}
  } catch {
    return {}
  }
}

function mapWorkflow(dto: BackendWorkflowDto): WorkflowDefinition {
  const parsed = parseDefinition(dto.definitionJson)
  return normalizeWorkflow({
    ...parsed,
    id: Number(dto.id || parsed.id || Date.now()),
    workflowId: dto.workflowNo || parsed.workflowId,
    name: dto.name || parsed.name,
    code: parsed.code || dto.workflowNo,
    description: parsed.description,
    category: parsed.category || dto.type,
    status: mapWorkflowStatus(dto.status, parsed),
    triggerType: mapTriggerType(dto.executeType, parsed),
    cron: parsed.cron,
    owner: parsed.owner || (dto.createdByUserId == null ? 'admin' : `user-${dto.createdByUserId}`),
    version: parsed.version || 1,
    tags: parsed.tags || ['流程'],
    createdAt: parsed.createdAt || dto.createdAt,
    updatedAt: parsed.updatedAt || dto.createdAt,
    nodes: parsed.nodes,
    lastRunAt: parsed.lastRunAt || dto.startTime || dto.endTime,
    runCount: parsed.runCount || 0,
    successRate: parsed.successRate ?? 0,
    boundTaskCount: parsed.boundTaskCount || 0
  })
}

function findRobotId(workflow: WorkflowDefinition) {
  return workflow.nodes.find((node) => node.type === 'robot' && node.robotId)?.robotId
}

function serializeWorkflow(workflow: WorkflowDefinition) {
  return {
    workflowNo: workflow.workflowId,
    name: workflow.name,
    type: workflow.category,
    status: workflow.status === 'active' ? 'RUNNING' : workflow.status === 'paused' ? 'FAILED' : 'PENDING',
    progress: workflow.status === 'active' ? 60 : 0,
    priority: 'MEDIUM',
    executeType: workflow.triggerType === 'schedule' ? 'SCHEDULED' : 'IMMEDIATE',
    scheduleTime: undefined,
    robotId: findRobotId(workflow),
    createdByUserId: undefined,
    definitionJson: JSON.stringify(workflow),
    result: undefined
  }
}

async function withFallback<T>(runner: () => Promise<ApiResult<T>>, fallback: T) {
  try {
    const res = await runner()
    if (res.code === 200 && typeof res.data !== 'undefined') {
      return res
    }
  } catch {
    // backend unavailable; use local persistence
  }
  return { code: 200, message: '', data: fallback }
}

export function getWorkflowCatalog() {
  return withFallback(
    () =>
      quietRequest
        .get<any, ApiResult<any>>('/workflow/list')
        .then((res) => ({
          ...res,
          data: (Array.isArray(res.data?.records) ? res.data.records : []).map((item: BackendWorkflowDto) => mapWorkflow(item))
        })),
    loadWorkflowCatalog()
  )
}

export function getWorkflowDefinition(id: number) {
  return withFallback(
    () =>
      quietRequest
        .get<any, ApiResult<BackendWorkflowDto>>(`/workflow/design/${id}`)
        .then((res) => ({ ...res, data: mapWorkflow(res.data || {}) })),
    loadWorkflowCatalog().find((item) => item.id === id) || createWorkflowDraft()
  )
}

export async function saveWorkflowDefinition(workflow: WorkflowDefinition) {
  const localDraft = upsertWorkflowDefinition(workflow, loadWorkflowCatalog())
  try {
    const res = workflow.id
      ? await quietRequest.put<any, ApiResult<BackendWorkflowDto>>(`/workflow/design/${workflow.id}`, serializeWorkflow(workflow))
      : await quietRequest.post<any, ApiResult<BackendWorkflowDto>>('/workflow/design', serializeWorkflow(workflow))

    if (res.code === 200 && res.data) {
      const mapped = mapWorkflow(res.data)
      const stored = upsertWorkflowDefinition(mapped, loadWorkflowCatalog())
      return { ...res, data: stored }
    }
  } catch {
    // keep local persistence fallback below
  }
  return { code: 200, message: '', data: localDraft }
}

export async function deleteWorkflowDefinition(id: number) {
  const localData = removeWorkflowDefinition(id, loadWorkflowCatalog())
  try {
    await quietRequest.delete<any, ApiResult<void>>(`/workflow/design/${id}`)
  } catch {
    // keep local deletion if backend unavailable
  }
  return { code: 200, message: '', data: undefined as void, localData }
}

export async function cloneWorkflowById(id: number) {
  const current = await getWorkflowDefinition(id)
  if (!current.data) {
    return { code: 404, message: '流程不存在', data: null }
  }
  const local = cloneWorkflowDefinition(current.data, loadWorkflowCatalog())
  return saveWorkflowDefinition(local)
}

export function createWorkflowTemplateWorkflow(template: Partial<WorkflowDefinition> = {}) {
  return createWorkflowDraft(template, loadWorkflowCatalog())
}

export function persistWorkflowCatalog(workflows: WorkflowDefinition[]) {
  return saveWorkflowCatalog(workflows)
}
