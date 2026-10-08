const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

const PAGE_SIZE = 10
const STATUS_LABEL = { 0: '草稿', 1: '进行中', 2: '已下架' }

Page({
  data: {
    user: null,
    avatarChar: '',
    avatarFull: '',
    roleLabel: '',
    showRole: false,
    hasIntro: false,
    emailMask: '',
    phoneMask: '',
    joinDate: '',
    lastLoginDate: '',
    isLogin: false,
    list: [],
    current: 1,
    total: 0,
    hasMore: true,
    loading: false
  },

  onShow() {
    const isLogin = auth.isLogin()
    const user = auth.getUser()
    this.setData(Object.assign({ isLogin }, this.fillUser(user)))
    if (isLogin) {
      this.refreshMe()
      this.loadList(true)
    } else {
      this.setData({ list: [], total: 0 })
    }
  },

  // 由 user 派生页面展示字段（Hero 卡 + 账号信息卡），onShow 与 refreshMe 共用同一口径
  fillUser(user) {
    if (!user) {
      return { user: null, avatarChar: '', avatarFull: '', roleLabel: '', showRole: false, hasIntro: false, emailMask: '', phoneMask: '', joinDate: '', lastLoginDate: '' }
    }
    return {
      user,
      avatarChar: this.avatarOf(user),
      avatarFull: util.resolveImage(user.avatarUrl),
      roleLabel: this.roleLabelOf(user.role),
      showRole: user.role === 1 || user.role === 2,
      hasIntro: !!(user.intro && user.intro.trim()),
      emailMask: this.maskEmail(user.email),
      phoneMask: this.maskPhone(user.phone),
      joinDate: this.dateOnly(user.createdAt),
      lastLoginDate: this.dateOnly(user.lastLoginAt)
    }
  },

  avatarOf(u) {
    if (!u) return ''
    const name = u.nickname || u.username || 'U'
    return name.charAt(0).toUpperCase()
  },

  roleLabelOf(role) {
    if (role === 1) return '超级管理员'
    if (role === 2) return '运营管理员'
    return '普通用户'
  },

  // "2026-09-30T12:00:00" → "2026-09-30"
  dateOnly(value) {
    if (!value) return ''
    return String(value).replace('T', ' ').slice(0, 10)
  },

  // 手机号脱敏：138****8888；非标准长度原样返回
  maskPhone(phone) {
    if (!phone) return ''
    const s = String(phone)
    if (s.length < 7) return s
    return s.slice(0, 3) + '****' + s.slice(-4)
  },

  // 邮箱脱敏：ab***@qq.com；保留 @ 及域名
  maskEmail(email) {
    if (!email) return ''
    const s = String(email)
    const at = s.indexOf('@')
    if (at <= 0) return s
    const local = s.slice(0, at)
    const domain = s.slice(at)
    const head = local.length <= 2 ? local.charAt(0) : local.slice(0, 2)
    return head + '***' + domain
  },

  // 拉最新用户信息（角色/资料可能变）
  refreshMe() {
    api.me().then((u) => {
      auth.setUser(u)
      this.setData(this.fillUser(u))
    }).catch(() => {})
  },

  goProfile() {
    // 未登录时 Hero 整卡点击不应进入资料编辑页，引导先登录
    if (!auth.isLogin()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    wx.navigateTo({ url: '/pages/profile/profile' })
  },

  loadList(reset) {
    if (this.data.loading) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true })
    api.listMyRankings({ current, size: PAGE_SIZE }).then((page) => {
      const records = (page.records || []).map((r) => this.mapCard(r))
      const total = Number(page.total) || 0
      const list = reset ? records : this.data.list.concat(records)
      this.setData({ list, total, current, hasMore: list.length < total })
    }).catch(() => {}).then(() => this.setData({ loading: false }))
  },

  mapCard(r) {
    return {
      id: r.id,
      title: r.title,
      cover: util.resolveImage(r.coverUrl),
      categoryName: r.categoryName,
      itemLimit: r.itemLimit,
      itemCount: r.itemCount,
      viewCount: r.viewCount,
      status: r.status,
      statusLabel: STATUS_LABEL[r.status] || '未知'
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  deleteRanking(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除榜单', content: '删除后不可恢复，确定？',
      success: (res) => {
        if (!res.confirm) return
        api.deleteRanking(id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadList(true)
        }).catch(() => {})
      }
    })
  },

  logout() {
    wx.showModal({
      title: '退出登录', content: '确定退出当前账号？',
      success: (res) => {
        if (!res.confirm) return
        auth.clear()
        this.setData(Object.assign({ isLogin: false, list: [], total: 0 }, this.fillUser(null)))
      }
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.loadList(false)
  },

  onPullDownRefresh() {
    if (this.data.isLogin) {
      this.refreshMe()
      this.loadList(true)
    }
    wx.stopPullDownRefresh()
  }
})
