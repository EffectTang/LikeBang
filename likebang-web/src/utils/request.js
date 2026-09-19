import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：自动携带 token
service.interceptors.request.use(config => {
  const token = localStorage.getItem('lb_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function redirectToLogin() {
  localStorage.removeItem('lb_token')
  localStorage.removeItem('lb_user')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    // HTTP 401：token 无效或过期，跳转登录
    if (error.response?.status === 401) {
      ElMessage.error('登录状态已失效，请重新登录')
      redirectToLogin()
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
