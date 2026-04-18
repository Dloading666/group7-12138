import request from '../utils/request.js'

/**
 * 获取所有启用的节点类型
 */
export function getNodeTypes() {
  return request({
    url: '/workflows/node-types',
    method: 'get'
  })
}

/**
 * 获取所有节点类型（包括禁用的）
 */
export function getAllNodeTypes() {
  return request({
    url: '/workflows/node-types/all',
    method: 'get'
  })
}

/**
 * 创建节点类型
 */
export function createNodeType(data) {
  return request({
    url: '/workflows/node-types',
    method: 'post',
    data
  })
}

/**
 * 更新节点类型
 */
export function updateNodeType(id, data) {
  return request({
    url: `/workflows/node-types/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除节点类型
 */
export function deleteNodeType(id) {
  return request({
    url: `/workflows/node-types/${id}`,
    method: 'delete'
  })
}

/**
 * 获取流程列表（分页）
 */
export function getWorkflowList(params) {
  return request({
    url: '/workflows',
    method: 'get',
    params
  })
}

/**
 * 获取所有流程
 */
export function getAllWorkflows() {
  return request({
    url: '/workflows/all',
    method: 'get'
  })
}

/**
 * 获取流程详情
 */
export function getWorkflowById(id) {
  return request({
    url: `/workflows/${id}`,
    method: 'get'
  })
}

/**
 * 创建流程
 */
export function createWorkflow(data) {
  return request({
    url: '/workflows',
    method: 'post',
    data
  })
}

/**
 * 更新流程
 */
export function updateWorkflow(id, data) {
  return request({
    url: `/workflows/${id}`,
    method: 'put',
    data
  })
}

/**
 * 发布流程
 */
export function publishWorkflow(id) {
  return request({
    url: `/workflows/${id}/publish`,
    method: 'post'
  })
}

/**
 * 删除流程
 */
export function deleteWorkflow(id) {
  return request({
    url: `/workflows/${id}`,
    method: 'delete'
  })
}

export function getPublishedWorkflowVersions(config = {}) {
  return request({
    url: '/workflows/published-versions',
    method: 'get',
    ...config
  })
}

export function getWorkflowVersion(id, config = {}) {
  return request({
    url: `/workflows/versions/${id}`,
    method: 'get',
    ...config
  })
}

export function createWorkflowDraft(data, config = {}) {
  return request({
    url: '/workflow-assistant/drafts',
    method: 'post',
    data,
    timeout: 180000,
    ...config
  })
}

export function createWorkflowDebugRun(id, data = {}, config = {}) {
  return request({
    url: `/workflows/${id}/debug-runs`,
    method: 'post',
    data,
    timeout: 180000,
    ...config
  })
}

export function getWorkflowDebugRuns(id, config = {}) {
  return request({
    url: `/workflows/${id}/debug-runs`,
    method: 'get',
    ...config
  })
}

export function getWorkflowDebugRunDetail(runId, config = {}) {
  return request({
    url: `/workflows/debug-runs/${runId}`,
    method: 'get',
    ...config
  })
}
