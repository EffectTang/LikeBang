const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

const PAGE_SIZE = 10
const STATUS_LABEL = { 0: '草稿', 1: '进行中', 2: '已下架' }

Page({
  data: {
    user: null,
    avatarChar: '',
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
    this.setData({ isLogin, user, avatarChar: this.avatarOf(user) })
    if (isLogin) {
      this.refreshMe()
      this.loadList(true)
    } else {
      this.setData({ list: [], total: 0 })
    }
  },

  avatarOf(u) {
    if (!u) return ''
    const name = u.nickname || u.username || 'U'
    return name.charAt(0).toUpperCase()
  },

  // 拉最新用户信息（角色可能变）
  refreshMe() {
    api.me().then((u) => {
      auth.setUser(u)
      this.setData({ user: u, avatarChar: this.avatarOf(u) })
    }).catch(() => {})
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

  goCreate() {
    wx.navigateTo({ url: '/pages/create/create' })
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
        this.setData({ isLogin: false, user: null, list: [], total: 0 })
      }
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.loadList(false)
  }
})
