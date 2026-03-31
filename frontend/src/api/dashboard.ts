import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { DashboardOverview, TaskItem } from '@/types/domain'
import { mapTaskItem } from '@/utils/admin'

interface BackendTrendPoint {
  name?: string
  executionCount?: number
  successCount?: number
}

interface BackendStatusCount {
  name?: string
  value?: number
}

interface BackendRecentTask {
  id?: number
  taskNo?: string
  name?: string
  type?: string
  status?: string
  progress?: number
  createdAt?: string
}

interface BackendDashboardOverview {
  totalTasks?: number
  runningTasks?: number
  totalRobots?: number
  robotCount?: number
  successRate?: number
  executionTrend?: BackendTrendPoint[]
  trend?: number[]
  successTrend?: number[]
  taskStatusDistribution?: BackendStatusCount[]
  statusDistribution?: Array<{ name: string; value: number }>
  recentTasks?: BackendRecentTask[]
}

function normalizeStatusName(name?: string) {
  const value = String(name || '').toUpperCase()
  if (value === 'RUNNING') return '执行中'
  if (value === 'COMPLETED') return '已完成'
  if (value === 'FAILED') return '失败'
  if (value === 'STOPPED') return '已停止'
  if (value === 'PENDING') return '待执行'
  return String(name || '')
}

function normalizeRecentTasks(items?: BackendRecentTask[]): TaskItem[] {
  return Array.isArray(items)
    ? items.map((item) =>
        mapTaskItem({
          id: item.id,
          taskNo: item.taskNo,
          name: item.name,
          type: item.type,
          status: item.status,
          progress: item.progress,
          priority: 'MEDIUM',
          createdAt: item.createdAt
        })
      )
    : []
}

function normalizeOverview(data?: BackendDashboardOverview): DashboardOverview {
  const trendPoints = Array.isArray(data?.executionTrend) ? data.executionTrend : []
  const statusDistribution = Array.isArray(data?.taskStatusDistribution)
    ? data.taskStatusDistribution.map((item) => ({
        name: normalizeStatusName(item.name),
        value: Number(item.value || 0)
      }))
    : Array.isArray(data?.statusDistribution)
      ? data.statusDistribution.map((item) => ({
          name: normalizeStatusName(item.name),
          value: Number(item.value || 0)
        }))
      : []

  return {
    totalTasks: Number(data?.totalTasks || 0),
    runningTasks: Number(data?.runningTasks || 0),
    robotCount: Number(data?.robotCount ?? data?.totalRobots ?? 0),
    successRate: Number(data?.successRate || 0),
    trend: Array.isArray(data?.trend) && data?.trend.length
      ? data.trend.map((item) => Number(item || 0))
      : trendPoints.map((item) => Number(item.executionCount || 0)),
    successTrend: Array.isArray(data?.successTrend) && data?.successTrend.length
      ? data.successTrend.map((item) => Number(item || 0))
      : trendPoints.map((item) => Number(item.successCount || 0)),
    statusDistribution,
    recentTasks: normalizeRecentTasks(data?.recentTasks)
  }
}

export function getDashboardOverview() {
  return request
    .get<any, ApiResult<BackendDashboardOverview>>('/dashboard/overview')
    .then((res) => ({
      ...res,
      data: normalizeOverview(res.data)
    }))
}
