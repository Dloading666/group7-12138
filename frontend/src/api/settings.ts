import quietRequest from '@/api/quiet'
import type { ApiResult } from '@/types/common'
import type { BasicSettingsState, NotificationSettingsState } from '@/types/domain'

export function getBasicSettings() {
  return quietRequest.get<any, ApiResult<BasicSettingsState>>('/settings/basic')
}

export function saveBasicSettings(data: BasicSettingsState) {
  return quietRequest.put<any, ApiResult<BasicSettingsState>>('/settings/basic', data)
}

export function getNotificationSettings() {
  return quietRequest.get<any, ApiResult<NotificationSettingsState>>('/settings/notification')
}

export function saveNotificationSettings(data: NotificationSettingsState) {
  return quietRequest.put<any, ApiResult<NotificationSettingsState>>('/settings/notification', data)
}
