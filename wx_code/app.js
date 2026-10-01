// 全局应用入口：缓存登录用户，供各页快速读取（真正的安全边界在后端）
const auth = require('./utils/auth')

App({
  globalData: {
    userInfo: null
  },
  onLaunch() {
    this.globalData.userInfo = auth.getUser()
  },
  // 登录/登出/fetchMe 后同步内存态
  setUserInfo(user) {
    this.globalData.userInfo = user
  }
})
