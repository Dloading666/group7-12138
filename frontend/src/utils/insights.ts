import type { DashboardOverview, ExecutionLogItem, RobotItem, TaskItem } from '@/types/domain'

export function buildExecutionLogs(tasks: TaskItem[], robots: RobotItem[]) {
  const robotMap = new Map(robots.map((robot) => [robot.id, robot]))
  const logs: ExecutionLogItem[] = []

  tasks.forEach((task, index) => {
    const robot = task.robotId ? robotMap.get(task.robotId) : robots.find((item) => item.name === task.robotName)
    const baseTime = task.endTime || task.startTime || task.createTime || new Date().toISOString()
    logs.push({
      id: task.id * 10 + 1,
      time: baseTime,
      level: task.status === 'failed' ? 'ERROR' : task.status === 'running' ? 'WARN' : 'INFO',
      source: 'task',
      title: `${task.name} - ${task.status}`,
      message: buildTaskMessage(task),
      taskId: task.taskId,
      taskName: task.name,
      robotId: robot?.id,
      robotName: robot?.name,
      duration: task.startTime && task.endTime ? diffMinutes(task.startTime, task.endTime) : undefined
    })

    if (robot) {
      logs.push({
        id: task.id * 10 + 2,
        time: robot.lastHeartbeat || baseTime,
        level: robot.status === 'offline' ? 'ERROR' : robot.status === 'busy' ? 'WARN' : 'INFO',
        source: 'robot',
        title: `${robot.name} 心跳`,
        message: `${robot.name} 当前状态 ${robot.status}，任务数 ${robot.taskCount ?? 0}`,
        robotId: robot.id,
        robotName: robot.name
      })
    }
  })

  return logs
    .sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime())
    .slice(0, 120)
}

export function summarizeLogs(logs: ExecutionLogItem[]) {
  const summary = {
    total: logs.length,
    info: 0,
    warn: 0,
    error: 0
  }
  logs.forEach((log) => {
    summary[log.level.toLowerCase() as 'info' | 'warn' | 'error'] += 1
  })
  return summary
}

export function buildTaskStatusSeries(tasks: TaskItem[]) {
  const map = new Map<TaskItem['status'], number>()
  tasks.forEach((task) => {
    map.set(task.status, (map.get(task.status) || 0) + 1)
  })
  return [
    { name: '待执行', value: map.get('pending') || 0 },
    { name: '执行中', value: map.get('running') || 0 },
    { name: '已完成', value: map.get('completed') || 0 },
    { name: '已停止', value: map.get('stopped') || 0 },
    { name: '失败', value: map.get('failed') || 0 }
  ]
}

export function buildRobotStatusSeries(robots: RobotItem[]) {
  const map = new Map<RobotItem['status'], number>()
  robots.forEach((robot) => {
    map.set(robot.status, (map.get(robot.status) || 0) + 1)
  })
  return [
    { name: '在线', value: map.get('online') || 0 },
    { name: '忙碌', value: map.get('busy') || 0 },
    { name: '离线', value: map.get('offline') || 0 },
    { name: '禁用', value: map.get('disabled') || 0 }
  ]
}

export function buildTaskTypeSeries(tasks: TaskItem[]) {
  const map = new Map<string, number>()
  tasks.forEach((task) => {
    map.set(task.type, (map.get(task.type) || 0) + 1)
  })
  return Array.from(map.entries()).map(([name, value]) => ({ name, value }))
}

export function buildTaskTrend(tasks: TaskItem[], days = 7) {
  const buckets = new Map<string, number>()
  const labels: string[] = []
  for (let i = days - 1; i >= 0; i -= 1) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    const key = date.toISOString().slice(0, 10)
    labels.push(key.slice(5))
    buckets.set(key, 0)
  }

  tasks.forEach((task) => {
    const day = (task.createTime || task.startTime || task.endTime || '').slice(0, 10)
    if (buckets.has(day)) {
      buckets.set(day, (buckets.get(day) || 0) + 1)
    }
  })

  return {
    labels,
    values: Array.from(buckets.values())
  }
}

export function buildTaskSummary(overview: DashboardOverview, tasks: TaskItem[], robots: RobotItem[]) {
  const total = tasks.length || overview.totalTasks
  const running = tasks.filter((task) => task.status === 'running').length || overview.runningTasks
  const robotsOnline = robots.filter((robot) => robot.status === 'online').length
  const success = tasks.length
    ? Math.round((tasks.filter((task) => task.status === 'completed').length / tasks.length) * 1000) / 10
    : overview.successRate

  return {
    total,
    running,
    robotsOnline,
    success
  }
}

function buildTaskMessage(task: TaskItem) {
  if (task.status === 'failed') {
    return task.errorMessage || `${task.name} 执行失败`
  }
  if (task.status === 'running') {
    return `${task.name} 正在 ${task.progress}% 运行中`
  }
  if (task.status === 'completed') {
    return task.result || `${task.name} 已成功完成`
  }
  if (task.status === 'stopped') {
    return `${task.name} 已停止执行`
  }
  return `${task.name} 等待调度`
}

function diffMinutes(start: string, end: string) {
  const startTime = new Date(start).getTime()
  const endTime = new Date(end).getTime()
  if (Number.isNaN(startTime) || Number.isNaN(endTime) || endTime < startTime) return undefined
  return Math.max(1, Math.round((endTime - startTime) / 60000))
}
