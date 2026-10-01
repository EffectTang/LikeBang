const api = require('../../utils/api')
const util = require('../../utils/util')

const PAGE_SIZE = 10

Page({
  data: {
    list: [],
    categories: [],
    activeCat: null,      // null=全部
    keyword: '',
    current: 1,
    total: 0,
    hasMore: true,
    loading: false,
    loadingMore: false
  },

  onLoad() {
    this.loadCategories()
    this.loadList(true)
  },

  onShow() {
    // 从详情/创建返回时刷新（可能新增/删除了榜单）
    if (this._loadedOnce) this.loadList(true)
    this._loadedOnce = true
  },

  loadCategories() {
    api.listCategories().then((data) => {
      this.setData({ categories: data || [] })
    }).catch(() => {})
  },

  // reset=true 回到第一页；否则追加下一页
  loadList(reset) {
    if (this.data.loading) return
    const current = reset ? 1 : this.data.current
    this.setData(reset ? { loading: true } : { loadingMore: true })

    const params = { current, size: PAGE_SIZE }
    if (this.data.keyword) params.keyword = this.data.keyword
    if (this.data.activeCat != null) params.categoryId = this.data.activeCat

    api.listPublicRankings(params).then((page) => {
      const records = (page.records || []).map(this.mapCard)
      const total = Number(page.total) || 0
      const list = reset ? records : this.data.list.concat(records)
      this.setData({
        list,
        total,
        current,
        hasMore: list.length < total
      })
    }).catch(() => {}).then(() => {
      this.setData({ loading: false, loadingMore: false })
      if (reset) wx.stopPullDownRefresh()
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
      viewCount: r.viewCount,
      creatorId: r.creatorId,
      creatorNickname: r.creatorNickname || '匿名'
    }
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.loadList(true)
  },

  selectCat(e) {
    const id = e.currentTarget.dataset.id
    this.setData({ activeCat: id === '' ? null : id })
    this.loadList(true)
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  // 点击创建者进入他人主页；creatorId 缺失（历史数据）时不跳转
  goUser(e) {
    const id = e.currentTarget.dataset.id
    if (id) wx.navigateTo({ url: '/pages/user/user?id=' + id })
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/create/create' })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loadingMore) {
      this.loadList(false)
    }
  },

  onPullDownRefresh() {
    this.loadList(true)
  }
})
