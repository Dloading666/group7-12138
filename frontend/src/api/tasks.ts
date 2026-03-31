import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { PageResult } from '@/types/common'
import type { TaskItem } from '@/types/domain'
import { mapTaskItem, toBackendExecuteType, toBackendTaskPriority, toBackendTaskStatus } from '@/utils/admin'

function normalizeTaskPage(res: ApiResult<any>): ApiResult<PageResult<TaskItem>> {
  const records = Array.isArray(res.data?.records) ? res.data.records : []
  return {
    ...res,
    data: {
      list: records.map((item: unknown) => mapTaskItem(item)),
      total: Number(res.data?.total || 0),
      page: Number(res.data?.page || 1),
      pageSize: Number(res.data?.size || 10)
    }
  }
}

function serializeTaskPayload(data: Partial<TaskItem>) {
  const taskId = data.taskId?.trim() || `T${Date.now()}`
  return {
    taskNo: taskId,
    name: data.name?.trim(),
    type: data.type?.trim() || undefined,
    status: toBackendTaskStatus(data.status),
    progress: data.progress ?? 0,
    priority: toBackendTaskPriority(data.priority),
    executeType: toBackendExecuteType(data.executeType),
    scheduleTime: data.scheduledTime || undefined,
    robotId: data.robotId,
    createdByUserId: undefined,
    params: data.description || undefined,
    result: data.result || undefined
  }
}

export function getTasks(params?: Record<string, unknown>) {
  return request
    .get<any, ApiResult<any>>('/tasks', { params })
    .then((res) => normalizeTaskPage(res))
}

export function createTask(data: Partial<TaskItem>) {
  return request.post<any, ApiResult<TaskItem>>('/tasks', serializeTaskPayload(data))
}

export function updateTask(id: number, data: Partial<TaskItem>) {
  return request.put<any, ApiResult<TaskItem>>(`/tasks/${id}`, serializeTaskPayload(data))
}

export function deleteTask(id: number) {
  return request.delete<any, ApiResult<void>>(`/tasks/${id}`)
}

export function startTask(id: number) {
  return request.post<any, ApiResult<void>>(`/tasks/${id}/start`)
}

export function stopTask(id: number) {
  return request.post<any, ApiResult<void>>(`/tasks/${id}/stop`)
}
