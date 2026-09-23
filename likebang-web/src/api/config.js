import request from '@/utils/request'

// 分页查询系统配置项（仅管理员，可按分组过滤，keyword 搜名称/键名）
export function pageConfigs(params) {
  return request.get('/configs/page', { params })
}

// 修改配置值（仅管理员）
export function updateConfig(id, configValue) {
  return request.put(`/configs/${id}`, { configValue })
}
