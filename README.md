# LikeBang
## 榜了个榜

## 技术栈总结

**后端**

- Spring Boot 3.2.5 + Java 17
- MyBatis-Plus 3.5.6（分页插件、逻辑删除、自动驼峰映射）
- MySQL 8.0
- Lombok（简化代码）
- Hutool（工具类库）
- Validation（参数校验）
- Spring Mail（找回密码邮件）

## 找回密码（邮箱重置链接）

- 流程：登录页「忘记密码」→ 输注册邮箱申请（免登录接口 `POST /user/auth/password-reset/request`，防账号探测、多级频控）→ 邮箱收到一次性重置链接（30 分钟有效，库内仅存令牌 SHA-256 哈希）→ 落地页 `/reset-password?token=xxx` 设新密码（`POST /user/auth/password-reset/complete`）。
- 启用条件：执行 `sql/schema.sql` 第 24 节建表 `password_reset_token`，并配置 SMTP（环境变量 `MAIL_SMTP_HOST`/`MAIL_USERNAME`/`MAIL_PASSWORD`，QQ/163 用授权码）；未配置时申请接口返回明确提示。生产还需 `RESET_WEB_BASE_URL` 指向正式域名。
- 微信登录账号无密码概念，不经此通道；小程序端不提供入口。

**前端**

- Vue 3.4 + Vite 5
- Vue Router 4 + Pinia
- Element Plus 2.7（UI组件库）
- Axios（HTTP请求封装，统一错误提示）
