import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('lb_token') || '',
    userInfo: JSON.parse(localStorage.getItem('lb_user') || 'null')
  }),
  getters: {
    isLogin: state => !!state.token
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
