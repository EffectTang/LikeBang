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
    // 微信扫码登录：取登录小程序码（{ sceneId, qrBase64, expiresIn }）
    async scanQr() {
      const res = await request.get('/user/auth/scan-qr')
      return res.data
    },
    // 轮询扫码状态（{ status, token, userInfo }），CONFIRMED 时一次性领取 token
    async scanStatus(sceneId) {
      const res = await request.get('/user/auth/scan-status', { params: { sceneId } })
      return res.data
    },
    // 找回密码：申请向登记邮箱发送重置链接邮件（后端防探测，始终成功；频控错误由 axios 拦截器提示）
    async requestPasswordReset(email) {
      await request.post('/user/auth/password-reset/request', { email })
    },
    // 完成密码重置：凭邮件链接中的一次性令牌设新密码，成功后令牌即刻失效
    async completePasswordReset(token, newPassword) {
      await request.post('/user/auth/password-reset/complete', { token, newPassword })
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
    // 自助修改资料：后端返回最新用户信息，直接刷新缓存，免二次 fetchMe
    async updateProfile(form) {
      const res = await request.put('/user/auth/profile', form)
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
