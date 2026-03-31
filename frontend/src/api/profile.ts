import request from '@/api/http'
import type { ApiResult } from '@/types/common'
import type { UserProfile } from '@/types/auth'

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export function getProfile() {
  return request.get<any, ApiResult<UserProfile>>('/user/profile')
}

export function updateProfile(data: Partial<UserProfile>) {
  return request.put<any, ApiResult<UserProfile>>('/user/profile', data)
}

export function changePassword(data: ChangePasswordPayload) {
  return request.put<any, ApiResult<UserProfile>>('/user/password', data)
}

export function uploadProfileAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResult<UserProfile>>('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
