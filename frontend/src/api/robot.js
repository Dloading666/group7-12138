import request from '../utils/request.js'

/**
 * 获取机器人列表（分页）
 */
export function getRobotList(params) {
  return request({
    url: '/robots',
    method: 'get',
    params
  })
}

/**
 * 获取所有机器人
 */
export function getAllRobots() {
  return request({
    url: '/robots/all',
    method: 'get'
  })
}

/**
 * 获取机器人详情
 */
export function getRobotById(id) {
  return request({
    url: `/robots/${id}`,
    method: 'get'
  })
}

/**
 * 创建机器人
 */
export function createRobot(data) {
  return request({
    url: '/robots',
    method: 'post',
    data
  })
}

/**
 * 更新机器人
 */
export function updateRobot(id, data) {
  return request({
    url: `/robots/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除机器人
 */
export function deleteRobot(id) {
  return request({
    url: `/robots/${id}`,
    method: 'delete'
  })
}

/**
 * 启动机器人
 */
export function startRobot(id) {
  return request({
    url: `/robots/${id}/start`,
    method: 'post'
  })
}

/**
 * 停止机器人
 */
export function stopRobot(id) {
  return request({
    url: `/robots/${id}/stop`,
    method: 'post'
  })
}

/**
 * 获取机器人统计
 */
export function getRobotStats() {
  return request({
    url: '/robots/stats',
    method: 'get'
  })
}
