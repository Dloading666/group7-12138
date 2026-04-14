import request from '../utils/request.js'

/**
 * 分页查询日志
 */
export function getLogList(params) {
  return request({
    url: '/logs',
    method: 'get',
    params
  })
}

/**
 * 获取任务的所有日志
 */
export function getLogsByTaskId(taskId) {
  return request({
    url: `/logs/task/${taskId}`,
    method: 'get'
  })
}

/**
 * 清空所有日志
 */
export function clearAllLogs() {
  return request({
    url: '/logs/clear',
    method: 'delete'
  })
}

/**
 * 清空指定天数之前的日志
 */
export function clearLogsBeforeDays(days) {
  return request({
    url: `/logs/clear-before/${days}`,
    method: 'delete'
  })
}

/**
 * 导出日志
 */
export function exportLogs(params) {
  return request({
    url: '/logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 获取日志统计
 */
export function getLogStats(params) {
  return request({
    url: '/logs/stats',
    method: 'get',
    params
  })
}
