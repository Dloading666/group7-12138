export const ROLES = {
  ADMIN: 'ADMIN',
  USER: 'USER',
  GUEST: 'GUEST'
}

export const ROLE_DISPLAY_NAMES = {
  ADMIN: '系统管理员',
  USER: '普通用户',
  GUEST: '访客'
}

export const MENU_PERMISSIONS = {
  '/system': [ROLES.ADMIN],
  '/system/user': [ROLES.ADMIN],
  '/system/role': [ROLES.ADMIN],
  '/workflow/design': [ROLES.ADMIN],
  '/settings': [ROLES.ADMIN],
  '/settings/basic': [ROLES.ADMIN],
  '/settings/notification': [ROLES.ADMIN]
}

export const ACTION_PERMISSIONS = {
  'system:user:add': [ROLES.ADMIN],
  'system:user:edit': [ROLES.ADMIN],
  'system:user:delete': [ROLES.ADMIN],
  'system:role:add': [ROLES.ADMIN],
  'system:role:edit': [ROLES.ADMIN],
  'system:role:delete': [ROLES.ADMIN],
  'task:create': [ROLES.ADMIN],
  'task:edit': [ROLES.ADMIN],
  'task:delete': [ROLES.ADMIN],
  'task:execute': [ROLES.ADMIN],
  'task:stop': [ROLES.ADMIN],
  'workflow:create': [ROLES.ADMIN],
  'workflow:edit': [ROLES.ADMIN],
  'workflow:delete': [ROLES.ADMIN],
  'workflow:publish': [ROLES.ADMIN],
  'robot:add': [ROLES.ADMIN],
  'robot:edit': [ROLES.ADMIN],
  'robot:delete': [ROLES.ADMIN],
  'robot:start': [ROLES.ADMIN],
  'robot:stop': [ROLES.ADMIN],
  'robot:config': [ROLES.ADMIN],
  'settings:save': [ROLES.ADMIN]
}

export function hasMenuPermission(path, role) {
  const permissions = MENU_PERMISSIONS[path]
  if (!permissions) {
    return true
  }
  return permissions.includes(role)
}

export function hasActionPermission(action, role) {
  const permissions = ACTION_PERMISSIONS[action]
  if (!permissions) {
    return false
  }
  return permissions.includes(role)
}

export function isAdmin(role) {
  return role === ROLES.ADMIN
}

export function isUser(role) {
  return role === ROLES.USER
}

export function isGuest(role) {
  return role === ROLES.GUEST
}

export function getRoleDisplayName(role) {
  return ROLE_DISPLAY_NAMES[role] || role || '未知角色'
}

export function getAccessibleMenus(role) {
  return Object.keys(MENU_PERMISSIONS).filter((path) => hasMenuPermission(path, role))
}
