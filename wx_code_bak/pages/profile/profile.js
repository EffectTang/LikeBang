const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

Page({
  data: {
    username: '',
    showUsername: false, // wx_ 技术串不展示（微信账号无真用户名），仅账密注册用户可见
    showRole: false,     // 普通用户 role=0 无价值，仅管理员/运营可见
    roleLabel: '',
    nickname: '',
    intro: '',
    avatarUrl: '',     // 提交用原始值（相对路径 /uploads/xxx 或 http(s) 外链），空串=清空
    avatarPrev: '',    // 展示用完整地址
    avatarChar: 'U',
    saving: false
  },

  onLoad() {
    if (!auth.isLogin()) {
      // 未登录 → redirectTo 而非 navigateTo：不把 login 叠在栈里，避免登录成功 navigateBack 回 profile 后，保存 navigateBack 弹回 login 的错乱（登录导航 bug）
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.fill(auth.getUser())
    // 拉最新资料，避免本地缓存过期
    api.me().then((u) => { auth.setUser(u); this.fill(u) }).catch(() => {})
  },

  fill(u) {
    if (!u) return
    const nickname = u.nickname || ''
    const username = u.username || ''
    const role = u.role || 0
    // 微信账号 username 约定为 "wx_" + md5前 24 位（参 UserServiceImpl.registerByWechat），对用户无意义，隐藏
    const isWxAccount = username.indexOf('wx_') === 0
    this.setData({
      username,
      showUsername: !!username && !isWxAccount,
      showRole: role > 0,
      roleLabel: role === 1 ? '超级管理员' : (role === 2 ? '运营管理员' : '普通用户'),
      nickname,
      intro: u.intro || '',
      avatarUrl: u.avatarUrl || '',
      avatarPrev: util.resolveImage(u.avatarUrl),
      avatarChar: (nickname || username || 'U').charAt(0).toUpperCase()
    })
  },

  onNickname(e) { this.setData({ nickname: e.detail.value }) },
  onIntro(e) { this.setData({ intro: e.detail.value }) },

  // 微信「头像昵称填写能力」：button open-type=chooseAvatar 回调拿临时路径，
  // 统一走站内上传落库为相对路径，渲染时补 BASE 前缀（与榜单图片同源同规则）
  onChooseAvatar(e) {
    const fp = e.detail.avatarUrl
    if (!fp) return
    wx.showLoading({ title: '上传中' })
    api.uploadImage(fp).then((path) => {
      wx.hideLoading()
      this.setData({ avatarUrl: path, avatarPrev: util.resolveImage(path) })
    }).catch(() => wx.hideLoading())
  },

  removeAvatar() {
    this.setData({ avatarUrl: '', avatarPrev: '' })
  },

  save() {
    const nickname = (this.data.nickname || '').trim()
    if (!nickname) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    // 头像/简介传空串=清空；后端返回最新用户信息，直接刷新本地登录态缓存
    api.updateProfile({
      nickname,
      avatarUrl: this.data.avatarUrl,
      intro: this.data.intro
    }).then((u) => {
      auth.setUser(u)
      wx.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => this.goBackSafe(), 600)
    }).catch(() => {}).then(() => this.setData({ saving: false }))
  },

  // 安全回退：栈里上一跳不是 login 时 navigateBack；否则直接回首页（避免“保存后又弹回登录页”的错乱）
  goBackSafe() {
    const pages = getCurrentPages()
    const prevRoute = pages.length >= 2 ? (pages[pages.length - 2].route || '') : ''
    if (prevRoute && prevRoute.indexOf('pages/login') !== 0) {
      wx.navigateBack()
    } else {
      // 上一跳是 login 或无可回退 → 一律 switchTab 回发现首页（tab 页安全）
      wx.switchTab({ url: '/pages/index/index' })
    }
  }
})
