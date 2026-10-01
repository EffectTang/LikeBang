import request from '@/utils/request'

// 他人公开资料（脱敏：昵称/头像/简介/加入时间，不含邮箱手机号）
export function getPublicProfile(userId) {
  return request.get(`/users/${userId}/profile`)
}
