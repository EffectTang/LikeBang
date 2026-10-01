// 登录态存取（key 与 Web 端 lb_token / lb_user 对齐，便于理解）
const TOKEN_KEY = 'lb_token'
const USER_KEY = 'lb_user'

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function getUser() {
  return wx.getStorageSync(USER_KEY) || null
}

function setAuth(loginData) {
  wx.setStorageSync(TOKEN_KEY, loginData.token)
  wx.setStorageSync(USER_KEY, loginData.userInfo)
  const app = getApp()
  if (app) app.setUserInfo(loginData.userInfo)
}

function setUser(user) {
  wx.setStorageSync(USER_KEY, user)
  const app = getApp()
  if (app) app.setUserInfo(user)
}

function clear() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
  const app = getApp()
  if (app) app.setUserInfo(null)
}

function isLogin() {
  return !!getToken()
}

// 角色：0普通 / 1管理员(ADMIN) / 2运营管理员(STAFF)
// isAdmin 严格对应后端 LoginUser.isAdmin()（仅 role===1），榜单/排名项结构管理用它门控
function isAdmin() {
  const u = getUser()
  return !!u && u.role === 1
}

module.exports = {
  getToken,
  getUser,
  setAuth,
  setUser,
  clear,
  isLogin,
  isAdmin
}
