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

// 为某排名项新增推荐理由
export function addReason(rankingId, itemId, content) {
  return request.post(`/rankings/${rankingId}/items/${itemId}/reasons`, { content })
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
