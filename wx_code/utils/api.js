// 接口集中封装（对照 likebang-web/src/api/ranking.js + user + file，路径与后端 context-path /api 拼接）
const http = require('./request')

module.exports = {
  // ---- 认证 ----
  login: (form) => http.post('/user/auth/login', form),
  register: (form) => http.post('/user/auth/register', form),
  // 微信登录：body { code, nickname?, avatarUrl? }，code 由 wx.login 获取，后端换 openid 签发同款 JWT
  wxLogin: (form) => http.post('/user/auth/wx-login', form),
  me: () => http.get('/user/auth/me'),

  // ---- 分类 / 来源字典 ----
  listCategories: () => http.get('/categories'),
  getSourceTypes: () => http.get('/rankings/source-types'),
  searchSourceNames: (keyword) => http.get('/rankings/source-names', { keyword }),

  // ---- 榜单浏览 / 详情 ----
  listPublicRankings: (params) => http.get('/rankings/public', params),
  listMyRankings: (params) => http.get('/rankings/mine', params),
  getRankingDetail: (id) => http.get(`/rankings/${id}`),
  createRanking: (data) => http.post('/rankings', data),
  updateRanking: (id, data) => http.put(`/rankings/${id}`, data),
  deleteRanking: (id) => http.del(`/rankings/${id}`),

  // ---- 排名项 CRUD（榜单创建者本人或管理员）----
  addRankingItem: (id, data) => http.post(`/rankings/${id}/items`, data),
  updateRankingItem: (id, itemId, data) => http.put(`/rankings/${id}/items/${itemId}`, data),
  deleteRankingItem: (id, itemId) => http.del(`/rankings/${id}/items/${itemId}`),
  updateItemImage: (id, itemId, imageUrl) => http.put(`/rankings/${id}/items/${itemId}/image`, { imageUrl }),

  // ---- 排名项详情 / 理由分页 ----
  getRankingItem: (id, itemId) => http.get(`/rankings/${id}/items/${itemId}`),
  listItemReasons: (id, itemId, params) => http.get(`/rankings/${id}/items/${itemId}/reasons`, params),

  // ---- 理由 CRUD ----
  addReason: (id, itemId, content, imageUrl) =>
    http.post(`/rankings/${id}/items/${itemId}/reasons`, { content, imageUrl }),
  updateReason: (reasonId, content, imageUrl) =>
    http.put(`/rankings/reasons/${reasonId}`, { content, imageUrl }),
  deleteReason: (reasonId) => http.del(`/rankings/reasons/${reasonId}`),

  // ---- 投票 ----
  voteItem: (id, itemId, voteType) => http.post(`/rankings/${id}/items/${itemId}/vote`, { voteType }),
  cancelItemVote: (id, itemId) => http.del(`/rankings/${id}/items/${itemId}/vote`),
  voteReason: (reasonId, voteType) => http.post(`/rankings/reasons/${reasonId}/vote`, { voteType }),
  cancelReasonVote: (reasonId) => http.del(`/rankings/reasons/${reasonId}/vote`),

  // ---- 理由评论（楼中楼）----
  addReasonComment: (reasonId, content) => http.post(`/rankings/reasons/${reasonId}/comments`, { content }),
  listReasonComments: (reasonId, params) => http.get(`/rankings/reasons/${reasonId}/comments`, params),
  deleteReasonComment: (commentId) => http.del(`/rankings/comments/${commentId}`),

  // ---- 文件 ----
  uploadImage: (filePath) => http.uploadImage(filePath)
}
