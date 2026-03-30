export type PermissionType = 'menu' | 'button' | 'api'

export type PermissionNode = {
  id: number
  name: string
  code: string
  type: PermissionType
  path?: string
  icon?: string
  parentId?: number
  status?: 'active' | 'inactive'
  children?: PermissionNode[]
}

export type PermissionScopeKey = 'taskMenuAndCreate' | 'taskMenuOnly' | 'noTaskMenu'

export const allPermissionCodes = [
  'dashboard:view',
  'system:user:view',
  'system:user:assign-scope',
  'system:role:view',
  'system:role:assign-permissions',
  'system:permission:view',
  'task:view',
  'task:create',
  'task:edit',
  'task:delete',
  'task:start',
  'task:stop',
  'robot:view',
  'robot:create',
  'robot:edit',
  'robot:delete',
  'robot:start',
  'robot:stop',
  'workflow:view',
  'monitor:view',
  'statistics:view',
  'settings:view'
] as const

export const basePermissionTree: PermissionNode[] = [
  {
    id: 1,
    name: '首页',
    code: 'dashboard:view',
    type: 'menu',
    path: '/dashboard',
    icon: 'HomeFilled',
    parentId: 0,
    status: 'active'
  },
  {
    id: 2,
    name: '系统管理',
    code: 'system:module:view',
    type: 'menu',
    path: '/users',
    icon: 'Setting',
    parentId: 0,
    status: 'active',
    children: [
      {
        id: 21,
        name: '用户管理',
        code: 'system:user:view',
        type: 'menu',
        path: '/users',
        parentId: 2,
        status: 'active',
        children: [
          {
            id: 211,
            name: '权限范围',
            code: 'system:user:assign-scope',
            type: 'button',
            parentId: 21,
            status: 'active'
          }
        ]
      },
      {
        id: 22,
        name: '角色管理',
        code: 'system:role:view',
        type: 'menu',
        path: '/roles',
        parentId: 2,
        status: 'active',
        children: [
          {
            id: 221,
            name: '分配权限',
            code: 'system:role:assign-permissions',
            type: 'button',
            parentId: 22,
            status: 'active'
          }
        ]
      },
      {
        id: 23,
        name: '权限管理',
        code: 'system:permission:view',
        type: 'menu',
        path: '/permissions',
        parentId: 2,
        status: 'active'
      }
    ]
  },
  {
    id: 3,
    name: '任务管理',
    code: 'task:module:view',
    type: 'menu',
    path: '/tasks',
    icon: 'List',
    parentId: 0,
    status: 'active',
    children: [
      {
        id: 31,
        name: '任务列表',
        code: 'task:view',
        type: 'menu',
        path: '/tasks',
        parentId: 3,
        status: 'active',
        children: [
          {
            id: 311,
            name: '创建任务',
            code: 'task:create',
            type: 'button',
            parentId: 31,
            status: 'active'
          },
          {
            id: 312,
            name: '编辑任务',
            code: 'task:edit',
            type: 'button',
            parentId: 31,
            status: 'active'
          },
          {
            id: 313,
            name: '删除任务',
            code: 'task:delete',
            type: 'button',
            parentId: 31,
            status: 'active'
          },
          {
            id: 314,
            name: '启动任务',
            code: 'task:start',
            type: 'button',
            parentId: 31,
            status: 'active'
          },
          {
            id: 315,
            name: '停止任务',
            code: 'task:stop',
            type: 'button',
            parentId: 31,
            status: 'active'
          }
        ]
      }
    ]
  },
  {
    id: 4,
    name: '流程定义与设计',
    code: 'workflow:view',
    type: 'menu',
    path: '/workflow/list',
    icon: 'Share',
    parentId: 0,
    status: 'active'
  },
  {
    id: 5,
    name: '机器人管理',
    code: 'robot:module:view',
    type: 'menu',
    path: '/robots',
    icon: 'Cpu',
    parentId: 0,
    status: 'active',
    children: [
      {
        id: 51,
        name: '机器人列表',
        code: 'robot:view',
        type: 'menu',
        path: '/robots',
        parentId: 5,
        status: 'active',
        children: [
          {
            id: 511,
            name: '启动机器人',
            code: 'robot:start',
            type: 'button',
            parentId: 51,
            status: 'active'
          },
          {
            id: 512,
            name: '停止机器人',
            code: 'robot:stop',
            type: 'button',
            parentId: 51,
            status: 'active'
          }
        ]
      }
    ]
  },
  {
    id: 6,
    name: '执行监控与记录',
    code: 'monitor:view',
    type: 'menu',
    path: '/monitor/realtime',
    icon: 'Monitor',
    parentId: 0,
    status: 'active'
  },
  {
    id: 7,
    name: '数据查询与统计',
    code: 'statistics:view',
    type: 'menu',
    path: '/statistics/query',
    icon: 'DataAnalysis',
    parentId: 0,
    status: 'active'
  },
  {
    id: 8,
    name: '系统设置',
    code: 'settings:view',
    type: 'menu',
    path: '/settings/basic',
    icon: 'Tools',
    parentId: 0,
    status: 'active'
  }
]

export const permissionScopes: Record<PermissionScopeKey, { grants: string[]; revokes: string[] }> = {
  taskMenuAndCreate: {
    grants: ['task:view', 'task:create'],
    revokes: []
  },
  taskMenuOnly: {
    grants: ['task:view'],
    revokes: ['task:create', 'task:edit', 'task:delete', 'task:start', 'task:stop']
  },
  noTaskMenu: {
    grants: [],
    revokes: ['task:view', 'task:create', 'task:edit', 'task:delete', 'task:start', 'task:stop']
  }
}

export function flattenPermissionTree(nodes: PermissionNode[]): PermissionNode[] {
  const result: PermissionNode[] = []
  for (const node of nodes) {
    result.push({ ...node, children: undefined })
    if (node.children?.length) {
      result.push(...flattenPermissionTree(node.children))
    }
  }
  return result
}

export function buildMenuTree(permissionCodes: string[]): PermissionNode[] {
  const allowed = new Set(permissionCodes)

  const walk = (nodes: PermissionNode[]): PermissionNode[] => {
    return nodes.flatMap((node) => {
      if (node.type !== 'menu') {
        return []
      }

      const childMenus = node.children?.length ? walk(node.children.filter((child) => child.type === 'menu')) : []
      const selfAllowed = allowed.has(node.code)
      if (!selfAllowed && childMenus.length === 0) {
        return []
      }

      return [
        {
          ...node,
          children: childMenus
        }
      ]
    })
  }

  return walk(basePermissionTree)
}
