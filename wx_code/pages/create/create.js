const api = require('../../utils/api')
const util = require('../../utils/util')
const auth = require('../../utils/auth')

const VIS_LABELS = ['公开', '仅链接可见', '私有']
const VIS_VALUES = [1, 2, 0]

Page({
  data: {
    title: '',
    description: '',
    coverUrl: '',
    coverPrev: '',

    categories: [],       // [{id, name, sourceEnabled}]，首项为“未分类”
    catIndex: 0,
    catNames: ['未分类'],

    itemLimit: 5,
    visLabels: VIS_LABELS,
    visIndex: 0,

    srcKeys: [''],        // ''=不填
    srcLabels: ['无来源'],
    sourceOn: false,      // 当前分类是否开启来源能力（随 catIndex 联动，供 WXML 条件渲染）

    items: [],            // [{name, description, reason, imageUrl, imagePrev, srcIndex}]
    submitting: false
  },

  onLoad() {
    if (!auth.isLogin()) {
      wx.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ items: [this.emptyItem(), this.emptyItem()] })
    this.loadCategories()
    this.loadSourceTypes()
  },

  emptyItem() {
    return { name: '', description: '', reason: '', imageUrl: '', imagePrev: '', srcIndex: 0 }
  },

  loadCategories() {
    api.listCategories().then((list) => {
      const cats = [{ id: null, name: '未分类', sourceEnabled: 0 }].concat(list || [])
      this.setData({ categories: cats, catNames: cats.map(c => c.name) })
    }).catch(() => {})
  },

  loadSourceTypes() {
    api.getSourceTypes().then((dict) => {
      const keys = ['']
      const labels = ['无来源']
      Object.keys(dict || {}).forEach((k) => { keys.push(k); labels.push(dict[k]) })
      this.setData({ srcKeys: keys, srcLabels: labels })
    }).catch(() => {})
  },

  // 当前所选分类是否开启来源能力
  currentCat() {
    return this.data.categories[this.data.catIndex] || {}
  },

  isSourceEnabled() {
    const cat = this.currentCat()
    return cat && Number(cat.sourceEnabled) === 1
  },

  onField(e) {
    this.setData({ [e.currentTarget.dataset.key]: e.detail.value })
  },
  onLimit(e) {
    this.setData({ itemLimit: e.detail.value })
  },
  onCatChange(e) {
    const idx = Number(e.detail.value)
    const cat = this.data.categories[idx] || {}
    this.setData({ catIndex: idx, sourceOn: Number(cat.sourceEnabled) === 1 })
  },
  onVisChange(e) {
    this.setData({ visIndex: Number(e.detail.value) })
  },
  pickCover() {
    this.uploadImage((path) => this.setData({ coverUrl: path, coverPrev: util.resolveImage(path) }))
  },

  // ---- 排名项 ----
  onItemField(e) {
    const { idx, key } = e.currentTarget.dataset
    this.setData({ [`items[${idx}].${key}`]: e.detail.value })
  },
  onItemSrcChange(e) {
    const idx = e.currentTarget.dataset.idx
    this.setData({ [`items[${idx}].srcIndex`]: Number(e.detail.value) })
  },
  pickItemImage(e) {
    const idx = e.currentTarget.dataset.idx
    this.uploadImage((path) => {
      this.setData({ [`items[${idx}].imageUrl`]: path, [`items[${idx}].imagePrev`]: util.resolveImage(path) })
    })
  },
  addItem() {
    if (this.data.items.length >= 50) {
      wx.showToast({ title: '最多 50 项', icon: 'none' })
      return
    }
    this.setData({ items: this.data.items.concat(this.emptyItem()) })
  },
  removeItem(e) {
    const idx = e.currentTarget.dataset.idx
    if (this.data.items.length <= 2) {
      wx.showToast({ title: '至少保留 2 项', icon: 'none' })
      return
    }
    const items = this.data.items.slice()
    items.splice(idx, 1)
    this.setData({ items })
  },

  submit() {
    if (this.data.submitting) return
    const title = (this.data.title || '').trim()
    if (!title) {
      wx.showToast({ title: '请输入榜单标题', icon: 'none' })
      return
    }
    const limit = Number(this.data.itemLimit)
    if (!limit || limit < 3 || limit > 50) {
      wx.showToast({ title: '名次上限需 3~50', icon: 'none' })
      return
    }
    const list = this.data.items
    if (list.length < 2) {
      wx.showToast({ title: '至少 2 个排名项', icon: 'none' })
      return
    }
    if (list.length > limit) {
      wx.showToast({ title: '排名项数量不能超过名次上限', icon: 'none' })
      return
    }
    for (let i = 0; i < list.length; i++) {
      if (!(list[i].name || '').trim()) {
        wx.showToast({ title: `第 ${i + 1} 项名称不能为空`, icon: 'none' })
        return
      }
    }

    const sourceOn = this.isSourceEnabled()
    const items = list.map((it) => {
      const node = {
        name: it.name.trim(),
        description: (it.description || '').trim() || undefined,
        imageUrl: it.imageUrl || undefined,
        reason: (it.reason || '').trim() || undefined
      }
      if (sourceOn) {
        node.sourceType = this.data.srcKeys[it.srcIndex] || undefined
        node.sourceName = (it.sourceName || '').trim() || undefined
        node.sourceDesc = (it.sourceDesc || '').trim() || undefined
      }
      return node
    })

    const payload = {
      title,
      description: (this.data.description || '').trim() || undefined,
      coverUrl: this.data.coverUrl || undefined,
      categoryId: this.currentCat().id || undefined,
      itemLimit: limit,
      visibility: VIS_VALUES[this.data.visIndex],
      items
    }

    this.setData({ submitting: true })
    api.createRanking(payload).then((id) => {
      wx.showToast({ title: '发布成功', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: '/pages/detail/detail?id=' + id }), 700)
    }).catch(() => {}).then(() => this.setData({ submitting: false }))
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
