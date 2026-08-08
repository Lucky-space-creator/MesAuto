# mesCore 前端管理后台

`mesCore` 配套的前端管理后台，使用 **Vue 3 + Vite + Element Plus** 构建，对接后端 `mesCore` REST 接口（默认 `http://localhost:8080`）。

## 技术栈

- **Vue 3**（`<script setup>` 组合式 API）
- **Vite 5**（开发服务器与构建）
- **Element Plus 2.8** + `@element-plus/icons-vue`（UI 组件库）
- **Vue Router 4**（路由 + 登录守卫）
- **Pinia 2**（登录态管理）
- **Axios**（HTTP 请求封装）

## 功能页面

| 模块 | 页面 |
| --- | --- |
| 工作台 | Dashboard（统计卡片 + 快捷入口） |
| 登录 | 用户名密码登录，JWT 持久化 |
| 物料 | 物料列表、物料分类（树） |
| 订单 | 订单列表（明细、提交审批） |
| 审批 | 审批待办/已办、审批模板（节点/发布/启停） |
| 仓库 | 仓库、库存、入库、出库（Tab 切换） |
| 工艺 | 工艺路线（工序步骤、发布） |
| 排产 | 生产计划/任务（开工/报工/完成）、工位、工作中心 |
| 系统 | 用户（角色分配）、角色（权限树授权）、权限（树） |

## 目录结构

```
fronted/
├── index.html
├── package.json
├── vite.config.js            # 含 /api 代理到 http://localhost:8080
└── src/
    ├── main.js               # 入口（挂载 Element Plus / Pinia / Router）
    ├── App.vue
    ├── utils/request.js      # Axios 封装（统一 R 解析、JWT 拦截、401 登出）
    ├── store/user.js         # 登录态（token 存 localStorage）
    ├── router/index.js       # 路由表 + 登录守卫
    ├── layout/MainLayout.vue # 侧边菜单 + 顶栏主框架
    └── views/                # 各业务页面
        ├── Login.vue
        ├── Dashboard.vue
        ├── material/  order/  approval/  warehouse/
        ├── process/   schedule/  system/
```

## 接口对接约定

- 后端统一响应：`{ code: 0, msg, data }`，`code === 0` 视为成功；`request.js` 已自动解包 `data` 并处理错误提示。
- 分页：`GET /xxx/page?pageNum=1&pageSize=10`，返回 `records / total / pageNum / pageSize`。
- 鉴权：登录后 `Authorization` 头携带 JWT；收到 `401` 自动清除登录态并跳转登录页。
- 开发代理：`vite.config.js` 中 `/api` 已转发到 `http://localhost:8080`（如后端端口不同请自行调整）。

## 快速开始

```bash
# 安装依赖
npm install

# 本地开发（默认 http://localhost:5173）
npm run dev

# 生产构建（输出到 dist/）
npm run build

# 预览构建产物
npm run preview
```

## 备注

- 构建时会出现 Element Plus 全量引入的体积警告，不影响运行；如需优化可后续按需引入或配置 `manualChunks`。
- 后端需先启动并监听 `8080`，前端才能正常拉取数据。
