import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：携带 token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一解析后端 R<T>
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 若返回体不是标准 R 结构，直接返回
    if (res === undefined || res === null || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === 0) {
      return res.data
    }
    ElMessage.error(res.msg || '请求失败')
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.replace('/login')
    }
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.replace('/login')
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
