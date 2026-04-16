import axios from 'axios'
import { ElMessage } from 'element-plus'

const shouldSilenceMessage = (config) => config?.silent === true

const service = axios.create({
  baseURL: '/api',
  timeout: 5000
})

service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
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

    if (res.code !== 200) {
      if (!shouldSilenceMessage(response.config)) {
        if (res.data && typeof res.data === 'object') {
          const errorMessages = Object.values(res.data).flat().join(', ')
          ElMessage.error(errorMessages || res.message || '请求失败')
        } else {
          ElMessage.error(res.message || '请求失败')
        }
      }

      if (res.code === 401) {
        if (!shouldSilenceMessage(response.config)) {
          ElMessage.error('登录已过期，请重新登录')
        }
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res
  },
  (error) => {
    console.error('响应错误:', error)
    const silent = shouldSilenceMessage(error.config)

    if (error.response) {
      const data = error.response.data

      if (!silent) {
        if (data && data.data && typeof data.data === 'object') {
          const errorMessages = Object.values(data.data).flat().join(', ')
          ElMessage.error(errorMessages || data.message || '请求失败')
        } else {
          switch (error.response.status) {
            case 400:
              ElMessage.error(data?.message || '请求参数错误')
              break
            case 401:
              ElMessage.error('未授权，请重新登录')
              localStorage.removeItem('token')
              localStorage.removeItem('userInfo')
              window.location.href = '/login'
              break
            case 403:
              ElMessage.error('拒绝访问')
              break
            case 404:
              ElMessage.error(data?.message || '请求资源不存在')
              break
            case 500:
              ElMessage.error('服务器内部错误')
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