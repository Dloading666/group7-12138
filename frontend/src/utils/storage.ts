const TOKEN_KEY = 'rpa_token'
const USER_KEY = 'rpa_user'
const PERMISSION_KEY = 'rpa_permissions'
const MENU_KEY = 'rpa_menu_tree'

export const storage = {
  getToken: () => localStorage.getItem(TOKEN_KEY) || '',
  setToken: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clearToken: () => localStorage.removeItem(TOKEN_KEY),
  getUser: <T = unknown>() => readJson<T>(USER_KEY),
  setUser: (value: unknown) => localStorage.setItem(USER_KEY, JSON.stringify(value)),
  clearUser: () => localStorage.removeItem(USER_KEY),
  getPermissions: () => readStringArray(PERMISSION_KEY),
  setPermissions: (value: string[]) => localStorage.setItem(PERMISSION_KEY, JSON.stringify(value)),
  clearPermissions: () => localStorage.removeItem(PERMISSION_KEY),
  getMenuTree: <T = unknown>() => readJson<T[]>(MENU_KEY) || [],
  setMenuTree: (value: unknown) => localStorage.setItem(MENU_KEY, JSON.stringify(value)),
  clearMenuTree: () => localStorage.removeItem(MENU_KEY),
  clearAll: () => {
    storage.clearToken()
    storage.clearUser()
    storage.clearPermissions()
    storage.clearMenuTree()
  }
}

function readJson<T>(key: string): T | null {
  const value = localStorage.getItem(key)
  if (!value) return null
  try {
    return JSON.parse(value) as T
  } catch {
    return null
  }
}

function readStringArray(key: string): string[] {
  const value = readJson<string[]>(key)
  return Array.isArray(value) ? value : []
}

