# 榜了个榜 · 微信小程序端（wx_code）

LikeBang 后端原生微信小程序客户端，直接复用现有 `/api` 接口，与网页端（`likebang-web`）同一套契约。

## 一、如何运行

1. 启动后端（默认 `http://localhost:8080`，context-path `/api`）。
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
│  ├─ config.js   接口基址
│  ├─ request.js  wx.request 封装（自动带 Bearer token、拆 Result{code,data}、401 跳登录）+ 图片上传
│  ├─ api.js      全部接口集中封装（对齐 likebang-web/src/api/*）
│  ├─ auth.js     登录态存取（token/userInfo、isAdmin 口径同后端 role===1）
│  └─ util.js     resolveImage / 时间格式化 / ID 比较 / canManage 门控
└─ pages/
   ├─ index   发现（公开浏览 + 搜索 + 分类 + 滚动分页，游客可看）
   ├─ detail  榜单详情（投票 / 理由增删 / 榜单与排名项管理）
   ├─ item    排名项详情（理由流分页 / 投票 / 楼中楼评论）
   ├─ create  发起榜单（榜单+排名项+理由，来源随分类开关）
   ├─ mine    我的 / 我的榜单（列表 + 删除 + 退出）
   └─ login   登录 / 注册
```

## 四、与网页端的能力对齐情况

**已覆盖**：发现/搜索/分类浏览、榜单详情、排名项投票、理由增删改（含配图）、来源展示、排名项详情页 + 全量理由分页 + 投票 + 楼中楼评论、我的榜单、发起榜单、榜单元数据编辑、排名项增删改、删除榜单、登录注册。

**有意未做（小程序侧）**：
- **管理后台**：分类管理、用户管理、系统配置（桌面端专用，未上小程序）。
- **来源字段的编辑**：排名项编辑仅可改名称/描述/配图，来源仍回原值原样提交（后端是全量替换语义，不回传会被清空）；来源的新增/修改请在网页端完成。
- **微信一键登录**：后端目前只有账号密码体系，无 `jscode2session`。当前登录沿用 `/user/auth/login|register`。

## 五、已知边界与约定（沿用工程约定）

- 全局 **Long→String**：所有 ID 与分页 `total` 都是字符串，比较用 `String(a)===String(b)`，`total` 用 `Number()` 归一化（已在 `utils` 统一处理）。
- 投票 = 换票 / 幂等取消，接口返回最新计数，前端**就地更新不整页刷新**。
- 榜单/排名项**结构管理门控**为「创建者本人或超级管理员」（`role===1`，运营管理员 STAFF 不可），与后端 `requireOwnedRanking` 一致；真正的安全边界始终在后端。
- 游客仅可浏览发现页与公开榜单；详情页、投票、理由、评论等均需登录，未登录会被引导到登录页。

## 六、后续如需微信登录（需改后端）

小程序端已具备 `wx.login` 取 code 的能力，缺的是后端：
1. 新增 `POST /user/auth/wx-login`（body `{code, nickname?, avatarUrl?}`）；
2. 服务端用 `appid + secret` 调 `jscode2session` 换 `openid`，据此「查/建」用户并签发既有 JWT；
3. 前端登录页把 `wx.login` 的 `code` 打到该接口即可复用现有 token 体系。
