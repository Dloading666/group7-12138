import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { PermissionNode } from '@/types/domain'
import { mapPermissionNode, toBackendStatus } from '@/utils/admin'

function serializePermissionPayload(data: Partial<PermissionNode>) {
  return {
    name: data.name,
    code: data.code,
    type: data.type,
    parentId: data.parentId ?? null,
    path: data.path || undefined,
    component: data.component || undefined,
    icon: data.icon || undefined,
    sortOrder: data.sortOrder ?? 0,
    status: toBackendStatus(data.status)
  }
}

function normalizePermissionTree(res: ApiResult<any>): ApiResult<PermissionNode[]> {
  const nodes = Array.isArray(res.data) ? res.data : []
  return {
    ...res,
    data: nodes.map((item: unknown) => mapPermissionNode(item))
  }
}

export function getPermissionTree() {
  return request
    .get<any, ApiResult<any>>('/permissions/tree')
    .then((res) => normalizePermissionTree(res))
}

export function getPermissions(params?: Record<string, unknown>) {
  return request
    .get<any, ApiResult<any>>('/permissions', { params })
    .then((res) => normalizePermissionTree(res))
}

export function createPermission(data: Partial<PermissionNode>) {
  return request.post<any, ApiResult<PermissionNode>>('/permissions', serializePermissionPayload(data))
}

export function updatePermission(id: number, data: Partial<PermissionNode>) {
  return request.put<any, ApiResult<PermissionNode>>(`/permissions/${id}`, serializePermissionPayload(data))
}

export function updatePermissionStatus(id: number, status: string) {
  return request.put<any, ApiResult<PermissionNode>>(`/permissions/${id}/status`, { status: toBackendStatus(status) })
}

export function deletePermission(id: number) {
  return request.delete<any, ApiResult<void>>(`/permissions/${id}`)
}
