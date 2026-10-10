const api = require('../../utils/api')
const util = require('../../utils/util')

const PAGE_SIZE = 10
const HISTORY_KEY = 'lb_search_history'
const HISTORY_MAX = 8

Page({
  data: {
    keyword: '',
    creatorNickname: '',
    searched: false,   // false=初始态（历史），true=结果态
    history: [],
    list: [],
    current: 1,
    total: 0,
    hasMore: true,
    loading: false,
    loadingMore: false
  },

  onLoad() {
    this.setData({ history: wx.getStorageSync(HISTORY_KEY) || [] })
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },
  onCreator(e) {
    this.setData({ creatorNickname: e.detail.value })
  },

  search() {
    const keyword = (this.data.keyword || '').trim()
    const creator = (this.data.creatorNickname || '').trim()
    if (!keyword && !creator) {
      wx.showToast({ title: '请输入标题或发起人', icon: 'none' })
      return
    }
    this.saveHistory(keyword, creator)
    this.loadList(true)
  },

  // 与 Web Phase2 同语义：同走 /rankings/public 的 keyword + creatorNickname AND 交叉过滤
  loadList(reset) {
    if (this.data.loading) return
    const current = reset ? 1 : this.data.current
    this.setData(reset ? { loading: true, searched: true } : { loadingMore: true })

    const params = { current, size: PAGE_SIZE }
    const kw = (this.data.keyword || '').trim()
    const ck = (this.data.creatorNickname || '').trim()
    if (kw) params.keyword = kw
    if (ck) params.creatorNickname = ck

    api.listPublicRankings(params).then((page) => {
      const records = (page.records || []).map((r) => ({
        id: r.id,
        title: r.title,
        description: r.description,
        cover: util.resolveImage(r.coverUrl),
        categoryName: r.categoryName,
        itemLimit: r.itemLimit,
        itemCount: r.itemCount,
        viewCount: r.viewCount,
        creatorNickname: r.creatorNickname || '匿名'
      }))
      const total = Number(page.total) || 0
      const list = reset ? records : this.data.list.concat(records)
      this.setData({ list, total, current, hasMore: list.length < total })
    }).catch(() => {}).then(() => {
      this.setData({ loading: false, loadingMore: false })
    })
  },

  // ---- 最近搜索（storage 存最近 HISTORY_MAX 组条件组合）----
  saveHistory(keyword, creator) {
    const label = [keyword, creator ? '@' + creator : ''].filter(Boolean).join(' · ')
    let history = (wx.getStorageSync(HISTORY_KEY) || [])
      .filter(h => !(h.keyword === keyword && h.creator === creator))
    history.unshift({ keyword, creator, label })
    history = history.slice(0, HISTORY_MAX)
    wx.setStorageSync(HISTORY_KEY, history)
    this.setData({ history })
  },
  tapHistory(e) {
    const h = this.data.history[e.currentTarget.dataset.idx]
    if (!h) return
    this.setData({ keyword: h.keyword || '', creatorNickname: h.creator || '' })
    this.loadList(true)
  },
  clearHistory() {
    wx.removeStorageSync(HISTORY_KEY)
    this.setData({ history: [] })
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loadingMore) this.loadList(false)
  }
})
