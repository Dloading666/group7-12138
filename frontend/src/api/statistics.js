import request from '../utils/request'

export function queryCrawlResults(params, config = {}) {
  return request({
    url: '/data/query',
    method: 'get',
    params,
    ...config
  })
}

export function getTaskStatusStats(config = {}) {
  return request({
    url: '/data/stats/status',
    method: 'get',
    ...config
  })
}

export function getTaskTrendStats(params, config = {}) {
  return request({
    url: '/data/stats/trend',
    method: 'get',
    params,
    ...config
  })
}

export function getTaskTypeStats(config = {}) {
  return request({
    url: '/data/stats/type',
    method: 'get',
    ...config
  })
}

export function getOverviewStats(config = {}) {
  return request({
    url: '/data/stats/overview',
    method: 'get',
    ...config
  })
}
