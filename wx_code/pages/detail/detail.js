const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

const VIS_LABELS = ['公开', '仅链接可见', '私有']
const VIS_VALUES = [1, 2, 0]

Page({
  data: {
    id: '',
    detail: null,
    items: [],
    canManage: false,          // 榜单层门控：创建者本人或超级管理员（增删改排名项/榜单均用它）
    loading: false,

    panel: '',                 // '' | 'rank' | 'item'
    rankForm: {},
    rankVisIndex: 0,
    visLabels: VIS_LABELS,
    itemForm: {},
    saving: false,
    previewUrl: ''
  },

  onLoad(options) {
    this.setData({ id: options.id })
  },

  onShow() {
    if (!auth.isLogin()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.loadDetail()
  },

  onPullDownRefresh() {
    this.loadDetail().then(() => wx.stopPullDownRefresh()).catch(() => wx.stopPullDownRefresh())
  },

  loadDetail() {
    this.setData({ loading: true })
    return api.getRankingDetail(this.data.id).then((d) => {
      const items = (d.items || []).map((it) => this.mapItem(it))
      this.setData({
        detail: d,
        coverFull: util.resolveImage(d.coverUrl),
        items,
        canManage: util.canManage(d.creatorId)
      })
    }).catch(() => {}).then(() => this.setData({ loading: false }))
  },

  mapItem(it) {
    return {
      id: it.id,
      name: it.name,
      description: it.description,
      sourceName: it.sourceName,
      sourceTypeRaw: it.sourceType || '',
      sourceDescRaw: it.sourceDesc || '',
      currentRank: it.currentRank,
      thumb: util.resolveImage(it.imageUrl),
      imageUrlRaw: it.imageUrl || '',
      agreeCount: it.agreeCount,
      opposeCount: it.opposeCount,
      participantCount: it.participantCount,
      myVoteType: it.myVoteType,
      reasonCount: it.reasonCount,
      reasonDraft: '',
      reasonImage: '',
      reasonImagePrev: '',
      submitting: false,
      reasons: (it.reasons || []).map((r) => this.mapReason(r))
    }
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

  // ---- 排名项投票：投票=换票，点同向则取消；用返回的最新计数就地更新 ----
  voteItem(e) {
    if (!auth.isLogin()) return this.toLogin()
    const idx = e.currentTarget.dataset.idx
    const type = Number(e.currentTarget.dataset.type)
    const it = this.data.items[idx]
    const cancel = it.myVoteType === type
    const req = cancel
      ? api.cancelItemVote(this.data.id, it.id)
      : api.voteItem(this.data.id, it.id, type)
    req.then((v) => {
      this.setData({
        [`items[${idx}].myVoteType`]: v.myVoteType,
        [`items[${idx}].agreeCount`]: v.agreeCount,
        [`items[${idx}].opposeCount`]: v.opposeCount
      })
    }).catch(() => {})
  },

  // ---- 理由投票 ----
  voteReason(e) {
    if (!auth.isLogin()) return this.toLogin()
    const { idx, ridx } = e.currentTarget.dataset
    const type = Number(e.currentTarget.dataset.type)
    const r = this.data.items[idx].reasons[ridx]
    const cancel = r.myVoteType === type
    const req = cancel ? api.cancelReasonVote(r.id) : api.voteReason(r.id, type)
    req.then((v) => {
      this.setData({
        [`items[${idx}].reasons[${ridx}].myVoteType`]: v.myVoteType,
        [`items[${idx}].reasons[${ridx}].agreeCount`]: v.agreeCount,
        [`items[${idx}].reasons[${ridx}].opposeCount`]: v.opposeCount
      })
    }).catch(() => {})
  },

  // ---- 新增理由 ----
  onReasonInput(e) {
    const idx = e.currentTarget.dataset.idx
    this.setData({ [`items[${idx}].reasonDraft`]: e.detail.value })
  },

  pickReasonImage(e) {
    const idx = e.currentTarget.dataset.idx
    this.uploadImage((path) => this.setData({ [`items[${idx}].reasonImage`]: path, [`items[${idx}].reasonImagePrev`]: util.resolveImage(path) }))
  },

  clearReasonImage(e) {
    const idx = e.currentTarget.dataset.idx
    this.setData({ [`items[${idx}].reasonImage`]: '', [`items[${idx}].reasonImagePrev`]: '' })
  },

  submitReason(e) {
    if (!auth.isLogin()) return this.toLogin()
    const idx = e.currentTarget.dataset.idx
    const it = this.data.items[idx]
    const content = (it.reasonDraft || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入理由内容', icon: 'none' })
      return
    }
    this.setData({ [`items[${idx}].submitting`]: true })
    api.addReason(this.data.id, it.id, content, it.reasonImage || undefined)
      .then(() => {
        wx.showToast({ title: '理由已发布', icon: 'success' })
        this.loadDetail()
      })
      .catch(() => {})
      .then(() => this.setData({ [`items[${idx}].submitting`]: false }))
  },

  deleteReason(e) {
    const { idx, ridx } = e.currentTarget.dataset
    const r = this.data.items[idx].reasons[ridx]
    wx.showModal({
      title: '删除理由', content: '确定删除这条理由吗？',
      success: (res) => {
        if (!res.confirm) return
        api.deleteReason(r.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadDetail()
        }).catch(() => {})
      }
    })
  },

  goItem(e) {
    const { idx } = e.currentTarget.dataset
    const it = this.data.items[idx]
    wx.navigateTo({ url: `/pages/item/item?id=${this.data.id}&itemId=${it.id}` })
  },

  // 点击发起人进入他人主页；creatorId 缺失（历史数据）时不跳转
  goUser(e) {
    const id = e.currentTarget.dataset.id
    if (id) wx.navigateTo({ url: '/pages/user/user?id=' + id })
  },

  // ---- 图片预览 ----
  preview(e) {
    this.setData({ previewUrl: e.currentTarget.dataset.url })
  },
  closePreview() {
    this.setData({ previewUrl: '' })
  },

  // ---- 榜单管理：删除 / 编辑元数据 ----
  deleteRanking() {
    wx.showModal({
      title: '删除榜单', content: '删除后不可恢复，确定删除？',
      success: (res) => {
        if (!res.confirm) return
        api.deleteRanking(this.data.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 600)
        }).catch(() => {})
      }
    })
  },

  openRankEdit() {
    const d = this.data.detail
    const visIndex = Math.max(0, VIS_VALUES.indexOf(d.visibility))
    this.setData({
      panel: 'rank',
      rankVisIndex: visIndex,
      rankForm: {
        title: d.title,
        description: d.description || '',
        itemLimit: d.itemLimit,
        coverUrl: d.coverUrl || '',
        coverPrev: util.resolveImage(d.coverUrl)
      }
    })
  },

  onRankField(e) {
    this.setData({ ['rankForm.' + e.currentTarget.dataset.key]: e.detail.value })
  },
  onRankNumber(e) {
    this.setData({ 'rankForm.itemLimit': e.detail.value })
  },
  onVisChange(e) {
    this.setData({ rankVisIndex: Number(e.detail.value) })
  },
  pickRankCover() {
    this.uploadImage((path) => this.setData({ 'rankForm.coverUrl': path, 'rankForm.coverPrev': util.resolveImage(path) }))
  },

  saveRankEdit() {
    const f = this.data.rankForm
    if (!(f.title || '').trim()) {
      wx.showToast({ title: '标题不能为空', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    api.updateRanking(this.data.id, {
      title: f.title.trim(),
      description: f.description,
      coverUrl: f.coverUrl || null,
      itemLimit: Number(f.itemLimit),
      visibility: VIS_VALUES[this.data.rankVisIndex]
    }).then(() => {
      wx.showToast({ title: '已保存', icon: 'success' })
      this.setData({ panel: '' })
      this.loadDetail()
    }).catch(() => {}).then(() => this.setData({ saving: false }))
  },

  // ---- 排名项管理：新增 / 编辑 / 删除 ----
  openItemAdd() {
    const d = this.data.detail
    const used = this.data.items.length
    if (used >= Number(d.itemLimit)) {
      wx.showToast({ title: `已达名次上限 Top ${d.itemLimit}`, icon: 'none' })
      return
    }
    this.setData({
      panel: 'item',
      itemForm: { mode: 'add', itemId: '', name: '', description: '', imageUrl: '', imagePrev: '' }
    })
  },

  openItemEdit(e) {
    const idx = e.currentTarget.dataset.idx
    const it = this.data.items[idx]
    this.setData({
      panel: 'item',
      itemForm: {
        mode: 'edit', itemId: it.id, name: it.name,
        description: it.description || '', imageUrl: it.imageUrlRaw || '', imagePrev: it.thumb,
        sourceType: it.sourceTypeRaw || '', sourceName: it.sourceName || '', sourceDesc: it.sourceDescRaw || ''
      }
    })
  },

  onItemField(e) {
    this.setData({ ['itemForm.' + e.currentTarget.dataset.key]: e.detail.value })
  },
  pickItemImage() {
    this.uploadImage((path) => this.setData({ 'itemForm.imageUrl': path, 'itemForm.imagePrev': util.resolveImage(path) }))
  },

  saveItem() {
    const f = this.data.itemForm
    if (!(f.name || '').trim()) {
      wx.showToast({ title: '名称不能为空', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    const req = f.mode === 'add'
      ? api.addRankingItem(this.data.id, { name: f.name.trim(), description: f.description || undefined, imageUrl: f.imageUrl || undefined })
      : api.updateRankingItem(this.data.id, f.itemId, {
        name: f.name.trim(),
        description: f.description || '',
        imageUrl: f.imageUrl || '',
        // 后端全量替换语义：不回传会清空来源，故编辑时把原来源值原样带回（来源细粒度编辑仍归网页端）
        sourceType: f.sourceType || '',
        sourceName: f.sourceName || '',
        sourceDesc: f.sourceDesc || ''
      })
    req.then(() => {
      wx.showToast({ title: '已保存', icon: 'success' })
      this.setData({ panel: '' })
      this.loadDetail()
    }).catch(() => {}).then(() => this.setData({ saving: false }))
  },

  deleteItem(e) {
    const idx = e.currentTarget.dataset.idx
    const it = this.data.items[idx]
    wx.showModal({
      title: '删除排名项', content: `删除「${it.name}」后其下理由一并隐藏（投票记录保留），确定？`,
      success: (res) => {
        if (!res.confirm) return
        api.deleteRankingItem(this.data.id, it.id).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadDetail()
        }).catch(() => {})
      }
    })
  },

  closePanel() {
    this.setData({ panel: '' })
  },

  // ---- 通用：选图上传，回抛相对路径 ----
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
  },

  toLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  }
})
