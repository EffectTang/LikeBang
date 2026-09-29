import request from '@/utils/request'

// 上传图片（multipart，字段名 file），返回可直接渲染/落库的相对路径（如 /uploads/202609/xxx.png）
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/image', formData)
}
