/**
 * 权限配置
 * 定义不同角色的菜单权限和操作权限
 */

// 角色枚举
export const ROLES = {
  ADMIN: 'ADMIN',
  USER: 'USER'
}

// 角色显示名称
export const ROLE_DISPLAY_NAMES = {
  ADMIN: '管理员',
  USER: '普通用户'
}

/**
 * 菜单权限配置
 * 每个菜单项定义允许访问的角色列表
 * 注意：只有明确需要限制的路径才配置在这里
 */
export const MENU_PERMISSIONS = {
  // 系统管理 - 仅管理员
  '/system': [ROLES.ADMIN],
  '/system/user': [ROLES.ADMIN],
  '/system/role': [ROLES.ADMIN],

  // 任务创建 - 仅管理员
  '/task/create': [ROLES.ADMIN],

  // 流程设计 - 仅管理员
  '/workflow/design': [ROLES.ADMIN],

  // 系统设置 - 仅管理员
  '/settings': [ROLES.ADMIN],
  '/settings/basic': [ROLES.ADMIN],
  '/settings/notification': [ROLES.ADMIN]
}

/**
 * 操作权限配置
 * 定义按钮级别的权限
 */
export const ACTION_PERMISSIONS = {
  // 用户管理操作
  'system:user:add': [ROLES.ADMIN],
  'system:user:edit': [ROLES.ADMIN],
  'system:user:delete': [ROLES.ADMIN],
  'system:user:resetPassword': [ROLES.ADMIN],

  // 角色管理操作
  'system:role:add': [ROLES.ADMIN],
  'system:role:edit': [ROLES.ADMIN],
  'system:role:delete': [ROLES.ADMIN],

  // 任务管理操作
  'task:create': [ROLES.ADMIN],
  'task:edit': [ROLES.ADMIN],
  'task:delete': [ROLES.ADMIN],
  'task:execute': [ROLES.ADMIN],
  'task:stop': [ROLES.ADMIN],

  // 流程管理操作
  'workflow:create': [ROLES.ADMIN],
  'workflow:edit': [ROLES.ADMIN],
  'workflow:delete': [ROLES.ADMIN],
  'workflow:publish': [ROLES.ADMIN],

  // 机器人管理操作
  'robot:add': [ROLES.ADMIN],
  'robot:edit': [ROLES.ADMIN],
  'robot:delete': [ROLES.ADMIN],
  'robot:start': [ROLES.ADMIN],
  'robot:stop': [ROLES.ADMIN],
  'robot:config': [ROLES.ADMIN],

  // 系统设置操作
  'settings:save': [ROLES.ADMIN]
}

/**
 * 检查用户是否有菜单访问权限
 * @param {string} path - 菜单路径
 * @param {string} role - 用户角色
 * @returns {boolean}
 */
export function hasMenuPermission(path, role) {
  const permissions = MENU_PERMISSIONS[path]
  // 如果路径未配置权限限制，默认允许访问
  // 只限制明确配置的路径
  if (!permissions) {
    return true
  }
  return permissions.includes(role)
}

/**
 * 检查用户是否有操作权限
 * @param {string} action - 操作标识
 * @param {string} role - 用户角色
 * @returns {boolean}
 */
export function hasActionPermission(action, role) {
  const permissions = ACTION_PERMISSIONS[action]
  if (!permissions) {
    // 未配置的操作默认拒绝
    return false
  }
  return permissions.includes(role)
}

/**
 * 检查用户是否为管理员
 * @param {string} role - 用户角色
 * @returns {boolean}
 */
export function isAdmin(role) {
  return role === ROLES.ADMIN
}

/**
 * 检查用户是否为普通用户（只读权限）
 * @param {string} role - 用户角色
 * @returns {boolean}
 */
export function isUser(role) {
  return role === ROLES.USER
}

/**
 * 获取用户可访问的菜单列表
 * @param {string} role - 用户角色
 * @returns {Array} 菜单路径数组
 */
export function getAccessibleMenus(role) {
  return Object.keys(MENU_PERMISSIONS).filter(path =>
    hasMenuPermission(path, role)
  )
}
