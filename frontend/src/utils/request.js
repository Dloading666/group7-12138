import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: 'http://localhost:8080/api', // 后端API地址
  timeout: 5000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 添加Authorization请求头
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 判断响应状态码
    if (res.code !== 200) {
      // 处理验证错误（字段级错误）
      if (res.data && typeof res.data === 'object') {
        const errorMessages = Object.values(res.data).flat().join(', ')
        ElMessage.error(errorMessages || res.message || '请求失败')
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      
      // 401: Token过期或未登录
      if (res.code === 401) {
        ElMessage.error('登录已过期，请重新登录')
        // 清除token
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        // 跳转到登录页
        window.location.href = '/login'
      }
      
      return Promise.reject(new Error(res.message || 'Error'))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    // 处理HTTP错误状态码
    if (error.response) {
      const data = error.response.data
      
      // 处理验证错误（字段级错误）
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
            ElMessage.error('请求资源不存在')
            break
          case 500:
            ElMessage.error('服务器内部错误')
            break
          default:
            ElMessage.error(data?.message || '请求失败')
        }
      }
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接')
    } else {
      ElMessage.error('请求配置错误')
    }
    
    return Promise.reject(error)
  }
)

export default service
