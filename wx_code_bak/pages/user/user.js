const api = require('../../utils/api')
const util = require('../../utils/util')

const PAGE_SIZE = 10

Page({
  data: {
    userId: '',
    profile: null,
    avatarPrev: '',
    avatarChar: 'U',
    joinDate: '',
    list: [],
    current: 1,
    total: 0,
    hasMore: true,
    loading: false,
    loadingMore: false
  },

  onLoad(options) {
    this.setData({ userId: options.id })
    this.loadProfile()
    this.loadList(true)
  },

  onPullDownRefresh() {
    this.loadProfile()
    this.loadList(true).then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh())
  },

  loadProfile() {
    api.getPublicProfile(this.data.userId).then((u) => {
      const nickname = u.nickname || ''
      this.setData({
        profile: u,
        avatarPrev: util.resolveImage(u.avatarUrl),
        avatarChar: (nickname || 'U').charAt(0).toUpperCase(),
        joinDate: u.createdAt ? String(u.createdAt).slice(0, 10) : ''
      })
    }).catch(() => {})
  },

  // reset=true 回到第一页；否则追加下一页
  loadList(reset) {
    if (this.data.loading) return Promise.resolve()
    const current = reset ? 1 : this.data.current
    this.setData(reset ? { loading: true } : { loadingMore: true })

    return api.listUserRankings(this.data.userId, { current, size: PAGE_SIZE }).then((page) => {
      const records = (page.records || []).map(this.mapCard)
      const total = Number(page.total) || 0
      const list = reset ? records : this.data.list.concat(records)
      this.setData({ list, total, current, hasMore: list.length < total })
    }).catch(() => {}).then(() => {
      this.setData({ loading: false, loadingMore: false })
    })
  },

  // 后端 coverUrl 为相对路径，预先生成可渲染全址（WXML 不能调用 JS 函数）
  mapCard(r) {
    return {
      id: r.id,
      title: r.title,
      description: r.description,
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

  onReachBottom() {
    if (this.data.hasMore && !this.data.loadingMore) {
      this.loadList(false)
    }
  }
})
