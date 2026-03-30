import request from '@/api/http'
import type { ApiResult, PageResult } from '@/types/common'
import type { PermissionNode, PermissionScope, UserItem } from '@/types/domain'
import { mapPermissionNode, mapUserItem, toBackendStatus } from '@/utils/admin'

type UserPayload = Partial<UserItem> & {
  password?: string
  avatar?: string
}

type PermissionScopePayload = {
  userId?: number
  rolePermissionIds?: number[]
  effectivePermissionIds?: number[]
  grantedPermissionIds?: number[]
  revokedPermissionIds?: number[]
  effectivePermissionCodes?: string[]
  menuTree?: PermissionNode[]
}

function serializeUserPayload(data: UserPayload) {
  return {
    username: data.username,
    password: data.password || undefined,
    realName: data.realName,
    email: data.email || undefined,
    phone: data.phone || undefined,
    avatar: data.avatar || undefined,
    roleId: data.roleId,
    status: toBackendStatus(data.status),
    superAdmin: data.superAdmin
  }
}

function normalizeUserPage(res: ApiResult<any>): ApiResult<PageResult<UserItem>> {
  const records = Array.isArray(res.data?.records) ? res.data.records : []
  return {
    ...res,
    data: {
      list: records.map((item: Record<string, unknown>) => mapUserItem(item)),
      total: Number(res.data?.total || 0),
      page: Number(res.data?.page || 1),
      pageSize: Number(res.data?.size || 10)
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

function normalizePermissionScope(res: ApiResult<PermissionScopePayload>): ApiResult<PermissionScope> {
  const data = res.data || {}
  return {
    ...res,
    data: {
      userId: Number(data.userId || 0),
      rolePermissionIds: Array.isArray(data.rolePermissionIds) ? data.rolePermissionIds.map((item) => Number(item)) : [],
      grantedPermissionIds: Array.isArray(data.grantedPermissionIds) ? data.grantedPermissionIds.map((item) => Number(item)) : [],
      revokedPermissionIds: Array.isArray(data.revokedPermissionIds) ? data.revokedPermissionIds.map((item) => Number(item)) : [],
      effectivePermissionIds: Array.isArray(data.effectivePermissionIds) ? data.effectivePermissionIds.map((item) => Number(item)) : [],
      effectivePermissionCodes: Array.isArray(data.effectivePermissionCodes)
        ? data.effectivePermissionCodes.map((item) => String(item))
        : [],
      menuTree: Array.isArray(data.menuTree) ? data.menuTree.map((item) => mapPermissionNode(item)) : []
    }
  }
}

export function getUsers(params?: Record<string, unknown>) {
  return request
    .get<any, ApiResult<any>>('/users', { params })
    .then((res) => normalizeUserPage(res))
}

export function createUser(data: UserPayload) {
  return request.post<any, ApiResult<UserItem>>('/users', serializeUserPayload(data))
}

export function updateUser(id: number, data: UserPayload) {
  return request.put<any, ApiResult<UserItem>>(`/users/${id}`, serializeUserPayload(data))
}

export function deleteUser(id: number) {
  return request.delete<any, ApiResult<void>>(`/users/${id}`)
}

export function updateUserStatus(id: number, status: string) {
  return request.put<any, ApiResult<UserItem>>(`/users/${id}/status`, { status: toBackendStatus(status) })
}

export function resetPassword(id: number, password: string) {
  return request.put<any, ApiResult<UserItem>>(`/users/${id}/password`, { password })
}

export function getPermissionTree() {
  return request
    .get<any, ApiResult<any>>('/permissions/tree')
    .then((res) => normalizePermissionTree(res))
}

export function getUserPermissionEffective(id: number) {
  return request
    .get<any, ApiResult<PermissionScopePayload>>(`/users/${id}/permissions/effective`)
    .then((res) => normalizePermissionScope(res))
}

export function getUserPermissionOverrides(id: number) {
  return request
    .get<any, ApiResult<PermissionScopePayload>>(`/users/${id}/permissions/overrides`)
    .then((res) => normalizePermissionScope(res))
}

export function updateUserPermissionOverrides(id: number, data: { grants: number[]; revokes: number[] }) {
  return request
    .put<any, ApiResult<PermissionScopePayload>>(`/users/${id}/permissions/overrides`, data)
    .then((res) => normalizePermissionScope(res))
}
