import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { PageResult } from '@/types/common'
import type { TaskItem } from '@/types/domain'

export function getTasks(params?: Record<string, unknown>) {
  return request.get<any, ApiResult<PageResult<TaskItem>>>('/tasks', { params })
}

export function createTask(data: Partial<TaskItem>) {
  return request.post<any, ApiResult<TaskItem>>('/tasks', data)
}

export function updateTask(id: number, data: Partial<TaskItem>) {
  return request.put<any, ApiResult<TaskItem>>(`/tasks/${id}`, data)
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

