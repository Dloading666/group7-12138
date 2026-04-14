import request from '../utils/request'

// 获取采集配置列表
export const getCollectConfigs = (params) => {
  return request.get('/collect/configs', { params })
}

// 获取采集配置详情
export const getCollectConfig = (id) => {
  return request.get(`/collect/config/${id}`)
}

// 创建采集配置
export const createCollectConfig = (data) => {
  return request.post('/collect/config', data)
}

// 更新采集配置
export const updateCollectConfig = (id, data) => {
  return request.put(`/collect/config/${id}`, data)
}

// 删除采集配置
export const deleteCollectConfig = (id) => {
  return request.delete(`/collect/config/${id}`)
}

// 执行采集任务
export const executeCollect = (configId, params) => {
  return request.post(`/collect/execute/${configId}`, null, { params })
}

// 获取启用的配置列表
export const getEnabledConfigs = () => {
  return request.get('/collect/configs/enabled')
}

// 获取采集数据列表
export const getCollectData = (params) => {
  return request.get('/collect/data', { params })
}
