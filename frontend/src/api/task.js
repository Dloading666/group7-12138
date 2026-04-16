import request from '../utils/request'

export function getTaskList(params, config = {}) {
  return request({
    url: '/tasks',
    method: 'get',
    params,
    ...config
  })
}

export function getAllTasks(config = {}) {
  return request({
    url: '/tasks/all',
    method: 'get',
    ...config
  })
}

export function getTaskDetail(id, config = {}) {
  return request({
    url: `/tasks/${id}`,
    method: 'get',
    ...config
  })
}

export function createTask(data, config = {}) {
  return request({
    url: '/tasks',
    method: 'post',
    data,
    ...config
  })
}

export function updateTask(data, config = {}) {
  return request({
    url: `/tasks/${data.id}`,
    method: 'put',
    data,
    ...config
  })
}

export function deleteTask(id, config = {}) {
  return request({
    url: `/tasks/${id}`,
    method: 'delete',
    ...config
  })
}

export function batchDeleteTasks(ids, config = {}) {
  return request({
    url: '/tasks/batch',
    method: 'delete',
    data: ids,
    ...config
  })
}

export function startTask(id, config = {}) {
  return request({
    url: `/tasks/${id}/start`,
    method: 'post',
    ...config
  })
}

export function stopTask(id, config = {}) {
  return request({
    url: `/tasks/${id}/stop`,
    method: 'post',
    ...config
  })
}

export function updateTaskProgress(id, progress, config = {}) {
  return request({
    url: `/tasks/${id}/progress`,
    method: 'put',
    params: { progress },
    ...config
  })
}

export function getTaskStats(config = {}) {
  return request({
    url: '/tasks/stats',
    method: 'get',
    ...config
  })
}