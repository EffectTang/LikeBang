const api = require('../../utils/api')
const auth = require('../../utils/auth')

Page({
  data: {
    scene: '',
    invalid: false,     // 二维码无效/过期（缺 scene）
    logged: false,      // 小程序当前是否已登录
    nickname: '',
    confirming: false,
    done: false         // 已确认成功
  },

  onLoad(options) {
    // 扫码进入时 scene 经 URL 编码，需 decodeURIComponent 还原
    const scene = options.scene ? decodeURIComponent(options.scene) : ''
    if (!scene) {
      this.setData({ invalid: true })
      return
    }
    this.setData({ scene })
    this.refreshLogin()
  },

  // 从登录页返回后重读登录态（onLoad 不再触发），确认按钮据此解锁
  onShow() {
    if (!this.data.invalid) this.refreshLogin()
  },

  refreshLogin() {
    const user = auth.getUser()
    this.setData({ logged: auth.isLogin(), nickname: (user && user.nickname) || '' })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  confirm() {
    const { scene, logged, confirming, done } = this.data
    // 未登录不发起（否则带空 token 会被 401 打回登录页）
    if (!logged || confirming || done) return
    this.setData({ confirming: true })
    api.scanConfirm(scene)
      .then(() => {
        this.setData({ confirming: false, done: true })
        wx.showToast({ title: '已确认，请在电脑端继续', icon: 'none', duration: 3000 })
      })
      .catch(() => {
        // 失败提示（二维码已失效/登录态过期等）由 request.js 统一 toast
        this.setData({ confirming: false })
      })
  }
})
