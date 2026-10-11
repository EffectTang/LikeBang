# 榜了个榜 · 微信小程序端（wx_code）

LikeBang 后端原生微信小程序客户端，直接复用现有 `/api` 接口，与网页端（`likebang-web`）同一套契约。

## 一、如何运行

1. 启动后端（默认 `http://localhost:8080`，context-path `/api`）。⚠ 首次拉代码需手动执行 `sql/schema.sql`（2026-10-11 新增第 23 段 `ranking_bookmark` 收藏表，不建表则收藏相关接口全部报错）。
2. 打开**微信开发者工具** → 导入项目 → 目录选择 `wx_code/`。
   - AppID 可先用「测试号」（`project.config.json` 里为 `touristappid`），或填自己的小程序 AppID。
3. 顶部菜单「详情 → 本地设置」勾选：
   - ✅ **不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书**（本地 http 必需）。
4. 编译运行即可。开发默认超管账号 `admin / root`。

## 二、接口基址配置

- 唯一配置点：`utils/config.js` → `BASE_URL`（默认 `http://localhost:8080/api`）。
- 真机 / 生产：改为你的 **HTTPS 域名**，并在小程序后台「开发管理 → 服务器域名」把该域名加入 `request`、`uploadFile`、`downloadFile` 合法域名白名单。
- 图片：后端返回相对路径 `/uploads/xxx`，展示时拼接为 `BASE_URL + /uploads/xxx`（见 `utils/util.js` 的 `resolveImage`，与网页端补 `/api` 前缀同源，只是小程序需带完整 host）。

## 三、目录结构

```
wx_code/
├─ app.js / app.json / app.wxss     入口、全局路由与 tabBar、全局样式
├─ project.config.json / sitemap.json
├─ utils/
│  ├─ config.js   接口基址 + UGC_ENABLED 总开关
│  ├─ request.js  wx.request 封装（自动带 Bearer token、拆 Result{code,data}、401 跳登录）+ 图片上传
│  ├─ api.js      全部接口集中封装（对齐 likebang-web/src/api/*）
│  ├─ auth.js     登录态存取（token/userInfo、isAdmin 口径同后端 role===1）
│  └─ util.js     resolveImage / 时间格式化 / ID 比较 / ugcEnabled + canManage 门控
└─ pages/
   ├─ index   发现（公开浏览 + 搜索 + 分类 + 滚动分页，游客可看）
   ├─ detail  榜单详情（查看 + 投票 + **收藏/取消收藏**；理由只读，管理与发布入口已下架，见下「个人主体 UGC 整改」）
   ├─ item    排名项详情（理由流分页 + 投票 + 楼中楼评论只读）
   ├─ create  发起榜单（**已从 app.json 摘除注册、tab 已下线，页面源码保留待恢复**）
   ├─ mine    我的（Hero 资料卡 + 账号信息 + **我的收藏**（长按卡片取消）+ 退出）
   └─ login   登录 / 注册 / 微信一键登录
```

## 四、能力范围（2026-10-11 个人主体 UGC 整改后）

> 微信**个人主体小程序不支持 UGC 类目**，用户可发布/评论的内容型功能无法过审。故小程序端整改为「只读 + 投票 + 收藏」，内容生产全部归网页端（`likebang-web`）运营。

**小程序端保留**：发现/搜索/分类浏览、榜单详情查看、排名项详情、理由与楼中楼评论**只读展示**、排名项与理由的投票（👍/👎 及取消）、**榜单收藏与「我的收藏」列表（2026-10-11 新增）**、修改昵称、账号密码登录/注册、微信一键登录、网页扫码登录确认。

> 收藏与投票同理：都是用户自身的私人标记，**不产生对外展示的内容，不属 UGC 生产**，整改后仍可保留。「我的」页原来的「我的榜单」列表已由「我的收藏」替换（内容生产已下架，用户没有自己的榜单可回看，该位置留白不如换成有实际用途的收藏）。

