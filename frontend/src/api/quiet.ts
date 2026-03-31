import axios, { AxiosError } from 'axios'
import { storage } from '@/utils/storage'

const quietService = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
  timeout: 15000
})

quietService.interceptors.request.use((config) => {
  const token = storage.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

quietService.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      return payload
    }
    return { code: 200, message: '', data: payload }
  },
  (error: AxiosError<any>) => Promise.reject(error)
)

export default quietService
