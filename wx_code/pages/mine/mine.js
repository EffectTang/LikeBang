const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

const PAGE_SIZE = 10

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
    loading: false,
    publishOn: util.ugcEnabled()   // 个性签名等 UGC 引导语显隐（个人主体整改下架，见 config.UGC_ENABLED）
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

  // 我的收藏列表：后端按收藏时间倒序返回（ranking_bookmark join ranking，一条分页 SQL）
  loadList(reset) {
    if (this.data.loading) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true })
    api.listBookmarkedRankings({ current, size: PAGE_SIZE }).then((page) => {
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
      viewCount: r.viewCount
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  // 长按取消收藏：确认后走后端幂等接口，成功就地剔除该行并 total-1（不整页重载，保住分页位置）
  cancelBookmark(e) {
    const id = e.currentTarget.dataset.id
    const target = (this.data.list || []).filter((x) => util.sameId(x.id, id))[0]
    if (!target) return
    wx.showModal({
      title: '取消收藏', content: `不再收藏「${target.title}」？`,
      success: (res) => {
        if (!res.confirm) return
        api.cancelBookmark(id).then(() => {
          wx.showToast({ title: '已取消收藏', icon: 'none' })
          const list = this.data.list.filter((x) => !util.sameId(x.id, id))
          this.setData({ list, total: Math.max(0, Number(this.data.total) - 1) })
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
