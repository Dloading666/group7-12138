import type { PermissionNode, RoleItem, UserItem } from '@/types/domain'

type StatusLike = string | undefined | null

export function normalizeStatus(status: StatusLike): 'active' | 'inactive' {
  return String(status || '').toLowerCase() === 'inactive' ? 'inactive' : 'active'
}

export function toBackendStatus(status: StatusLike): 'ACTIVE' | 'INACTIVE' {
  return normalizeStatus(status) === 'inactive' ? 'INACTIVE' : 'ACTIVE'
}

function asRecord(payload: unknown) {
  return (payload ?? {}) as Record<string, unknown>
}

export function mapUserItem(payload: unknown): UserItem {
  const data = asRecord(payload)
  return {
    id: Number(data.id || 0),
    username: String(data.username || ''),
    realName: data.realName ? String(data.realName) : '',
    phone: data.phone ? String(data.phone) : '',
    email: data.email ? String(data.email) : '',
    roleId: data.roleId == null ? undefined : Number(data.roleId),
    roleCode: data.roleCode ? String(data.roleCode) : '',
    roleName: data.roleName ? String(data.roleName) : '',
    status: normalizeStatus(data.status as StatusLike),
    superAdmin: Boolean(data.superAdmin),
    createdAt: data.createdAt ? String(data.createdAt) : '',
    updatedAt: data.updatedAt ? String(data.updatedAt) : '',
    lastLoginAt: data.lastLoginAt ? String(data.lastLoginAt) : '',
    lastLoginIp: data.lastLoginIp ? String(data.lastLoginIp) : ''
  }
}

export function mapRoleItem(payload: unknown, userCount = 0): RoleItem {
  const data = asRecord(payload)
  const builtIn = Boolean(data.builtIn ?? data.isDefault)
  return {
    id: Number(data.id || 0),
    name: String(data.name || ''),
    code: String(data.code || ''),
    description: data.description ? String(data.description) : '',
    status: normalizeStatus(data.status as StatusLike),
    userCount,
    isDefault: builtIn,
    builtIn
  }
}

export function mapPermissionNode(payload: unknown): PermissionNode {
  const data = asRecord(payload)
  const children = Array.isArray(data.children) ? data.children.map((child) => mapPermissionNode(child)) : []

  return {
    id: Number(data.id || 0),
    name: String(data.name || ''),
    code: String(data.code || ''),
    type: (data.type as PermissionNode['type']) || 'MENU',
    parentId: data.parentId == null ? null : Number(data.parentId),
    path: data.path ? String(data.path) : '',
    component: data.component ? String(data.component) : '',
    icon: data.icon ? String(data.icon) : '',
    status: normalizeStatus(data.status as StatusLike),
    sortOrder: data.sortOrder == null ? 0 : Number(data.sortOrder),
    description: data.description ? String(data.description) : '',
    children
  }
}

export function buildRoleUserCountMap(users: UserItem[]) {
  const counter = new Map<number, number>()
  users.forEach((user) => {
    if (!user.roleId) return
    counter.set(user.roleId, (counter.get(user.roleId) || 0) + 1)
  })
  return counter
}

export function flattenPermissionIds(nodes: PermissionNode[]) {
  const ids: number[] = []
  const walk = (items: PermissionNode[]) => {
    items.forEach((item) => {
      ids.push(item.id)
      if (item.children?.length) {
        walk(item.children)
      }
    })
  }
  walk(nodes)
  return ids
}

export function collectPermissionLabelMap(nodes: PermissionNode[]) {
  const labels = new Map<number, string>()
  const walk = (items: PermissionNode[], parentNames: string[] = []) => {
    items.forEach((item) => {
      labels.set(item.id, [...parentNames, item.name].join(' / '))
      if (item.children?.length) {
        walk(item.children, [...parentNames, item.name])
      }
    })
  }
  walk(nodes)
  return labels
}
