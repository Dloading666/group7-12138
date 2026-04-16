import request from '../utils/request'

export function createAiAnalysisTask(data, config = {}) {
  return request({
    url: '/ai-analysis/tasks',
    method: 'post',
    data,
    ...config
  })
}

export function getAiAnalysisMessages(taskId, config = {}) {
  return request({
    url: `/ai-analysis/tasks/${taskId}/messages`,
    method: 'get',
    ...config
  })
}

export function sendAiAnalysisMessage(taskId, question, config = {}) {
  return request({
    url: `/ai-analysis/tasks/${taskId}/messages`,
    method: 'post',
    data: { question },
    ...config
  })
}
