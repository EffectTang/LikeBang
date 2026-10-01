// 后端接口基址：Spring context-path 为 /api，故 API 与图片静态资源同走此前缀。
// 后端返回的图片相对路径形如 /uploads/202609/xxx.png，拼接后 = http://host:8080/api/uploads/...
// 开发调试：微信开发者工具需勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」。
// 真机 / 生产：改成你的 HTTPS 域名并在小程序后台配置 request / uploadFile / downloadFile 合法域名。
module.exports = {
  BASE_URL: 'http://localhost:8080/api'
}
