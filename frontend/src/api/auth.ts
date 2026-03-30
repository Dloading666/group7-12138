import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { CurrentUserResult, LoginPayload, LoginResult } from '@/types/auth'

export function loginApi(data: LoginPayload) {
  return request.post<any, ApiResult<LoginResult>>('/auth/login', data)
}

export function meApi() {
  return request.get<any, ApiResult<CurrentUserResult>>('/auth/me')
}

export function logoutApi() {
  return request.post<any, ApiResult<void>>('/auth/logout')
}

