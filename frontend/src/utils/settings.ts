import type { BasicSettingsState, NotificationSettingsState } from '@/types/domain'

const BASIC_SETTINGS_KEY = 'rpa_basic_settings'
const NOTIFICATION_SETTINGS_KEY = 'rpa_notification_settings'

export const defaultBasicSettings: BasicSettingsState = {
  systemName: 'RPA 管理系统',
  systemSubtitle: '自动化任务与执行监控平台',
  companyName: '示例科技有限公司',
  supportEmail: 'support@example.com',
  supportPhone: '400-000-0000',
  loginNotice: '请使用授权账号登录系统。',
  maintenanceMode: false
}

export const defaultNotificationSettings: NotificationSettingsState = {
  emailEnabled: true,
  emailHost: 'smtp.example.com',
  emailPort: 587,
  emailUsername: 'ops@example.com',
  emailFrom: 'noreply@example.com',
  webhookEnabled: false,
  taskFailureAlert: true,
  robotOfflineAlert: true,
  webhookUrl: ''
}

export function loadBasicSettings() {
  return readSettings<BasicSettingsState>(BASIC_SETTINGS_KEY, defaultBasicSettings)
}

export function saveBasicSettings(value: BasicSettingsState) {
  writeSettings(BASIC_SETTINGS_KEY, value)
}

export function loadNotificationSettings() {
  return readSettings<NotificationSettingsState>(NOTIFICATION_SETTINGS_KEY, defaultNotificationSettings)
}

export function saveNotificationSettings(value: NotificationSettingsState) {
  writeSettings(NOTIFICATION_SETTINGS_KEY, value)
}

function readSettings<T>(key: string, fallback: T) {
  const raw = localStorage.getItem(key)
  if (!raw) return fallback
  try {
    return { ...fallback, ...(JSON.parse(raw) as Partial<T>) }
  } catch {
    return fallback
  }
}

function writeSettings(key: string, value: unknown) {
  localStorage.setItem(key, JSON.stringify(value))
}
