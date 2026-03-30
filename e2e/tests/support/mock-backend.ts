import { type Page, type Route } from '@playwright/test'
import {
  allPermissionCodes,
  basePermissionTree,
  buildMenuTree,
  flattenPermissionTree,
  permissionScopes,
  type PermissionScopeKey
} from '../fixtures/permissions'

type RoleRecord = {
  id: number
  code: string
  name: string
  permissionCodes: string[]
}

type UserRecord = {
  id: number
  username: string
  password: string
  realName: string
  email: string
  avatar: string
  roleId: number
  status: 'active' | 'inactive'
}

type PermissionOverride = {
  grants: string[]
  revokes: string[]
}

type TokenSession = {
  token: string
  userId: number
}

type ApiEnvelope<T> = {
  code: number
  message: string
  data: T
}

function ok<T>(data: T, message = 'success'): ApiEnvelope<T> {
  return { code: 200, message, data }
}

function pageResult<T>(items: T[], total: number, page = 1, size = 10) {
  return ok({
    list: items,
    total,
    page,
    pageSize: size,
    content: items,
    totalElements: total,
    totalPages: Math.max(1, Math.ceil(total / size)),
    size,
    number: page - 1
  })
}

function jsonBody(payload: unknown) {
  return {
    status: 200,
    contentType: 'application/json; charset=utf-8',
    body: JSON.stringify(payload)
  }
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

const flatPermissions = flattenPermissionTree(basePermissionTree)
const permissionCodeMap = new Map(flatPermissions.map((node) => [node.code, node.id]))
const permissionIdMap = new Map(flatPermissions.map((node) => [node.id, node.code]))

function parseJsonBody(route: Route): Record<string, unknown> {
  const request = route.request()
  const raw = request.postData()
  if (!raw) {
    return {}
  }

  try {
    return request.postDataJSON() as Record<string, unknown>
  } catch {
    try {
      return JSON.parse(raw) as Record<string, unknown>
    } catch {
      return {}
    }
  }
}

function extractResourceId(route: Route, resource: string): number {
  const segments = new URL(route.request().url()).pathname.split('/').filter(Boolean)
  const resourceIndex = segments.lastIndexOf(resource)
  if (resourceIndex === -1) {
    return NaN
  }
  return Number(segments[resourceIndex + 1])
}

function codesToIds(codes: string[]) {
  return codes
    .map((code) => permissionCodeMap.get(code))
    .filter((id): id is number => typeof id === 'number')
}

function idsToCodes(ids: number[]) {
  return ids
    .map((id) => permissionIdMap.get(id))
    .filter((code): code is string => typeof code === 'string')
}

function buildDashboardOverview() {
  return {
    totalTasks: 42,
    runningTasks: 5,
    robotCount: 8,
    successRate: 0.86,
    trend: [118, 128, 99, 132, 88, 229, 210],
    successTrend: [116, 127, 98, 131, 86, 224, 205],
    statusDistribution: [
      { name: 'Done', value: 36 },
      { name: 'Running', value: 5 },
      { name: 'Pending', value: 4 },
      { name: 'Failed', value: 3 }
    ],
    recentTasks: [
      { id: 1001, taskId: 'T-1001', name: 'Daily Sync', type: 'Scheduled', status: 'running', progress: 76, createTime: '2026-03-30 10:20:00' },
      { id: 1002, taskId: 'T-1002', name: 'Store Check', type: 'Manual', status: 'completed', progress: 100, createTime: '2026-03-30 09:40:00' },
      { id: 1003, taskId: 'T-1003', name: 'Order Backfill', type: 'Scheduled', status: 'failed', progress: 58, createTime: '2026-03-29 18:10:00' }
    ]
  }
}

function buildTaskList() {
  return [
    {
      id: 1001,
      name: 'Daily Sync',
      type: 'Scheduled',
      status: 'running',
      progress: 76,
      priority: 'high',
      robotName: 'Robot-01',
      createTime: '2026-03-30 10:20:00'
    },
    {
      id: 1002,
      name: 'Store Check',
      type: 'Manual',
      status: 'completed',
      progress: 100,
      priority: 'medium',
      robotName: 'Robot-02',
      createTime: '2026-03-30 09:40:00'
    }
  ]
}

function buildRoleList() {
  return [
    { id: 1, name: 'Super Admin', code: 'SUPER_ADMIN', description: 'Fixed system administrator', status: 'active', sortOrder: 1 },
    { id: 2, name: 'Operator', code: 'OPERATOR', description: 'Default task operator role', status: 'active', sortOrder: 2 },
    { id: 3, name: 'Viewer', code: 'VIEWER', description: 'Read-only role', status: 'active', sortOrder: 3 }
  ]
}

function buildUserList() {
  return [
    {
      id: 1,
      username: 'admin',
      realName: 'System Admin',
      email: 'admin@example.com',
      phone: '13800000000',
      roleId: 1,
      status: 'active'
    },
    {
      id: 2,
      username: 'operator',
      realName: 'Operator',
      email: 'operator@example.com',
      phone: '13800000001',
      roleId: 2,
      status: 'active'
    },
    {
      id: 3,
      username: 'viewer',
      realName: 'Viewer',
      email: 'viewer@example.com',
      phone: '13800000002',
      roleId: 3,
      status: 'active'
    }
  ]
}

export type MockBackendHandle = ReturnType<typeof createMockBackend>

export function createMockBackend() {
  const roles = new Map<number, RoleRecord>([
    [1, { id: 1, code: 'SUPER_ADMIN', name: 'Super Admin', permissionCodes: [...allPermissionCodes] }],
    [2, { id: 2, code: 'OPERATOR', name: 'Operator', permissionCodes: ['dashboard:view', 'task:view'] }],
    [3, { id: 3, code: 'VIEWER', name: 'Viewer', permissionCodes: ['dashboard:view'] }]
  ])

  const users = new Map<number, UserRecord>([
    [
      1,
      {
        id: 1,
        username: 'admin',
        password: 'admin123',
        realName: 'System Admin',
        email: 'admin@example.com',
        avatar: '/avatars/admin.png',
        roleId: 1,
        status: 'active'
      }
    ],
    [
      2,
      {
        id: 2,
        username: 'operator',
        password: 'user123',
        realName: 'Operator',
        email: 'operator@example.com',
        avatar: '/avatars/operator.png',
        roleId: 2,
        status: 'active'
      }
    ],
    [
      3,
      {
        id: 3,
        username: 'viewer',
        password: 'viewer123',
        realName: 'Viewer',
        email: 'viewer@example.com',
        avatar: '/avatars/viewer.png',
        roleId: 3,
        status: 'active'
      }
    ]
  ])

  const overrides = new Map<number, PermissionOverride>([
    [2, { grants: [], revokes: [] }],
    [3, { grants: [], revokes: ['task:view', 'task:create', 'task:edit', 'task:delete', 'task:start', 'task:stop'] }]
  ])

  const sessions = new Map<string, TokenSession>()

  function getRole(userId: number) {
    const user = users.get(userId)
    if (!user) {
      throw new Error(`Unknown user id: ${userId}`)
    }
    const role = roles.get(user.roleId)
    if (!role) {
      throw new Error(`Unknown role id: ${user.roleId}`)
    }
    return role
  }

  function getOverride(userId: number): PermissionOverride {
    return overrides.get(userId) ?? { grants: [], revokes: [] }
  }

  function effectivePermissionCodes(userId: number) {
    const user = users.get(userId)
    if (!user) {
      return []
    }

    const role = getRole(userId)
    if (role.code === 'SUPER_ADMIN') {
      return [...allPermissionCodes]
    }

    const override = getOverride(userId)
    const combined = new Set([...role.permissionCodes, ...override.grants])
    for (const code of override.revokes) {
      combined.delete(code)
    }
    return [...combined]
  }

  function effectiveMenuTree(userId: number) {
    return buildMenuTree(effectivePermissionCodes(userId))
  }

  function effectivePermissionIds(userId: number) {
    return codesToIds(effectivePermissionCodes(userId))
  }

  function sessionPayload(userId: number) {
    const user = users.get(userId)
    if (!user) {
      throw new Error(`Unknown user id: ${userId}`)
    }
    const role = getRole(userId)
    return {
      user: {
        id: user.id,
        username: user.username,
        realName: user.realName,
        email: user.email,
        avatar: user.avatar,
        status: user.status,
        role: role.code,
        roleName: role.name
      },
      role: {
        id: role.id,
        code: role.code,
        name: role.name
      },
      permissionCodes: effectivePermissionCodes(userId),
      menuTree: effectiveMenuTree(userId)
    }
  }

  function resolveUserFromAuth(route: Route): number | null {
    const header = route.request().headers()['authorization']
    if (!header) {
      return null
    }
    const token = header.replace(/^Bearer\s+/i, '')
    const session = sessions.get(token)
    return session?.userId ?? null
  }

  function applyUserScope(userId: number, scope: PermissionScopeKey) {
    overrides.set(userId, clone(permissionScopes[scope]))
  }

  async function install(page: Page) {
    await page.addInitScript(() => {
      if (!window.sessionStorage.getItem('__rpa_mock_reset__')) {
        window.localStorage.removeItem('rpa_token')
        window.localStorage.removeItem('rpa_user')
        window.localStorage.removeItem('rpa_permissions')
        window.localStorage.removeItem('rpa_menu_tree')
        window.sessionStorage.setItem('__rpa_mock_reset__', '1')
      }
    })

    await page.route('**/api/auth/login', async (route) => {
      const body = parseJsonBody(route)
      const username = String(body.username ?? '')
      const password = String(body.password ?? '')
      const match = [...users.values()].find((user) => user.username === username && user.password === password)

      if (!match) {
        await route.fulfill(jsonBody({ code: 401, message: 'Invalid credentials', data: null }))
        return
      }

      const token = `mock-${match.id}-${Date.now()}`
      sessions.set(token, { token, userId: match.id })

      await route.fulfill(
        jsonBody(
          ok({
            token,
            tokenType: 'Bearer',
            expiresIn: 7_200,
            ...sessionPayload(match.id)
          })
        )
      )
    })

    await page.route('**/api/auth/me', async (route) => {
      const userId = resolveUserFromAuth(route)
      if (!userId) {
        await route.fulfill(jsonBody({ code: 401, message: 'Unauthorized', data: null }))
        return
      }
      await route.fulfill(jsonBody(ok(sessionPayload(userId))))
    })

    await page.route('**/api/auth/logout', async (route) => {
      const header = route.request().headers()['authorization']
      if (header) {
        const token = header.replace(/^Bearer\s+/i, '')
        sessions.delete(token)
      }
      await route.fulfill(jsonBody(ok(true)))
    })

    await page.route('**/api/dashboard/overview', async (route) => {
      await route.fulfill(jsonBody(ok(buildDashboardOverview())))
    })

    await page.route('**/api/tasks/stats', async (route) => {
      await route.fulfill(
        jsonBody(
          ok({
            totalTasks: 12,
            runningTasks: 3,
            successRate: 0.91,
            statusDistribution: [
              { name: 'Done', value: 7 },
              { name: 'Running', value: 3 },
              { name: 'Pending', value: 1 },
              { name: 'Failed', value: 1 }
            ]
          })
        )
      )
    })

    await page.route('**/api/robots/stats', async (route) => {
      await route.fulfill(
        jsonBody(
          ok({
            robotCount: 8,
            onlineCount: 6,
            busyCount: 2,
            offlineCount: 0
          })
        )
      )
    })

    await page.route('**/api/tasks', async (route) => {
      const method = route.request().method().toUpperCase()
      if (method === 'GET') {
        const tasks = buildTaskList()
        await route.fulfill(jsonBody(pageResult(tasks, tasks.length)))
        return
      }
      await route.fulfill(jsonBody(ok(true)))
    })

    await page.route('**/api/robots', async (route) => {
      const method = route.request().method().toUpperCase()
      if (method === 'GET') {
        const robots = [
          { id: 1, name: 'Robot-01', status: 'online', taskCount: 2 },
          { id: 2, name: 'Robot-02', status: 'busy', taskCount: 1 }
        ]
        await route.fulfill(jsonBody(pageResult(robots, robots.length)))
        return
      }
      await route.fulfill(jsonBody(ok(true)))
    })

    await page.route('**/api/permissions/tree', async (route) => {
      await route.fulfill(jsonBody(ok(clone(basePermissionTree))))
    })

    await page.route('**/api/permissions', async (route) => {
      const flat = flattenPermissionTree(basePermissionTree)
      await route.fulfill(jsonBody(pageResult(flat, flat.length)))
    })

    await page.route('**/api/roles/all', async (route) => {
      await route.fulfill(jsonBody(ok(buildRoleList())))
    })

    await page.route('**/api/roles/active', async (route) => {
      await route.fulfill(jsonBody(ok(buildRoleList().filter((role) => role.status === 'active'))))
    })

    await page.route('**/api/roles', async (route) => {
      const method = route.request().method().toUpperCase()
      if (method === 'GET') {
        await route.fulfill(jsonBody(pageResult(buildRoleList(), 3)))
        return
      }
      await route.fulfill(jsonBody(ok(true)))
    })

    await page.route(/\/api\/roles\/\d+\/permissions$/, async (route) => {
      const roleId = extractResourceId(route, 'roles')
      const method = route.request().method().toUpperCase()
      const role = roles.get(roleId)
      if (!role) {
        await route.fulfill(jsonBody({ code: 404, message: 'Role not found', data: null }))
        return
      }

      if (method === 'GET') {
        await route.fulfill(jsonBody(ok(codesToIds(role.permissionCodes))))
        return
      }

      if (method === 'PUT') {
        const body = parseJsonBody(route)
        const permissionIds = Array.isArray(body.permissionIds)
          ? body.permissionIds.map((id) => Number(id))
          : Array.isArray(body)
            ? body.map((id) => Number(id))
            : []
        role.permissionCodes = idsToCodes(permissionIds)
        roles.set(roleId, role)
        await route.fulfill(jsonBody(ok(permissionIds)))
        return
      }

      await route.fulfill(jsonBody({ code: 405, message: 'Method Not Allowed', data: null }))
    })

    await page.route('**/api/users/all', async (route) => {
      await route.fulfill(jsonBody(ok(buildUserList())))
    })

    await page.route('**/api/users/stats', async (route) => {
      await route.fulfill(
        jsonBody(
          ok({
            totalUsers: 3,
            activeUsers: 3,
            disabledUsers: 0
          })
        )
      )
    })

    await page.route('**/api/users', async (route) => {
      const method = route.request().method().toUpperCase()
      if (method === 'GET') {
        const usersList = buildUserList()
        await route.fulfill(jsonBody(pageResult(usersList, usersList.length)))
        return
      }
      await route.fulfill(jsonBody(ok(true)))
    })

    await page.route(/\/api\/users\/\d+\/permissions\/effective$/, async (route) => {
      const userId = extractResourceId(route, 'users')
      await route.fulfill(jsonBody(ok(effectivePermissionIds(userId))))
    })

    await page.route(/\/api\/users\/\d+\/permissions\/overrides$/, async (route) => {
      const userId = extractResourceId(route, 'users')
      const method = route.request().method().toUpperCase()
      if (method === 'GET') {
        const override = getOverride(userId)
        await route.fulfill(jsonBody(ok({ grants: codesToIds(override.grants), revokes: codesToIds(override.revokes) })))
        return
      }
      if (method === 'PUT') {
        const body = parseJsonBody(route)
        const grants = Array.isArray(body.grants) ? body.grants.map((id) => Number(id)) : []
        const revokes = Array.isArray(body.revokes) ? body.revokes.map((id) => Number(id)) : []
        const nextValue = { grants: idsToCodes(grants), revokes: idsToCodes(revokes) }
        overrides.set(userId, nextValue)
        await route.fulfill(jsonBody(ok({ grants, revokes })))
        return
      }
      await route.fulfill(jsonBody({ code: 405, message: 'Method Not Allowed', data: null }))
    })

    await page.route('**/api/user/profile', async (route) => {
      const userId = resolveUserFromAuth(route) ?? 1
      const user = users.get(userId) ?? users.get(1)!
      await route.fulfill(
        jsonBody(
          ok({
            id: user.id,
            username: user.username,
            realName: user.realName,
            email: user.email,
            avatar: user.avatar,
            phone: '13800000000'
          })
        )
      )
    })

    await page.route('**/api/user/password', async (route) => {
      await route.fulfill(jsonBody(ok(true)))
    })

  }

  function setUserScope(userId: number, scope: PermissionScopeKey) {
    applyUserScope(userId, scope)
  }

  function setUserOverrides(userId: number, grants: string[] = [], revokes: string[] = []) {
    overrides.set(userId, { grants: [...grants], revokes: [...revokes] })
  }

  function reset() {
    overrides.set(2, { grants: [], revokes: [] })
    overrides.set(3, { grants: [], revokes: ['task:view', 'task:create', 'task:edit', 'task:delete', 'task:start', 'task:stop'] })
    sessions.clear()
  }

  return {
    install,
    reset,
    setUserScope,
    setUserOverrides,
    getUserSession: (userId: number) => sessionPayload(userId),
    getPermissionTree: () => clone(basePermissionTree),
    getFlatPermissions: () => flattenPermissionTree(basePermissionTree),
    getDashboardOverview: buildDashboardOverview,
    getTaskList: buildTaskList,
    getRoles: buildRoleList,
    getUsers: buildUserList,
    effectivePermissionCodes
  }
}
