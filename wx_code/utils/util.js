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

// UGC 总开关读取口：个人主体整改下，内容生产类入口（发布/编辑/删除/上传）统一以此门控，
// 单点收敛在 config.UGC_ENABLED，避免各页散落 if 判断口径不一致
function ugcEnabled() {
  return config.UGC_ENABLED !== false
}

// 榜单/排名项/理由内容管理门控：本人或超级管理员（口径同 Web canDelete / requireOwnedRanking）。
// UGC 下架期间整体返回 false——管理入口本身也是内容生产，不能只藏按钮
function canManage(creatorId) {
  if (!ugcEnabled()) return false
  const u = auth.getUser()
  if (!u) return false
  return auth.isAdmin() || sameId(creatorId, u.id)
}

module.exports = {
  resolveImage,
  fmtTime,
  sameId,
  ugcEnabled,
  canManage
}
