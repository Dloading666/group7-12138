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
  timeout: 10000
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
  return params.toString() ? `${LOGIN_PATH}?${params.toString()}` : LOGIN_PATH
}

const redirectToLogin = (message, silent = false) => {
  clearAuthState()

  if (!silent && message) {
    ElMessage.error(message)
  }

  if (redirectingToLogin || isLoginPage()) {
    return
  }

  redirectingToLogin = true
  window.location.replace(buildLoginRedirectUrl())
}

const extractErrorMessage = (payload) => {
  if (!payload) {
    return ''
  }
  if (typeof payload === 'string') {
    return payload
  }
  if (payload.message) {
    return String(payload.message)
  }
  if (payload.data && typeof payload.data === 'object') {
    return Object.values(payload.data)
      .flat()
      .map((item) => String(item))
      .join(', ')
  }
  return ''
}

const isAuthLikeFailure = (status, data) => {
  if (status === 401) {
    return true
  }
  if (status !== 403 || !localStorage.getItem('token')) {
    return false
  }

  const message = extractErrorMessage(data)
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
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    const silent = shouldSilenceMessage(response.config)

    if (res?.code === 401) {
      redirectToLogin(res.message || '登录已失效，请重新登录', silent)
      return Promise.reject(new Error(res.message || 'Unauthorized'))
    }

    if (res?.code !== 200) {
      if (!silent) {
        ElMessage.error(extractErrorMessage(res) || '请求失败')
      }
      return Promise.reject(new Error(res?.message || 'Request failed'))
    }

    return res
  },
  (error) => {
    const silent = shouldSilenceMessage(error.config)

    if (error.response) {
      const { status, data } = error.response

      if (isAuthLikeFailure(status, data)) {
        redirectToLogin(extractErrorMessage(data) || '登录已失效，请重新登录', silent)
        return Promise.reject(error)
      }

      if (!silent) {
        const message = extractErrorMessage(data)
        if (message) {
          ElMessage.error(message)
        } else if (status === 400) {
          ElMessage.error('请求参数错误')
        } else if (status === 403) {
          ElMessage.error('权限不足，无法访问')
        } else if (status === 404) {
          ElMessage.error('请求资源不存在')
        } else if (status === 500) {
          ElMessage.error('服务端内部错误')
        } else {
          ElMessage.error('请求失败')
        }
      }
    } else if (error.request) {
      if (!silent) {
        ElMessage.error('网络错误，请检查连接')
      }
    } else if (!silent) {
      ElMessage.error(error.message || '请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default service
