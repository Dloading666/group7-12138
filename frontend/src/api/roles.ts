import request from '@/api/http'
import type { ApiResult, PageResult } from '@/types/common'
import type { PermissionNode, RoleItem, RolePermissionAssignment } from '@/types/domain'
import { mapPermissionNode, mapRoleItem, toBackendStatus } from '@/utils/admin'

function serializeRolePayload(data: Partial<RoleItem>) {
  return {
    name: data.name,
    code: data.code,
    description: data.description || undefined,
    status: toBackendStatus(data.status),
    builtIn: data.builtIn
  }
}

function normalizeRolePage(res: ApiResult<any>): ApiResult<PageResult<RoleItem>> {
  const records = Array.isArray(res.data?.records) ? res.data.records : []
  return {
    ...res,
    data: {
      list: records.map((item: unknown) => mapRoleItem(item)),
      total: Number(res.data?.total || 0),
      page: Number(res.data?.page || 1),
      pageSize: Number(res.data?.size || 10)
    }
  }
}

function normalizeRolePermissionAssignment(res: ApiResult<any>): ApiResult<RolePermissionAssignment> {
  return {
    ...res,
    data: {
      roleId: Number(res.data?.roleId || 0),
      permissionIds: Array.isArray(res.data?.permissionIds) ? res.data.permissionIds.map((item: unknown) => Number(item)) : [],
      tree: Array.isArray(res.data?.tree) ? res.data.tree.map((item: unknown) => mapPermissionNode(item)) : []
    }
  }
}

function normalizePermissionTree(res: ApiResult<any>): ApiResult<PermissionNode[]> {
  const nodes = Array.isArray(res.data) ? res.data : []
  return {
    ...res,
    data: nodes.map((item: unknown) => mapPermissionNode(item))
  }
}

export function getRoles(params?: Record<string, unknown>) {
  return request
    .get<any, ApiResult<any>>('/roles', { params })
    .then((res) => normalizeRolePage(res))
}

export function createRole(data: Partial<RoleItem>) {
  return request.post<any, ApiResult<RoleItem>>('/roles', serializeRolePayload(data))
}

export function updateRole(id: number, data: Partial<RoleItem>) {
  return request.put<any, ApiResult<RoleItem>>(`/roles/${id}`, serializeRolePayload(data))
}

export function deleteRole(id: number) {
  return request.delete<any, ApiResult<void>>(`/roles/${id}`)
}

export function updateRoleStatus(id: number, status: string) {
  return request.put<any, ApiResult<RoleItem>>(`/roles/${id}/status`, { status: toBackendStatus(status) })
}

export function getRolePermissions(id: number) {
  return request
    .get<any, ApiResult<any>>(`/roles/${id}/permissions`)
    .then((res) => normalizeRolePermissionAssignment(res))
}

export function assignRolePermissions(id: number, permissionIds: number[]) {
  return request
    .put<any, ApiResult<any>>(`/roles/${id}/permissions`, { permissionIds })
    .then((res) => normalizeRolePermissionAssignment(res))
}

export function getPermissionTree() {
  return request
    .get<any, ApiResult<any>>('/permissions/tree')
    .then((res) => normalizePermissionTree(res))
}
