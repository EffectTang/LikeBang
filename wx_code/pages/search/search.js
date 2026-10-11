const api = require('../../utils/api')
const util = require('../../utils/util')

const PAGE_SIZE = 10
const HISTORY_KEY = 'lb_search_history'
const HISTORY_MAX = 8

Page({
  data: {
    // UGC 整改（2026-10-11，见 README_ForAi 4.19/4.21）：个人主体下架「按发起人找人/找榜」
    // 这一 UGC 发现路径，同时结果卡片上的 @发起人 标识一并隐藏；将来企业主体恢复 UGC 时翻回开关即可
    publishOn: util.ugcEnabled(),
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
    let history = wx.getStorageSync(HISTORY_KEY) || []
    // UGC 下架期间：清洗整改前遗留的「@发起人」历史（含只填了发起人的组合），避免历史 chip 直接暴露用户标识
    if (!this.data.publishOn) {
      history = history
        .filter(h => h.keyword)
        .map(h => ({ keyword: h.keyword, creator: '', label: h.keyword }))
    }
    this.setData({ history })
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },
  onCreator(e) {
    this.setData({ creatorNickname: e.detail.value })
  },

  search() {
    const keyword = (this.data.keyword || '').trim()
    const creator = this.data.publishOn ? (this.data.creatorNickname || '').trim() : ''
    if (!keyword && !creator) {
      wx.showToast({ title: this.data.publishOn ? '请输入标题或发起人' : '请输入标题关键词', icon: 'none' })
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
    const ck = this.data.publishOn ? (this.data.creatorNickname || '').trim() : ''
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
    this.setData({
      keyword: h.keyword || '',
      creatorNickname: this.data.publishOn ? (h.creator || '') : ''
    })
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
