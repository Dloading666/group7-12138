import request from '../utils/request.js'

/**
 * 获取所有权限列表
 */
export function getAllPermissions() {
  return request({
    url: '/permissions/all',
    method: 'get'
  })
}

/**
 * 获取权限树形结构
 */
export function getPermissionTree() {
  return request({
    url: '/permissions/tree',
    method: 'get'
  })
}

/**
 * 获取权限详情
 */
export function getPermissionById(id) {
  return request({
    url: `/permissions/${id}`,
    method: 'get'
  })
}

/**
 * 创建权限
 */
export function createPermission(data) {
  return request({
    url: '/permissions',
    method: 'post',
    data
  })
}

/**
 * 更新权限
 */
export function updatePermission(id, data) {
  return request({
    url: `/permissions/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除权限
 */
export function deletePermission(id) {
  return request({
    url: `/permissions/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除权限
 */
export function batchDeletePermissions(ids) {
  return request({
    url: '/permissions/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 更新权限状态
 */
export function updatePermissionStatus(id, status) {
  return request({
    url: `/permissions/${id}/status`,
    method: 'put',
    params: { status }
  })
}
