import request from '../utils/request'

/**
 * 角色管理API
 */

// 获取角色列表（分页）
export function getRoleList(params) {
  return request({
    url: '/roles',
    method: 'get',
    params
  })
}

// 获取所有角色
export function getAllRoles() {
  return request({
    url: '/roles/all',
    method: 'get'
  })
}

// 获取启用的角色
export function getActiveRoles() {
  return request({
    url: '/roles/active',
    method: 'get'
  })
}

// 获取角色详情
export function getRoleById(id) {
  return request({
    url: `/roles/${id}`,
    method: 'get'
  })
}

// 创建角色
export function createRole(data) {
  return request({
    url: '/roles',
    method: 'post',
    data
  })
}

// 更新角色
export function updateRole(id, data) {
  return request({
    url: `/roles/${id}`,
    method: 'put',
    data
  })
}

// 删除角色
export function deleteRole(id) {
  return request({
    url: `/roles/${id}`,
    method: 'delete'
  })
}

// 批量删除角色
export function batchDeleteRoles(ids) {
  return request({
    url: '/roles/batch',
    method: 'delete',
    data: ids
  })
}

// 更新角色状态
export function updateRoleStatus(id, status) {
  return request({
    url: `/roles/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 为角色分配权限
export function assignPermissions(id, permissionIds) {
  return request({
    url: `/roles/${id}/permissions`,
    method: 'put',
    data: permissionIds
  })
}

// 获取角色的权限ID列表
export function getRolePermissions(id) {
  return request({
    url: `/roles/${id}/permissions`,
    method: 'get'
  })
}
