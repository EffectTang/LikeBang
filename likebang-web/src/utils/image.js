// 后端上传接口返回的是 /uploads/xxx 相对路径；后端 context-path 为 /api，
// <img> 直连页面根会 404，统一补 /api 前缀（开发走 Vite 代理，生产同源部署同样成立）
export function resolveImage(url) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:')) return url
  return url.startsWith('/api') ? url : `/api${url}`
}