**小程序端已下架**：
- 发起榜单（tab 与 `pages` 注册均已移除）、榜单元数据编辑、删除榜单；
- 排名项新增/编辑/删除与配图、榜单封面上传；
- 发布理由、理由配图、删除理由；发布/删除楼中楼回复；
- 头像上传（`chooseAvatar`）与「自我介绍」填写——资料页仅昵称可编（走微信官方昵称填写能力）。

**开关与恢复**：上述入口统一由 `utils/config.js` 的 `UGC_ENABLED` 控制（`util.ugcEnabled()` 读取，`util.canManage()` 在关闭时恒返回 `false`，写操作另有 `ugcBlocked()` 兜底）。将来升级企业主体并接入微信内容安全审核接口后：开关翻回 `true` + 在 `app.json` 重新注册 `pages/create/create` 及其 tab，即整体恢复。

**有意未做（小程序侧，与整改无关）**：
- **管理后台**：分类管理、用户管理、系统配置（桌面端专用，未上小程序）。
- **来源字段的编辑**：来源的新增/修改请在网页端完成（排名项编辑入口现也随 UGC 一并下架）。
- **榜单封面多图**：详情页仍只消费 `coverUrl` 单图。

## 五、已知边界与约定（沿用工程约定）

- 全局 **Long→String**：所有 ID 与分页 `total` 都是字符串，比较用 `String(a)===String(b)`，`total` 用 `Number()` 归一化（已在 `utils` 统一处理）。
- 投票 = 换票 / 幂等取消，接口返回最新计数，前端**就地更新不整页刷新**。
- 收藏 = **幂等**（`POST/DELETE /rankings/{id}/bookmark`），接口返回**操作后的收藏态布尔**，前端以服务端返回为准、不本地取反猜；详情页的收藏态由 `GET /rankings/{id}` 的 `myBookmark` 一次带回（不再单独发一次「我是否收藏过」）；「我的收藏」按收藏时间倒序分页，取消走长按卡片、就地剔除不整页重载。
- 榜单/排名项**结构管理门控**为「创建者本人或超级管理员」（`role===1`，运营管理员 STAFF 不可），与后端 `requireOwnedRanking` 一致；真正的安全边界始终在后端。**2026-10-11 UGC 整改后**：`util.canManage` 在开关关闭时前置短路恒 `false`（理由 `canEdit`、评论 `canDelete` 也已收口到同一门控），故所有管理入口自动消失；后端写接口本身未动——Web 端运营仍需它产出内容。
- 游客仅可浏览发现页与公开榜单；详情页、投票、理由、评论等均需登录，未登录会被引导到登录页。

## 六、微信登录（已落地，2026-10）

原「后续如需微信登录」方案已实现，此处仅留接入要点，细节见 `README_ForAi.md` 4.11：
1. 后端 `POST /user/auth/wx-login`（body `{code, nickname?, avatarUrl?}`，免登录白名单）用 `appid + secret` 调 `jscode2session` 换 `openid`，命中即登录、未命中静默注册，签发同款 JWT。
2. 配置项 `likebang.auth.wx.app-id/app-secret` 走环境变量 `WX_APP_ID`/`WX_APP_SECRET`，**默认留空**；未配置时接口返回 `WX_CONFIG_MISSING(1006)`。`sys_user` 需有 `openid` 唯一列（`sql/schema.sql` 第 19 段迁移）。
3. 前端 `utils/auth.js` + `pages/login` 的「微信一键登录」已接好；新用户首登弹一次性「完善资料」引导（UGC 整改后只引导填昵称）。
4. ⚠ `jscode2session` **不返回昵称头像**，`wx.getUserInfo/getUserProfile` 已被官方收回——真实昵称只能靠 `input type="nickname"` 由用户主动填写（后端对未补全者发随机占位昵称「榜友xxxx」）。
