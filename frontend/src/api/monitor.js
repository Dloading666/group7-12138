import request from '../utils/request.js'

/**
 * 获取实时监控统计数据
 */
export function getMonitorStats() {
  return request({
    url: '/monitor/stats',
    method: 'get'
  })
}

/**
 * 获取正在执行的任务列表
 */
export function getRunningTasks(params) {
  return request({
    url: '/monitor/running-tasks',
    method: 'get',
    params
  })
}

/**
 * 获取系统资源使用情况
 */
export function getSystemResources() {
  return request({
    url: '/monitor/system-resources',
    method: 'get'
  })
}

/**
 * 获取任务执行详情
 */
export function getTaskExecutionDetail(taskId) {
  return request({
    url: `/monitor/task/${taskId}/detail`,
    method: 'get'
  })
}
