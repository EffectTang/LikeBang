const api = require('../../utils/api')
const auth = require('../../utils/auth')

Page({
  data: {
    mode: 'login',
    submitting: false,
    form: { username: '', password: '', nickname: '', email: '', phone: '' }
  },

  switchMode(e) {
    this.setData({ mode: e.currentTarget.dataset.mode })
  },

  onInput(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ ['form.' + key]: e.detail.value })
  },

  submit() {
    const { mode, form, submitting } = this.data
    if (submitting) return
    if (!form.username || !form.password) {
      wx.showToast({ title: '请填写用户名和密码', icon: 'none' })
      return
    }
    if (mode === 'register' && (!form.nickname || !form.email)) {
      wx.showToast({ title: '请填写昵称和邮箱', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    const req = mode === 'login'
      ? api.login({ username: form.username, password: form.password })
      : api.register(form)

    req.then((data) => {
      // 后端登录/注册统一返回 { token, userInfo }
      auth.setAuth(data)
      wx.showToast({ title: mode === 'login' ? '登录成功' : '注册成功', icon: 'success' })
      setTimeout(() => this.goAfterLogin(), 600)
    }).catch(() => {
      this.setData({ submitting: false })
    })
  },

  goAfterLogin() {
    this.setData({ submitting: false })
    // 从别的页跳来登录 → 返回原页；直接进登录页 → 回发现首页
    if (getCurrentPages().length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/index/index' })
    }
  }
})
