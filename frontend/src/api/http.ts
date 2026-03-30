import axios, { AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { storage } from '@/utils/storage'

export type AppAxiosError = AxiosError<any> & {
  __toastHandled?: boolean
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = storage.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      return payload
    }
    return { code: 200, message: '', data: payload }
  },
  (error: AxiosError<any>) => {
    const handledError = error as AppAxiosError
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '请求失败'

    if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认后端已启动')
    } else if (status === 401) {
      ElMessage.error('登录状态已失效，请重新登录')
      storage.clearAll()
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('无权限访问')
    } else if (status === 404) {
      ElMessage.error('接口不存在')
    } else if (status && status >= 500) {
      ElMessage.error('服务暂时不可用')
    } else {
      ElMessage.error(message)
    }

    handledError.__toastHandled = true
    return Promise.reject(handledError)
  }
)

export default service
