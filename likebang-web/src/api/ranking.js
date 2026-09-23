import request from '@/utils/request'

// 公开榜单分页浏览
export function listPublicRankings(params) {
  return request.get('/rankings/public', { params })
}

// 榜单详情
export function getRankingDetail(id) {
  return request.get(`/rankings/${id}`)
}

// 创建榜单
export function createRanking(data) {
  return request.post('/rankings', data)
}

// 删除榜单（创建者本人或管理员）
export function deleteRanking(id) {
  return request.delete(`/rankings/${id}`)
}

// 我创建的榜单（分页，排除已删除）
export function listMyRankings(params) {
  return request.get('/rankings/mine', { params })
}

// 编辑榜单元数据（创建者本人或管理员；null 字段不修改）
export function updateRanking(id, data) {
  return request.put(`/rankings/${id}`, data)
}

// 为某排名项新增推荐理由
export function addReason(rankingId, itemId, content) {
  return request.post(`/rankings/${rankingId}/items/${itemId}/reasons`, { content })
}

// 某排名项详情（排名项页面主体）
export function getRankingItem(rankingId, itemId) {
  return request.get(`/rankings/${rankingId}/items/${itemId}`)
}

// 某排名项的全量理由（分页，按认同数降序）
export function listItemReasons(rankingId, itemId, params) {
  return request.get(`/rankings/${rankingId}/items/${itemId}/reasons`, { params })
}

// 发布理由评论（返回完整评论体，时间正序下可直接追加到流尾部）
export function addReasonComment(reasonId, content) {
  return request.post(`/rankings/reasons/${reasonId}/comments`, { content })
}

// 某理由的评论分页（时间正序）
export function listReasonComments(reasonId, params) {
  return request.get(`/rankings/reasons/${reasonId}/comments`, { params })
}

// 删除理由评论（本人或管理员）
export function deleteReasonComment(commentId) {
  return request.delete(`/rankings/comments/${commentId}`)
}

// 修改推荐理由
export function updateReason(reasonId, content) {
  return request.put(`/rankings/reasons/${reasonId}`, { content })
}

// 删除推荐理由
export function deleteReason(reasonId) {
  return request.delete(`/rankings/reasons/${reasonId}`)
}

// 所有启用分类
export function listCategories() {
  return request.get('/categories')
}

// 对排名项投/换 认同(1)或反对(-1)票
export function voteItem(rankingId, itemId, voteType) {
  return request.post(`/rankings/${rankingId}/items/${itemId}/vote`, { voteType })
}

// 取消对排名项的投票
export function cancelItemVote(rankingId, itemId) {
  return request.delete(`/rankings/${rankingId}/items/${itemId}/vote`)
}

// 对理由投/换 认同(1)或反对(-1)票
export function voteReason(reasonId, voteType) {
  return request.post(`/rankings/reasons/${reasonId}/vote`, { voteType })
}

// 取消对理由的投票
export function cancelReasonVote(reasonId) {
  return request.delete(`/rankings/reasons/${reasonId}/vote`)
}
