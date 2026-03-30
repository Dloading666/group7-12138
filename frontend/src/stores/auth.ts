import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { loginApi, logoutApi, meApi } from '@/api/auth'
import { appNavigation } from '@/config/navigation'
import type { AuthMenuNode, CurrentUserResult, LoginPayload, LoginResult, UserProfile } from '@/types/auth'
import { filterMenuTree } from '@/utils/menu'
import { storage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(storage.getToken())
  const user = ref<UserProfile | null>(storage.getUser<UserProfile>())
  const permissionCodes = ref<string[]>(storage.getPermissions())
  const menuTree = ref<AuthMenuNode[]>(storage.getMenuTree<AuthMenuNode>())
  const hydrated = ref(false)
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN' || user.value?.username === 'admin')
  const roleName = computed(() => user.value?.roleName || (isAdmin.value ? '管理员' : '普通用户'))
  const displayName = computed(() => user.value?.realName || user.value?.username || '访客')

  function hydrate() {
    if (hydrated.value) return
    hydrated.value = true
    token.value = storage.getToken()
    user.value = storage.getUser<UserProfile>()
    permissionCodes.value = storage.getPermissions()
    menuTree.value = storage.getMenuTree<AuthMenuNode>()

    if (!menuTree.value.length) {
      menuTree.value = filterMenuTree(appNavigation, permissionCodes.value)
    }
  }

  function hasPermission(code?: string) {
    if (!code) return true
    if (isAdmin.value) return true
    return permissionCodes.value.includes(code)
  }

  function hasAnyPermission(codes?: string[]) {
    if (!codes || codes.length === 0) return true
    return codes.some((code) => hasPermission(code))
  }

  async function login(payload: LoginPayload) {
    const res = await loginApi(payload)
    if (res.code !== 200) throw new Error(res.message || '登录失败')
    applyAuth(res.data as LoginResult)
    return res.data as LoginResult
  }

  async function fetchCurrentUser() {
    if (!token.value) return null
    loading.value = true
    try {
      const res = await meApi()
      if (res.code !== 200) throw new Error(res.message || '获取用户信息失败')
      applyCurrentUser(res.data as CurrentUserResult)
      return res.data as CurrentUserResult
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearAuth()
    }
  }

  function applyAuth(payload: LoginResult) {
    token.value = payload.token
    user.value = payload.user
    permissionCodes.value = payload.permissionCodes || []
    menuTree.value = normalizeMenuTree(payload.menuTree, payload.permissionCodes || [])
    persist()
  }

  function applyCurrentUser(payload: CurrentUserResult) {
    user.value = payload.user
    permissionCodes.value = payload.permissionCodes || []
    menuTree.value = normalizeMenuTree(payload.menuTree, payload.permissionCodes || [])
    persist()
  }

  function normalizeMenuTree(tree: AuthMenuNode[], codes: string[]) {
    const source = Array.isArray(tree) && tree.length ? tree : appNavigation
    return filterMenuTree(source, codes)
  }

  function persist() {
    if (token.value) storage.setToken(token.value)
    if (user.value) storage.setUser(user.value)
    storage.setPermissions(permissionCodes.value)
    storage.setMenuTree(menuTree.value)
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    permissionCodes.value = []
    menuTree.value = []
    storage.clearAll()
  }

  return {
    token,
    user,
    permissionCodes,
    menuTree,
    hydrated,
    loading,
    isLoggedIn,
    isAdmin,
    roleName,
    displayName,
    hydrate,
    login,
    fetchCurrentUser,
    logout,
    hasPermission,
    hasAnyPermission,
    clearAuth
  }
})

