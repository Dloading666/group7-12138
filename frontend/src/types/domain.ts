export interface DashboardOverview {
  totalTasks: number
  runningTasks: number
  robotCount: number
  successRate: number
  trend: number[]
  successTrend: number[]
  statusDistribution: Array<{ name: string; value: number }>
  recentTasks: TaskItem[]
}

export interface TaskItem {
  id: number
  taskId?: string
  name: string
  type: string
  status: 'pending' | 'running' | 'completed' | 'failed' | 'stopped'
  progress: number
  priority: 'high' | 'medium' | 'low'
  robotName?: string
  robotId?: number
  executeType?: 'immediate' | 'scheduled'
  scheduledTime?: string
  createTime?: string
  startTime?: string
  endTime?: string
  description?: string
  result?: string
  errorMessage?: string
  userName?: string
}

export interface UserItem {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  roleId?: number
  roleCode?: string
  roleName?: string
  status: 'active' | 'inactive'
  superAdmin?: boolean
  createdAt?: string
  updatedAt?: string
  lastLoginAt?: string
  lastLoginIp?: string
}

export interface RoleItem {
  id: number
  name: string
  code: string
  description?: string
  status: 'active' | 'inactive'
  userCount?: number
  isDefault?: boolean
  builtIn?: boolean
}

export interface PermissionNode {
  id: number
  name: string
  code: string
  type: 'MENU' | 'BUTTON' | 'API'
  parentId?: number | null
  path?: string
  component?: string
  icon?: string
  status?: 'active' | 'inactive'
  sortOrder?: number
  description?: string
  children?: PermissionNode[]
}

export interface PermissionScope {
  userId: number
  rolePermissionIds: number[]
  grantedPermissionIds: number[]
  revokedPermissionIds: number[]
  effectivePermissionIds: number[]
  effectivePermissionCodes: string[]
  menuTree: PermissionNode[]
}

export interface RolePermissionAssignment {
  roleId: number
  permissionIds: number[]
  tree: PermissionNode[]
}

export interface RobotItem {
  id: number
  robotId?: string
  name: string
  ip?: string
  status: 'online' | 'offline' | 'busy' | 'disabled'
  taskCount?: number
  lastHeartbeat?: string
  description?: string
}

export interface ExecutionLogItem {
  id: number
  time: string
  level: 'INFO' | 'WARN' | 'ERROR'
  source: 'task' | 'robot' | 'system'
  title: string
  message: string
  taskId?: string
  taskName?: string
  robotId?: number
  robotName?: string
  duration?: number
}

export interface BasicSettingsState {
  systemName: string
  systemSubtitle: string
  companyName: string
  supportEmail: string
  supportPhone: string
  loginNotice: string
  maintenanceMode: boolean
}

export interface NotificationSettingsState {
  emailEnabled: boolean
  emailHost: string
  emailPort: number
  emailUsername: string
  emailFrom: string
  webhookEnabled: boolean
  taskFailureAlert: boolean
  robotOfflineAlert: boolean
  webhookUrl: string
}
