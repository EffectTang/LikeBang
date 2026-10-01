const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

const PAGE_SIZE = 10

Page({
  data: {
    id: '',
    itemId: '',
    item: null,
    coverFull: '',
    reasons: [],
    total: 0,
    current: 1,
    hasMore: true,
    loading: false,

    draft: '',
    draftImage: '',
    draftImagePrev: '',
    publishing: false,

    commentsMap: {},   // reasonId -> { visible, loaded, loading, submitting, list, total, current, draft }
    previewUrl: ''
  },

  onLoad(options) {
    this.setData({ id: options.id, itemId: options.itemId })
  },

  onShow() {
    if (!auth.isLogin()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.loadItem()
    this.loadReasons(true)
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.loadReasons(false)
  },

  loadItem() {
    return api.getRankingItem(this.data.id, this.data.itemId).then((it) => {
      this.setData({
        item: {
          id: it.id,
          name: it.name,
          description: it.description,
          sourceName: it.sourceName,
          sourceDesc: it.sourceDesc,
          currentRank: it.currentRank,
          thumb: util.resolveImage(it.imageUrl),
          agreeCount: it.agreeCount,
          opposeCount: it.opposeCount,
          myVoteType: it.myVoteType,
          reasonCount: it.reasonCount,
          creatorNickname: it.creatorNickname || '匿名',
          createdAt: util.fmtTime(it.createdAt)
        }
      })
    }).catch(() => {})
  },

  mapReason(r) {
    const u = auth.getUser()
    return {
      id: r.id,
      content: r.content,
      image: util.resolveImage(r.imageUrl),
      agreeCount: r.agreeCount,
      opposeCount: r.opposeCount,
      myVoteType: r.myVoteType,
      commentCount: r.commentCount || 0,
      creatorNickname: r.creatorNickname || '匿名',
      canEdit: auth.isAdmin() || util.sameId(r.creatorId, u && u.id),
      createdAt: util.fmtTime(r.createdAt)
    }
  },

  loadReasons(reset) {
    if (this.data.loading) return
    const current = reset ? 1 : this.data.current
    this.setData({ loading: true })
    api.listItemReasons(this.data.id, this.data.itemId, { current, size: PAGE_SIZE }).then((page) => {
      const records = (page.records || []).map((r) => this.mapReason(r))
      const total = Number(page.total) || 0
      const reasons = reset ? records : this.data.reasons.concat(records)
      this.setData({ reasons, total, current, hasMore: reasons.length < total })
    }).catch(() => {}).then(() => this.setData({ loading: false }))
  },

  // ---- 主体投票 ----
  voteItem() {
    if (!auth.isLogin()) return this.toLogin()
    const it = this.data.item
    // 点认同：若当前已认同则取消，否则投认同
    const wantAgree = it.myVoteType !== 1
    const req = wantAgree ? api.voteItem(this.data.id, it.id, 1) : api.cancelItemVote(this.data.id, it.id)
    req.then((v) => {
      this.setData({ 'item.myVoteType': v.myVoteType, 'item.agreeCount': v.agreeCount, 'item.opposeCount': v.opposeCount })
    }).catch(() => {})
  },
  voteItemOppose() {
    if (!auth.isLogin()) return this.toLogin()
    const it = this.data.item
    const wantOppose = it.myVoteType !== -1
    const req = wantOppose ? api.voteItem(this.data.id, it.id, -1) : api.cancelItemVote(this.data.id, it.id)
    req.then((v) => {
      this.setData({ 'item.myVoteType': v.myVoteType, 'item.agreeCount': v.agreeCount, 'item.opposeCount': v.opposeCount })
    }).catch(() => {})
  },

  // ---- 发布理由 ----
  onDraftInput(e) {
    this.setData({ draft: e.detail.value })
  },
  pickDraftImage() {
    this.uploadImage((path) => this.setData({ draftImage: path, draftImagePrev: util.resolveImage(path) }))
  },
  clearDraftImage() {
    this.setData({ draftImage: '', draftImagePrev: '' })
  },
  submitReason() {
    if (!auth.isLogin()) return this.toLogin()
    const content = (this.data.draft || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入理由内容', icon: 'none' })
      return
    }
    this.setData({ publishing: true })
    api.addReason(this.data.id, this.data.itemId, content, this.data.draftImage || undefined).then(() => {
      wx.showToast({ title: '已发布', icon: 'success' })
      this.setData({ draft: '', draftImage: '', draftImagePrev: '' })
      this.loadItem()
      this.loadReasons(true)
    }).catch(() => {}).then(() => this.setData({ publishing: false }))
  },

  // ---- 理由投票 / 删除 ----
  voteReason(e) {
    if (!auth.isLogin()) return this.toLogin()
    const idx = e.currentTarget.dataset.idx
    const type = Number(e.currentTarget.dataset.type)
    const r = this.data.reasons[idx]
    const cancel = r.myVoteType === type
    const req = cancel ? api.cancelReasonVote(r.id) : api.voteReason(r.id, type)
    req.then((v) => {
      this.setData({
        [`reasons[${idx}].myVoteType`]: v.myVoteType,
        [`reasons[${idx}].agreeCount`]: v.agreeCount,
        [`reasons[${idx}].opposeCount`]: v.opposeCount
      })
    }).catch(() => {})
  },

  deleteReason(e) {
    const idx = e.currentTarget.dataset.idx
    const r = this.data.reasons[idx]
    wx.showModal({
      title: '删除理由', content: '确定删除这条理由吗？',
      success: (res) => {
        if (!res.confirm) return
        api.deleteReason(r.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadItem()
          this.loadReasons(true)
        }).catch(() => {})
      }
    })
  },

  // ---- 楼中楼评论 ----
  ensureBox(reasonId) {
    const map = this.data.commentsMap
    if (!map[reasonId]) {
      map[reasonId] = { visible: false, loaded: false, loading: false, submitting: false, list: [], total: 0, current: 1, draft: '' }
    }
    return map
  },

  toggleComments(e) {
    const rid = e.currentTarget.dataset.rid
    const box = JSON.parse(JSON.stringify(this.ensureBox(rid)))
    box.visible = !box.visible
    this.setData({ [`commentsMap.${rid}`]: box })
    if (box.visible && !box.loaded) this.loadComments(rid, 1)
  },

  loadComments(rid, page) {
    const box = this.ensureBox(rid)
    box.loading = true
    this.setData({ [`commentsMap.${rid}.loading`]: true })
    api.listReasonComments(rid, { current: page, size: PAGE_SIZE }).then((p) => {
      const records = (p.records || []).map((c) => ({
        id: c.id,
        content: c.content,
        creatorNickname: c.creatorNickname || '匿名',
        createdAt: util.fmtTime(c.createdAt),
        canDelete: auth.isAdmin() || util.sameId(c.creatorId, (auth.getUser() || {}).id)
      }))
      const total = Number(p.total) || 0
      const list = page === 1 ? records : this.data.commentsMap[rid].list.concat(records)
      this.setData({
        [`commentsMap.${rid}.list`]: list,
        [`commentsMap.${rid}.total`]: total,
        [`commentsMap.${rid}.current`]: page,
        [`commentsMap.${rid}.loaded`]: true
      })
    }).catch(() => {}).then(() => this.setData({ [`commentsMap.${rid}.loading`]: false }))
  },

  onCommentInput(e) {
    const rid = e.currentTarget.dataset.rid
    this.setData({ [`commentsMap.${rid}.draft`]: e.detail.value })
  },

  submitComment(e) {
    if (!auth.isLogin()) return this.toLogin()
    const rid = e.currentTarget.dataset.rid
    const box = this.data.commentsMap[rid]
    const content = (box.draft || '').trim()
    if (!content) return
    this.setData({ [`commentsMap.${rid}.submitting`]: true })
    api.addReasonComment(rid, content).then((c) => {
      const item = {
        id: c.id,
        content: c.content,
        creatorNickname: c.creatorNickname || '匿名',
        createdAt: util.fmtTime(c.createdAt),
        canDelete: auth.isAdmin() || util.sameId(c.creatorId, (auth.getUser() || {}).id)
      }
      const cur = this.data.commentsMap[rid]
      const append = cur.list.length >= cur.total ? [item] : []
      this.setData({
        [`commentsMap.${rid}.list`]: cur.list.concat(append),
        [`commentsMap.${rid}.total`]: cur.total + 1,
        [`commentsMap.${rid}.draft`]: ''
      })
      // 同步理由评论数
      const idx = this.data.reasons.findIndex(r => String(r.id) === String(rid))
      if (idx >= 0) this.setData({ [`reasons[${idx}].commentCount`]: (this.data.reasons[idx].commentCount || 0) + 1 })
    }).catch(() => {}).then(() => this.setData({ [`commentsMap.${rid}.submitting`]: false }))
  },

  loadMoreComments(e) {
    const rid = e.currentTarget.dataset.rid
    this.loadComments(rid, this.data.commentsMap[rid].current + 1)
  },

  deleteComment(e) {
    const { rid, cid } = e.currentTarget.dataset
    wx.showModal({
      title: '删除回复', content: '确定删除这条回复吗？',
      success: (res) => {
        if (!res.confirm) return
        api.deleteReasonComment(cid).then(() => {
          const cur = this.data.commentsMap[rid]
          const list = cur.list.filter(c => String(c.id) !== String(cid))
          this.setData({
            [`commentsMap.${rid}.list`]: list,
            [`commentsMap.${rid}.total`]: Math.max(0, cur.total - 1)
          })
          const idx = this.data.reasons.findIndex(r => String(r.id) === String(rid))
          if (idx >= 0) this.setData({ [`reasons[${idx}].commentCount`]: Math.max(0, (this.data.reasons[idx].commentCount || 0) - 1) })
        }).catch(() => {})
      }
    })
  },

  // ---- 通用 ----
  preview(e) {
    this.setData({ previewUrl: e.currentTarget.dataset.url })
  },
  closePreview() {
    this.setData({ previewUrl: '' })
  },
  goBack() {
    wx.navigateBack()
  },
  toLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },
  uploadImage(callback) {
    wx.chooseMedia({
      count: 1, mediaType: ['image'], sourceType: ['album', 'camera'], sizeType: ['compressed'],
      success: (res) => {
        const fp = res.tempFiles[0].tempFilePath
        wx.showLoading({ title: '上传中' })
        api.uploadImage(fp).then((path) => {
          wx.hideLoading()
          callback(path)
        }).catch(() => wx.hideLoading())
      }
    })
  }
})
