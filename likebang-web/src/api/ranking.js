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

// 所有启用分类
export function listCategories() {
  return request.get('/categories')
}
