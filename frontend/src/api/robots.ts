import request from '@/api/http'
import type { ApiResult, PageResult } from '@/types/common'
import type { RobotItem } from '@/types/domain'

export function getRobots(params?: Record<string, unknown>) {
  return request.get<any, ApiResult<PageResult<RobotItem>>>('/robots', { params })
}

export function createRobot(data: Partial<RobotItem>) {
  return request.post<any, ApiResult<RobotItem>>('/robots', data)
}

export function updateRobot(id: number, data: Partial<RobotItem>) {
  return request.put<any, ApiResult<RobotItem>>(`/robots/${id}`, data)
}

export function deleteRobot(id: number) {
  return request.delete<any, ApiResult<void>>(`/robots/${id}`)
}

export function startRobot(id: number) {
  return request.post<any, ApiResult<void>>(`/robots/${id}/start`)
}

export function stopRobot(id: number) {
  return request.post<any, ApiResult<void>>(`/robots/${id}/stop`)
}

