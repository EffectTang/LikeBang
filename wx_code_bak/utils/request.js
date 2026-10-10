// 统一请求封装：自动带 Bearer token、拆 Result、code!==200 报错、401 跳登录
const config = require('./config')
const auth = require('./auth')

function buildHeader() {
  const header = { 'content-type': 'application/json' }
  const token = auth.getToken()
  if (token) header.Authorization = 'Bearer ' + token
  return header
}

function toLogin() {
  auth.clear()
  wx.reLaunch({ url: '/pages/login/login' })
}

// method: GET/POST/PUT/DELETE；data 对 GET 会作为 query 拼接
function request(method, path, data, options) {
  options = options || {}
  return new Promise((resolve, reject) => {
    wx.request({
      url: config.BASE_URL + path,
      method,
      data: data || {},
      header: buildHeader(),
      timeout: 15000,
      success(res) {
        // 拦截器返回 401（未登录/过期）
        if (res.statusCode === 401) {
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          setTimeout(toLogin, 800)
          reject(res)
          return
        }
        const body = res.data
        if (res.statusCode >= 200 && res.statusCode < 300 && body && body.code === 200) {
          resolve(body.data)
        } else {
          const msg = (body && body.message) || ('请求失败(' + res.statusCode + ')')
          if (!options.silent) wx.showToast({ title: msg, icon: 'none' })
          reject(body || res)
        }
      },
      fail(err) {
        if (!options.silent) wx.showToast({ title: '网络错误，请检查后端是否启动', icon: 'none' })
        reject(err)
      }
    })
  })
}

// 图片上传：multipart 字段名 file，成功返回相对路径 /uploads/...
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.BASE_URL + '/files/image',
      filePath,
      name: 'file',
      header: { Authorization: 'Bearer ' + auth.getToken() },
      success(res) {
        let body = {}
        try { body = JSON.parse(res.data) } catch (e) { /* 非 JSON（如 500 页面）走下方失败分支 */ }
        if (body && body.code === 200) {
          resolve(body.data)
        } else {
          wx.showToast({ title: (body && body.message) || '上传失败', icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        wx.showToast({ title: '上传失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  get: (path, data, options) => request('GET', path, data, options),
  post: (path, data, options) => request('POST', path, data, options),
  put: (path, data, options) => request('PUT', path, data, options),
  del: (path, data, options) => request('DELETE', path, data, options),
  uploadImage
}
