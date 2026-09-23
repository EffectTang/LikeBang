import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('lb_token') || '',
    userInfo: JSON.parse(localStorage.getItem('lb_user') || 'null')
  }),
  getters: {
    isLogin: state => !!state.token,
    // 角色：0普通用户，1管理员，2运营管理员（仅用于前端渲染控制，安全边界在后端）
    isAdmin: state => state.userInfo?.role === 1,
    // 用户管理能力：管理员 + 被授权的运营管理员
    canManageUsers: state => [1, 2].includes(state.userInfo?.role)
  },
  actions: {
    async login(form) {
      const res = await request.post('/user/auth/login', form)
      this.setAuth(res.data)
      return res.data
    },
    async register(form) {
      const res = await request.post('/user/auth/register', form)
      this.setAuth(res.data)
      return res.data
    },
    setAuth({ token, userInfo }) {
      this.token = token
      this.userInfo = userInfo
      localStorage.setItem('lb_token', token)
      localStorage.setItem('lb_user', JSON.stringify(userInfo))
    },
    async fetchMe() {
      const res = await request.get('/user/auth/me')
      this.userInfo = res.data
      localStorage.setItem('lb_user', JSON.stringify(res.data))
      return res.data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('lb_token')
      localStorage.removeItem('lb_user')
    }
  }
})
