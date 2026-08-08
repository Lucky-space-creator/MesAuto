# mesCore — MES 制造执行系统核心

`mesCore` 是一套面向中小型制造企业的 **MES（制造执行系统）核心后端**，基于 Spring Boot 4 + MyBatis-Plus 构建，提供从物料、工艺、订单到排产、仓库、审批的完整业务 API，配套前端管理后台位于 `fronted/` 目录。

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Spring Boot 4.1.0（Java 17） |
| 持久层 | MyBatis-Plus 3.5.9 + MySQL 8 + Druid 连接池 |
| 分库分表 | ShardingSphere-JDBC 5.5.2 |
| 缓存 | Redis（Spring Data Redis / Lettuce） |
| 安全 | Shiro-core 2.0.3 + JWT 0.12.6（无状态鉴权） |
| 对象存储 | MinIO 8.5.17 |
| 文件导入导出 | EasyExcel 4.0.3 |
| 工具 | Hutool 5.8.42、Lombok |
| API 文档 | Knife4j 4.5.0（OpenAPI 3 / Swagger） |

## 模块结构

后端按业务域划分模块（`src/main/java/com/lucky/mescore/modules/`）：

- **system** — 用户、角色、权限、登录鉴权（`AuthController`）
- **material** — 物料主数据、物料分类（树）
- **order** — 生产订单、订单明细、提交审批
- **process** — 工艺路线、工序步骤
- **schedule** — 生产计划、生产任务、开工/报工/完成、工位与工作中心
- **warehouse** — 仓库、库存、入库、出库
- **approval** — 审批待办/已办、审批模板（节点/发布/启停）

通用层 `common/` 包含：统一返回 `R<T>`、分页 `PageRequest/PageResponse`、全局异常、Shiro 集成、JWT 工具、枚举与实体基类。

## 约定

- 统一响应：`{ code: 0, msg: "ok", data: ... }`，`code === 0` 表示成功。
- 分页请求：`GET /xxx/page?pageNum=1&pageSize=10`，响应包含在 `PageResponse` 中（`records / total / pageNum / pageSize`）。
- 鉴权：登录后前端在 `Authorization` 头携带 JWT，未登录访问受保护接口返回 `401`。

## 环境依赖

- JDK 17+
- MySQL 8.x（库名 `mescore`，字符集 `utf8mb4`）
- Redis 6/7
- MinIO（可选，用于文件存储）
- Maven 3.8+

## 配置

核心配置在 `src/main/resources/application.yml`（默认激活 `dev` 环境）：

- 数据源：`spring.datasource.druid`（默认 `localhost:3306/mescore`，账号/密码 `root`）
- Redis：`spring.data.redis`
- MinIO：`minio.endpoint/access-key/secret-key/bucket`
- JWT：`jwt.secret / jwt.expire-seconds`（默认 7200 秒）
- 端口：默认 `8080`，上下文路径见 `application-dev.yml`

## 启动

```bash
# 1. 准备数据库（建库后执行 docs/ 下 SQL 初始化脚本）
# 2. 启动 Redis / MinIO（按需）
# 3. 运行
mvn spring-boot:run
# 或打包
mvn clean package -DskipTests
java -jar target/mesCore-0.0.1-SNAPSHOT.jar
```

## API 文档

启动后访问：

- Knife4j 文档：`http://localhost:8080/doc.html`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 前端

管理后台前端见 [`fronted/README.md`](fronted/README.md)（Vue 3 + Vite + Element Plus）。

## 目录一览

```
mesCore/
├── pom.xml                      # Maven 工程定义
├── src/main/java/.../mescore/
│   ├── common/                 # 通用组件（R/分页/异常/Shiro/JWT/工具）
│   └── modules/                # 业务模块（system/material/order/...）
├── src/main/resources/
│   ├── application.yml         # 主配置
│   └── application-dev.yml     # dev 环境配置
├── docs/                       # 数据库 / 说明文档
└── fronted/                    # 前端管理后台（Vue 3）
```
