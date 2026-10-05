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

// 来源类型字典：key -> 展示名（下拉选项单一事实源，需登录）
export function getSourceTypes() {
  return request.get('/rankings/source-types')
}

// 榜单封面最大数量上限（后台“系统设置”可改 ranking.detail.cover_limit，需登录）
export function getCoverLimit() {
  return request.get('/rankings/cover-limit')
}

// 站内历史来源作品名（创建表单自动补全，keyword 可选，需登录）
export function searchSourceNames(keyword) {
  return request.get('/rankings/source-names', { params: { keyword } })
}

// 跟榜单排名项内容搜索（支持 keyword + sourceName 复合过滤，需登录）
export function searchItems(params) {
  return request.get('/rankings/items/search', { params })
}

// 删除榜单（创建者本人或管理员）
export function deleteRanking(id) {
  return request.delete(`/rankings/${id}`)
}

// 我创建的榜单（分页，排除已删除）
export function listMyRankings(params) {
  return request.get('/rankings/mine', { params })
}

// 我参与投票的榜单（分页，仅未删除，供「我的空间-动态」用）
export function listVotedRankings(params) {
  return request.get('/rankings/voted-by-me', { params })
}

// 我的空间数据概览（仅本人：我发布榜单数 / 收到票数 / 参与投票榜单数）
export function getSpaceStats() {
  return request.get('/rankings/space-stats')
}

// 某用户的公开榜单（他人主页，分页，仅已发布+公开）
export function listUserRankings(userId, params) {
  return request.get(`/rankings/by-user/${userId}`, { params })
}

// 编辑榜单元数据（创建者本人或管理员；null 字段不修改）
export function updateRanking(id, data) {
  return request.put(`/rankings/${id}`, data)
}

// 为某排名项新增推荐理由（imageUrl 可选，一条理由一张图；不传即无图）
export function addReason(rankingId, itemId, content, imageUrl) {
  return request.post(`/rankings/${rankingId}/items/${itemId}/reasons`, { content, imageUrl })
}

// 某排名项详情（排名项页面主体）
export function getRankingItem(rankingId, itemId) {
  return request.get(`/rankings/${rankingId}/items/${itemId}`)
}

// 某排名项的全量理由（分页，按认同数降序）
export function listItemReasons(rankingId, itemId, params) {
  return request.get(`/rankings/${rankingId}/items/${itemId}/reasons`, { params })
}

// 修改排名项配图（创建者本人或管理员）；imageUrl 传空串=清空配图
export function updateItemImage(rankingId, itemId, imageUrl) {
  return request.put(`/rankings/${rankingId}/items/${itemId}/image`, { imageUrl })
}

// 新增排名项（榜单创建者本人或管理员），返回新项ID
export function addRankingItem(rankingId, data) {
  return request.post(`/rankings/${rankingId}/items`, data)
}

// 删除排名项（榜单创建者本人或管理员）；软删该项及其下理由
export function deleteRankingItem(rankingId, itemId) {
  return request.delete(`/rankings/${rankingId}/items/${itemId}`)
}

// 编辑排名项（榜单创建者本人或管理员）；全量替换 name/description/imageUrl/来源字段
export function updateRankingItem(rankingId, itemId, data) {
  return request.put(`/rankings/${rankingId}/items/${itemId}`, data)
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

// 修改推荐理由（仅本人或管理员）；imageUrl 不传=不改图，传空串=清空图
export function updateReason(reasonId, content, imageUrl) {
  return request.put(`/rankings/reasons/${reasonId}`, { content, imageUrl })
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
