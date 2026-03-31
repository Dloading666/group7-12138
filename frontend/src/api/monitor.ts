import quietRequest from '@/api/quiet'
import type { ApiResult, PageResult } from '@/types/common'
import type { ExecutionLogItem } from '@/types/domain'

interface BackendExecutionLog {
  id?: number
  taskId?: number
  taskNo?: string
  taskName?: string
  robotId?: number
  robotName?: string
  level?: string
  message?: string
  createdAt?: string
}

interface BackendRealtimeSnapshot {
  updatedAt?: string
  [key: string]: unknown
}

function normalizeLogLevel(level?: string): ExecutionLogItem['level'] {
  const normalized = String(level || '').toUpperCase()
  if (normalized === 'WARN' || normalized === 'ERROR') return normalized
  return 'INFO'
}

function mapExecutionLog(payload: BackendExecutionLog): ExecutionLogItem {
  const level = normalizeLogLevel(payload.level)
  const source: ExecutionLogItem['source'] = payload.taskId ? 'task' : payload.robotId ? 'robot' : 'system'
  const title = payload.taskName
    ? `${payload.taskName} 执行日志`
    : payload.robotName
      ? `${payload.robotName} 运行日志`
      : '系统执行日志'

  return {
    id: Number(payload.id || 0),
    time: payload.createdAt || new Date().toISOString(),
    level,
    source,
    title,
    message: payload.message || '',
    taskId: payload.taskNo || (payload.taskId == null ? undefined : String(payload.taskId)),
    taskName: payload.taskName,
    robotId: payload.robotId,
    robotName: payload.robotName
  }
}

export function getExecutionLogs(params?: Record<string, unknown>) {
  return quietRequest
    .get<any, ApiResult<any>>('/monitor/logs', { params })
    .then((res) => ({
      ...res,
      data: {
        list: (Array.isArray(res.data?.records) ? res.data.records : []).map((item: BackendExecutionLog) => mapExecutionLog(item)),
        total: Number(res.data?.total || 0),
        page: Number(res.data?.page || 1),
        pageSize: Number(res.data?.size || 10)
      } satisfies PageResult<ExecutionLogItem>
    }))
}

export function getRealtimeSnapshot() {
  return quietRequest
    .get<any, ApiResult<BackendRealtimeSnapshot>>('/monitor/realtime')
    .then((res) => ({
      ...res,
      data: {
        ...res.data,
        updatedAt: res.data?.updatedAt || new Date().toISOString()
      }
    }))
}
