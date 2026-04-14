import request from '../utils/request.js'

/**
 * 用户登录
 */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return request({
    url: '/auth/userinfo',
    method: 'get'
  })
}

/**
 * 用户登出
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/**
 * 获取个人资料
 */
export function getProfile() {
  return request({
    url: '/user/profile',
    method: 'get'
  })
}

/**
 * 获取当前用户权限
 */
export function getUserPermissions() {
  return request({
    url: '/user/permissions',
    method: 'get'
  })
}

/**
 * 更新个人资料
 */
export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 */
export function changePassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

// ==================== 用户管理 API ====================

/**
 * 获取用户列表（分页）
 */
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

/**
 * 获取所有用户
 */
export function getAllUsers() {
  return request({
    url: '/users/all',
    method: 'get'
  })
}

/**
 * 获取用户详情
 */
export function getUserById(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

/**
 * 创建用户
 */
export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

/**
 * 更新用户
 */
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 */
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除用户
 */
export function batchDeleteUsers(ids) {
  return request({
    url: '/users/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 更新用户状态
 */
export function updateUserStatus(id, status) {
  return request({
    url: `/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 重置密码
 */
export function resetPassword(id, password) {
  return request({
    url: `/users/${id}/password`,
    method: 'put',
    params: { password }
  })
}

/**
 * 获取用户统计
 */
export function getUserStats() {
  return request({
    url: '/users/stats',
    method: 'get'
  })
}
