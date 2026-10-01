const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

Page({
  data: {
    username: '',
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
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.fill(auth.getUser())
    // 拉最新资料，避免本地缓存过期
    api.me().then((u) => { auth.setUser(u); this.fill(u) }).catch(() => {})
  },

  fill(u) {
    if (!u) return
    const nickname = u.nickname || ''
    this.setData({
      username: u.username || '',
      roleLabel: u.role === 1 ? '超级管理员' : (u.role === 2 ? '运营管理员' : '普通用户'),
      nickname,
      intro: u.intro || '',
      avatarUrl: u.avatarUrl || '',
      avatarPrev: util.resolveImage(u.avatarUrl),
      avatarChar: (nickname || u.username || 'U').charAt(0).toUpperCase()
    })
  },

  onNickname(e) { this.setData({ nickname: e.detail.value }) },
  onIntro(e) { this.setData({ intro: e.detail.value }) },

  // 头像统一走站内上传，落库仅存相对路径，渲染时补 BASE 前缀（与榜单图片同源同规则）
  chooseAvatar() {
    wx.chooseMedia({
      count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], sizeType: ['compressed'],
      success: (res) => {
        const fp = res.tempFiles[0].tempFilePath
        wx.showLoading({ title: '上传中' })
        api.uploadImage(fp).then((path) => {
          wx.hideLoading()
          this.setData({ avatarUrl: path, avatarPrev: util.resolveImage(path) })
        }).catch(() => wx.hideLoading())
      }
    })
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
      setTimeout(() => wx.navigateBack(), 600)
    }).catch(() => {}).then(() => this.setData({ saving: false }))
  }
})
