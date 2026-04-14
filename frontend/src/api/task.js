import request from '../utils/request'

/**
 * 任务管理API
 */

// 获取任务列表（分页）
export function getTaskList(params) {
  return request({
    url: '/tasks',
    method: 'get',
    params
  })
}

// 获取所有任务
export function getAllTasks() {
  return request({
    url: '/tasks/all',
    method: 'get'
  })
}

// 获取任务详情
export function getTaskDetail(id) {
  return request({
    url: `/tasks/${id}`,
    method: 'get'
  })
}

// 创建任务
export function createTask(data) {
  return request({
    url: '/tasks',
    method: 'post',
    data
  })
}

// 更新任务
export function updateTask(data) {
  return request({
    url: `/tasks/${data.id}`,
    method: 'put',
    data
  })
}

// 删除任务
export function deleteTask(id) {
  return request({
    url: `/tasks/${id}`,
    method: 'delete'
  })
}

// 批量删除任务
export function batchDeleteTasks(ids) {
  return request({
    url: '/tasks/batch',
    method: 'delete',
    data: ids
  })
}

// 启动任务
export function startTask(id) {
  return request({
    url: `/tasks/${id}/start`,
    method: 'post'
  })
}

// 停止任务
export function stopTask(id) {
  return request({
    url: `/tasks/${id}/stop`,
    method: 'post'
  })
}

// 更新任务进度
export function updateTaskProgress(id, progress) {
  return request({
    url: `/tasks/${id}/progress`,
    method: 'put',
    params: { progress }
  })
}

// 获取任务统计
export function getTaskStats() {
  return request({
    url: '/tasks/stats',
    method: 'get'
  })
}
