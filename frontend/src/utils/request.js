import axios from 'axios'
import { ElMessage } from 'element-plus'

const LOGIN_PATH = '/login'
const AUTH_EXPIRED_HINTS = [
  '未登录',
  '登录已失效',
  '认证失败',
  'token',
  'Token',
  'Full authentication is required'
]

let redirectingToLogin = false

const shouldSilenceMessage = (config) => config?.silent === true

const service = axios.create({
  baseURL: '/api',
  timeout: 5000
})

const clearAuthState = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userPermissions')
}

const isLoginPage = () => window.location.pathname === LOGIN_PATH

const buildLoginRedirectUrl = () => {
  if (isLoginPage()) {
    return LOGIN_PATH
  }

  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
  const params = new URLSearchParams()

  if (currentPath && currentPath !== LOGIN_PATH) {
    params.set('redirect', currentPath)
  }

  const query = params.toString()
  return query ? `${LOGIN_PATH}?${query}` : LOGIN_PATH
}

const redirectToLogin = (message, silent = false) => {
  clearAuthState()

  if (!silent && message) {
    ElMessage.error(message)
  }

  if (redirectingToLogin) {
    return
  }

  if (isLoginPage()) {
    return
  }

  redirectingToLogin = true
  window.location.replace(buildLoginRedirectUrl())
}

const isAuthLikeFailure = (status, data) => {
  if (status === 401) {
    return true
  }

  if (status !== 403) {
    return false
  }

  if (!localStorage.getItem('token')) {
    return false
  }

  const message = String(data?.message || '')
  if (!message) {
    return true
  }

  return AUTH_EXPIRED_HINTS.some((hint) => message.includes(hint))
}

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    const silent = shouldSilenceMessage(response.config)

    if (res.code === 401) {
      redirectToLogin(res.message || '登录已失效，请重新登录', silent)
      return Promise.reject(new Error(res.message || 'Unauthorized'))
    }

    if (res.code !== 200) {
      if (!silent) {
        if (res.data && typeof res.data === 'object') {
          const errorMessages = Object.values(res.data).flat().join(', ')
          ElMessage.error(errorMessages || res.message || '请求失败')
        } else {
          ElMessage.error(res.message || '请求失败')
        }
      }

      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res
  },
  (error) => {
    console.error('响应错误:', error)
    const silent = shouldSilenceMessage(error.config)

    if (error.response) {
      const { status, data } = error.response

      if (isAuthLikeFailure(status, data)) {
        redirectToLogin(data?.message || '登录已失效，请重新登录', silent)
        return Promise.reject(error)
      }

      if (!silent) {
        if (data && data.data && typeof data.data === 'object') {
          const errorMessages = Object.values(data.data).flat().join(', ')
          ElMessage.error(errorMessages || data.message || '请求失败')
        } else {
          switch (status) {
            case 400:
              ElMessage.error(data?.message || '请求参数错误')
              break
            case 403:
              ElMessage.error(data?.message || '权限不足，无法访问')
              break
            case 404:
              ElMessage.error(data?.message || '请求资源不存在')
              break
            case 500:
              ElMessage.error(data?.message || '服务器内部错误')
              break
            default:
              ElMessage.error(data?.message || '请求失败')
          }
        }
      }
    } else if (error.request) {
      if (!silent) {
        ElMessage.error('网络错误，请检查网络连接')
      }
    } else if (!silent) {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default service
