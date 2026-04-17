import request from '../utils/request'

export function createCrawlTask(data, config = {}) {
  return request({
    url: '/crawl/task',
    method: 'post',
    data,
    ...config
  })
}

export function getCrawlResultList(params, config = {}) {
  return request({
    url: '/crawl/results',
    method: 'get',
    params,
    ...config
  })
}

export function getCrawlResultDetail(taskId, config = {}) {
  return request({
    url: `/crawl/results/${taskId}`,
    method: 'get',
    ...config
  })
}
