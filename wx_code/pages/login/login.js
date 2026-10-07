const api = require('../../utils/api')
const auth = require('../../utils/auth')

Page({
  data: {
    mode: 'login',
    submitting: false,
    wxSubmitting: false,
    form: { username: '', password: '', nickname: '', email: '', phone: '' },
    // 新用户首次微信登录后的一次性「完善资料」引导（可跳过；登录页本身不再平铺头像昵称框）
    showFill: false,
    fillNickname: '',
    fillAvatarTemp: '',   // chooseAvatar 临时本地路径，保存时再上传
    fillAvatarPrev: '',   // 预览用地址（临时路径可直接渲染）
    fillSaving: false
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
    this.setData({ submitting: false, showFill: false })
    // 从其它页 navigateTo 跳来 → 回原页继续未完成动作（如 detail/item 登录墙后回看）；
    // 直接进登录页或无可回退 → switchTab 回发现首页。tab 页当前面已包含 login 且回退时也能安全降级。
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.switchTab({ url: '/pages/index/index' })
    }
  },

  // 微信一键登录：wx.login 取 code → 后端 jscode2session 换 openid，命中登录/未命中静默注册 → 同款 JWT
  // 新用户（data.newUser）登录成功后弹一次“完善资料”引导（可跳过），老用户直接进
  wxAuthLogin() {
    if (this.data.wxSubmitting) return
    this.setData({ wxSubmitting: true })
    wx.login({
      success: (res) => {
        if (!res.code) {
          this.setData({ wxSubmitting: false })
          wx.showToast({ title: '微信登录失败，请重试', icon: 'none' })
          return
        }
        api.wxLogin({ code: res.code })
          .then((data) => {
            auth.setAuth(data)
            if (data.newUser) {
              // 已拿到 token（上传可用）；预填后端随机昵称，用户可改可跳
              this.setData({
                wxSubmitting: false,
                showFill: true,
                fillNickname: (data.userInfo && data.userInfo.nickname) || '',
                fillAvatarTemp: '',
                fillAvatarPrev: ''
              })
              return
            }
            wx.showToast({ title: '登录成功', icon: 'success' })
            setTimeout(() => this.goAfterLogin(), 600)
          })
          .catch(() => {
            this.setData({ wxSubmitting: false })
          })
      },
      fail: () => {
        this.setData({ wxSubmitting: false })
        wx.showToast({ title: '微信登录失败，请重试', icon: 'none' })
      }
    })
  },

  // ==== 新用户一次性「头像昵称填写」引导 ====（仅新用户首登触发，微信收回 getUserProfile 后只能靠用户主动填写）
  onFillNickname(e) { this.setData({ fillNickname: e.detail.value }) },

  // chooseAvatar 先只暂存临时本地路径与预览；真正上传在点“保存”时进行（已拿到 token）
  onFillChooseAvatar(e) {
    const fp = e.detail.avatarUrl
    if (!fp) return
    this.setData({ fillAvatarTemp: fp, fillAvatarPrev: fp })
  },

  // 保存：有头像先上传换站内路径再 updateProfile；无头像仅改昵称。均不传 intro 避免清空已有简介
  saveFill() {
    if (this.data.fillSaving) return
    const nickname = (this.data.fillNickname || '').trim()
    if (!nickname) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    this.setData({ fillSaving: true })
    const done = (u) => {
      auth.setUser(u)
      wx.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => this.goAfterLogin(), 600)
    }
    const fail = () => this.setData({ fillSaving: false })
    if (this.data.fillAvatarTemp) {
      api.uploadImage(this.data.fillAvatarTemp)
        .then((path) => api.updateProfile({ nickname, avatarUrl: path }))
        .then(done)
        .catch(fail)
    } else {
      api.updateProfile({ nickname })
        .then(done)
        .catch(fail)
    }
  },

  // 跳过：不惩罚用户，直接以随机昵称/无头像进入（之后可在“我的资料”修改）
  skipFill() {
    this.setData({ showFill: false })
    this.goAfterLogin()
  }
})
