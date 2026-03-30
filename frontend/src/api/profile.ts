import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { UserProfile } from '@/types/auth'

export function getProfile() {
  return request.get<any, ApiResult<UserProfile>>('/user/profile')
}

export function updateProfile(data: Partial<UserProfile>) {
  return request.put<any, ApiResult<UserProfile>>('/user/profile', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<any, ApiResult<void>>('/user/password', data)
}

