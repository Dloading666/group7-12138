import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { DashboardOverview } from '@/types/domain'

export function getDashboardOverview() {
  return request.get<any, ApiResult<DashboardOverview>>('/dashboard/overview')
}

