import request from '../utils/request'

// 数据查询 - 对应 DataQueryController (/data)

export function queryCrawlResults(params) {
  return request({
    url: '/data/query',
    method: 'get',
    params
  })
}

// 统计报表 - 对应 DataQueryController (/data/stats/*)

export function getTaskStatusStats() {
  return request({
    url: '/data/stats/status',
    method: 'get'
  })
}

export function getTaskTrendStats(params) {
  return request({
    url: '/data/stats/trend',
    method: 'get',
    params  // { days: 7 | 30 }
  })
}

export function getTaskTypeStats() {
  return request({
    url: '/data/stats/type',
    method: 'get'
  })
}

export function getOverviewStats() {
  return request({
    url: '/data/stats/overview',
    method: 'get'
  })
}
