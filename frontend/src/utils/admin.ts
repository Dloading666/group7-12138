import type { PermissionNode, RobotItem, RoleItem, TaskItem, UserItem } from '@/types/domain'

type StatusLike = string | undefined | null

export function normalizeStatus(status: StatusLike): 'active' | 'inactive' {
  return String(status || '').toLowerCase() === 'inactive' ? 'inactive' : 'active'
}

export function toBackendStatus(status: StatusLike): 'ACTIVE' | 'INACTIVE' {
  return normalizeStatus(status) === 'inactive' ? 'INACTIVE' : 'ACTIVE'
}

export function normalizeTaskStatus(status: StatusLike): TaskItem['status'] {
  const value = String(status || '').toLowerCase()
  if (value === 'running' || value === 'completed' || value === 'failed' || value === 'stopped') {
    return value
  }
  return 'pending'
}

export function toBackendTaskStatus(status?: TaskItem['status']) {
  const value = String(status || 'pending').toUpperCase()
  if (value === 'RUNNING' || value === 'COMPLETED' || value === 'FAILED') return value
  return 'PENDING'
}

export function normalizeTaskPriority(priority?: TaskItem['priority'] | string | null): TaskItem['priority'] {
  const value = String(priority || '').toLowerCase()
  if (value === 'high' || value === 'low') return value
  return 'medium'
}

export function toBackendTaskPriority(priority?: TaskItem['priority']) {
  return normalizeTaskPriority(priority).toUpperCase()
}

export function normalizeExecuteType(type?: TaskItem['executeType'] | string | null): TaskItem['executeType'] {
  return String(type || '').toLowerCase() === 'scheduled' ? 'scheduled' : 'immediate'
}

export function toBackendExecuteType(type?: TaskItem['executeType']): 'IMMEDIATE' | 'SCHEDULED' {
  return normalizeExecuteType(type) === 'scheduled' ? 'SCHEDULED' : 'IMMEDIATE'
}

export function normalizeRobotStatus(status?: RobotItem['status'] | string | null): RobotItem['status'] {
  const value = String(status || '').toLowerCase()
  if (value === 'online' || value === 'busy' || value === 'disabled') return value
  return 'offline'
}

export function toBackendRobotStatus(status?: RobotItem['status']) {
  return normalizeRobotStatus(status).toUpperCase()
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

export function mapTaskItem(payload: unknown): TaskItem {
  const data = asRecord(payload)
  const robotId = data.robotId == null ? undefined : Number(data.robotId)
  return {
    id: Number(data.id || 0),
    taskId: data.taskId ? String(data.taskId) : data.taskNo ? String(data.taskNo) : '',
    name: String(data.name || ''),
    type: data.type ? String(data.type) : '',
    status: normalizeTaskStatus(data.status as StatusLike),
    progress: data.progress == null ? 0 : Number(data.progress),
    priority: normalizeTaskPriority(data.priority as string | null | undefined),
    robotId,
    robotName: data.robotName ? String(data.robotName) : robotId ? `Robot-${String(robotId).padStart(2, '0')}` : '',
    executeType: normalizeExecuteType(data.executeType as string | null | undefined),
    scheduledTime: data.scheduledTime ? String(data.scheduledTime) : data.scheduleTime ? String(data.scheduleTime) : '',
    createTime: data.createTime ? String(data.createTime) : data.createdAt ? String(data.createdAt) : '',
    startTime: data.startTime ? String(data.startTime) : '',
    endTime: data.endTime ? String(data.endTime) : '',
    description: data.description ? String(data.description) : '',
    result: data.result ? String(data.result) : '',
    errorMessage: data.errorMessage ? String(data.errorMessage) : '',
    userName: data.userName ? String(data.userName) : ''
  }
}

export function mapRobotItem(payload: unknown): RobotItem {
  const data = asRecord(payload)
  const id = Number(data.id || 0)
  return {
    id,
    robotId: data.robotId ? String(data.robotId) : id ? `R-${String(id).padStart(3, '0')}` : '',
    name: String(data.name || ''),
    ip: data.ip ? String(data.ip) : data.ipAddress ? String(data.ipAddress) : '',
    status: normalizeRobotStatus(data.status as string | null | undefined),
    taskCount: data.taskCount == null ? 0 : Number(data.taskCount),
    lastHeartbeat: data.lastHeartbeat ? String(data.lastHeartbeat) : '',
    description: data.description ? String(data.description) : data.type ? String(data.type) : ''
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
