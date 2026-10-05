import request from '@/utils/request'

// 全站多维搜索统一入口
// type: ALL(聚合导航，不分页) | RANKING | USER | ITEM | SOURCE
export function search({ keyword, type = 'ALL', current = 1, size = 20 } = {}) {
  return request.get('/search', { params: { keyword, type, current, size } })
}

// 便捷封装：聚合导航（全部）
export function searchAll(keyword) {
  return search({ keyword, type: 'ALL' })
}

// 便捷封装：单维分页搜索
export function searchByType(type, { keyword, current = 1, size = 20 } = {}) {
  return search({ keyword, type, current, size })
}
