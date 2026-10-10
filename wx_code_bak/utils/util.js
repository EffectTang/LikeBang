// 通用工具
const config = require('./config')
const auth = require('./auth')

// 后端存 /uploads 相对路径；补 BASE_URL 供 <image src> 使用（同 Web 端 resolveImage，只是无代理需带 host）
function resolveImage(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('wxfile://')) return url
  return config.BASE_URL + url
}

// 后端 LocalDateTime 序列化为 "2026-09-30T12:00:00"，展示裁剪到分钟
function fmtTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

// 全局 Long→String：ID 比较统一转字符串，避免精度/类型不一致
function sameId(a, b) {
  if (a == null || b == null) return false
  return String(a) === String(b)
}

// 榜单/排名项结构管理门控：本人或超级管理员（口径同 Web canDelete / requireOwnedRanking）
function canManage(creatorId) {
  const u = auth.getUser()
  if (!u) return false
  return auth.isAdmin() || sameId(creatorId, u.id)
}

module.exports = {
  resolveImage,
  fmtTime,
  sameId,
  canManage
}
