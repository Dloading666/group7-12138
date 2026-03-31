import request from '@/api/http'
import type { ApiResult, PageResult } from '@/types/common'
import type { RobotItem } from '@/types/domain'
import { mapRobotItem, toBackendRobotStatus } from '@/utils/admin'

function normalizeRobotPage(res: ApiResult<any>): ApiResult<PageResult<RobotItem>> {
  const records = Array.isArray(res.data?.records) ? res.data.records : []
  return {
    ...res,
    data: {
      list: records.map((item: unknown) => mapRobotItem(item)),
      total: Number(res.data?.total || 0),
      page: Number(res.data?.page || 1),
      pageSize: Number(res.data?.size || 10)
    }
  }
}

function serializeRobotPayload(data: Partial<RobotItem>) {
  return {
    name: data.name?.trim(),
    type: data.description?.trim() || undefined,
    status: toBackendRobotStatus(data.status),
    ipAddress: data.ip?.trim() || undefined,
    port: undefined,
    config: undefined,
    lastHeartbeat: data.lastHeartbeat || undefined,
    taskCount: data.taskCount ?? 0,
    successRate: undefined
  }
}

export function getRobots(params?: Record<string, unknown>) {
  return request
    .get<any, ApiResult<any>>('/robots', { params })
    .then((res) => normalizeRobotPage(res))
}

export function createRobot(data: Partial<RobotItem>) {
  return request.post<any, ApiResult<RobotItem>>('/robots', serializeRobotPayload(data))
}

export function updateRobot(id: number, data: Partial<RobotItem>) {
  return request.put<any, ApiResult<RobotItem>>(`/robots/${id}`, serializeRobotPayload(data))
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
