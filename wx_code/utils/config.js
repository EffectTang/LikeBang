// 后端接口基址：Spring context-path 为 /api，故 API 与图片静态资源同走此前缀。
// 后端返回的图片相对路径形如 /uploads/202609/xxx.png，拼接后 = http://host:8080/api/uploads/...
// 开发调试：微信开发者工具需勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」。
// 真机 / 生产：改成你的 HTTPS 域名并在小程序后台配置 request / uploadFile / downloadFile 合法域名。
module.exports = {
  BASE_URL: 'http://localhost:8080/api',
  // UGC 总开关（2026-10-11 个人主体整改）：微信个人主体小程序不支持 UGC 类目，故内容生产
  // （发起榜单、新增/编辑/删除排名项、发理由、楼中楼回复、头像与简介等资料填写）在小程序端全部下架，
  // 只留「查看 + 投票」；内容改由 Web 端（likebang-web）运营产出。
  // 各页入口统一走 util.ugcEnabled() 取此值，将来升级企业主体 + 接入内容安全审核接口后翻回 true 即整体恢复。
  UGC_ENABLED: false
}
