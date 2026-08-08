# MesAuto
mesCore 是一套面向中小型制造企业的 MES（制造执行系统）核心平台，后端基于 Spring Boot 4 + MyBatis-Plus 构建，前端采用 Vue 3 + Element Plus 管理后台。系统覆盖物料、工艺、订单、排产、仓库及系统权限等模块，并提供可配置的自定义审批流程（模板 + 多节点 + 发布/启停，引擎驱动待办与通过/驳回）。后端以统一 REST API 与 JWT 鉴权对外服务，集成 Redis、MinIO、ShardingSphere 与 Knife4j。特色在于模块化分层、统一响应契约、前后端分离开箱即用，可快速搭建车间数字化管理。
