# MES Core 数据库表设计

---

## 一、设计说明

- **数据库**：MySQL 8.0+，字符集 `utf8mb4`，排序规则 `utf8mb4_general_ci`
- **引擎**：InnoDB
- **主键策略**：自增 BIGINT（MyBatis-Plus `IdType.AUTO`）
- **分片表**：`mes_order`、`mes_order_item`、`mes_inventory_log`、`mes_approval_process`、`mes_approval_record`、`mes_approval_task` 按年分片
- **总表数**：32 张（30 张业务表 + 2 张系统配置表），其中 6 张分片表
- **通用字段**（每表必含）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `tenant_id` | VARCHAR(32) | 租户 ID |
| `create_by` | VARCHAR(64) | 创建人用户名 |
| `create_time` | DATETIME | 创建时间 |
| `update_by` | VARCHAR(64) | 更新人用户名 |
| `update_time` | DATETIME | 更新时间 |
| `deleted` | TINYINT(1) | 逻辑删除 (0=正常, 1=已删除) |

---

## 二、权限管理（sys_*）

### 2.1 sys_user（用户表）

```sql
CREATE TABLE sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    username        VARCHAR(64)     NOT NULL                 COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL                 COMMENT '密码(加密)',
    salt            VARCHAR(32)     NOT NULL                 COMMENT '盐值',
    real_name       VARCHAR(64)     DEFAULT NULL             COMMENT '真实姓名',
    phone           VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    email           VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    avatar          VARCHAR(512)    DEFAULT NULL             COMMENT '头像URL(MinIO)',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    last_login_time DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_username (tenant_id, username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';
```

### 2.2 sys_role（角色表）

```sql
CREATE TABLE sys_role (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id   VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    role_code   VARCHAR(64)     NOT NULL                 COMMENT '角色编码',
    role_name   VARCHAR(64)     NOT NULL                 COMMENT '角色名称',
    description VARCHAR(256)    DEFAULT NULL             COMMENT '描述',
    status      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    create_by   VARCHAR(64)     DEFAULT NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64)     DEFAULT NULL,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_role_code (tenant_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';
```

### 2.3 sys_permission（权限表）

```sql
CREATE TABLE sys_permission (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id   VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    parent_id   BIGINT          DEFAULT 0                COMMENT '父权限ID(0=顶级)',
    perm_code   VARCHAR(128)    NOT NULL                 COMMENT '权限标识(如 order:add)',
    perm_name   VARCHAR(64)     NOT NULL                 COMMENT '权限名称',
    perm_type   VARCHAR(16)     NOT NULL DEFAULT 'API'   COMMENT '类型(MENU=菜单,BTN=按钮,API=接口)',
    url         VARCHAR(256)    DEFAULT NULL             COMMENT '请求路径',
    icon        VARCHAR(64)     DEFAULT NULL             COMMENT '图标',
    sort        INT             DEFAULT 0                COMMENT '排序',
    status      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    create_by   VARCHAR(64)     DEFAULT NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64)     DEFAULT NULL,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_perm_code (tenant_id, perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限';
```

### 2.4 sys_user_role（用户-角色关联）

```sql
CREATE TABLE sys_user_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    user_id     BIGINT      NOT NULL                 COMMENT '用户ID',
    role_id     BIGINT      NOT NULL                 COMMENT '角色ID',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';
```

### 2.5 sys_role_permission（角色-权限关联）

```sql
CREATE TABLE sys_role_permission (
    id              BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    role_id         BIGINT      NOT NULL                 COMMENT '角色ID',
    permission_id   BIGINT      NOT NULL                 COMMENT '权限ID',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';
```

### 2.6 sys_operation_log（操作日志）

```sql
CREATE TABLE sys_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    username        VARCHAR(64)     DEFAULT NULL             COMMENT '操作用户',
    module          VARCHAR(64)     DEFAULT NULL             COMMENT '操作模块',
    action          VARCHAR(64)     DEFAULT NULL             COMMENT '操作类型',
    target          VARCHAR(256)    DEFAULT NULL             COMMENT '操作对象',
    request_url     VARCHAR(512)    DEFAULT NULL             COMMENT '请求URL',
    request_method  VARCHAR(16)     DEFAULT NULL             COMMENT '请求方法',
    request_param   TEXT            DEFAULT NULL             COMMENT '请求参数(截断)',
    response_code   INT             DEFAULT NULL             COMMENT '响应状态码',
    cost_time       BIGINT          DEFAULT NULL             COMMENT '耗时(ms)',
    ip              VARCHAR(64)     DEFAULT NULL             COMMENT '客户端IP',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_time (tenant_id, create_time),
    KEY idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
```

---

## 三、物料管理（mes_material_*）

### 3.1 mes_material_category（物料分类）

```sql
CREATE TABLE mes_material_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    parent_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '父分类ID(0=根)',
    category_code   VARCHAR(64)     NOT NULL                 COMMENT '分类编码',
    category_name   VARCHAR(128)    NOT NULL                 COMMENT '分类名称',
    sort            INT             DEFAULT 0                COMMENT '排序',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_category_code (tenant_id, category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类';
```

### 3.2 mes_unit（计量单位）

```sql
CREATE TABLE mes_unit (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id   VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    unit_code   VARCHAR(32)     NOT NULL                 COMMENT '单位编码',
    unit_name   VARCHAR(32)     NOT NULL                 COMMENT '单位名称',
    status      TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    create_by   VARCHAR(64)     DEFAULT NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64)     DEFAULT NULL,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_unit_code (tenant_id, unit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量单位';
```

### 3.3 mes_material（物料主数据）

```sql
CREATE TABLE mes_material (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    material_code   VARCHAR(64)     NOT NULL                 COMMENT '物料编码',
    material_name   VARCHAR(256)    NOT NULL                 COMMENT '物料名称',
    material_spec   VARCHAR(256)    DEFAULT NULL             COMMENT '规格型号',
    drawing_no      VARCHAR(128)    DEFAULT NULL             COMMENT '图号',
    category_id     BIGINT          DEFAULT NULL             COMMENT '物料分类ID',
    primary_unit_id BIGINT          NOT NULL                 COMMENT '主单位ID',
    auxiliary_unit_id BIGINT        DEFAULT NULL             COMMENT '辅助单位ID',
    conversion_rate DECIMAL(20,6)   DEFAULT NULL             COMMENT '主辅单位换算率',
    material_type   VARCHAR(16)     NOT NULL DEFAULT 'MATERIAL' COMMENT '类型(RAW=原材料,SEMI=半成品,FINISHED=成品,MATERIAL=物料)',
    min_stock       DECIMAL(20,4)   DEFAULT 0                COMMENT '最低库存',
    max_stock       DECIMAL(20,4)   DEFAULT 0                COMMENT '最高库存',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_material_code (tenant_id, material_code),
    KEY idx_category (category_id),
    KEY idx_material_type (material_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料主数据';
```

### 3.4 mes_bom（BOM 头）

```sql
CREATE TABLE mes_bom (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    bom_code        VARCHAR(64)     NOT NULL                 COMMENT 'BOM编码',
    bom_name        VARCHAR(256)    NOT NULL                 COMMENT 'BOM名称',
    material_id     BIGINT          NOT NULL                 COMMENT '父物料ID(成品/半成品)',
    version         VARCHAR(32)     NOT NULL DEFAULT 'V1.0'  COMMENT '版本号',
    effective_date  DATE            DEFAULT NULL             COMMENT '生效日期',
    expired_date    DATE            DEFAULT NULL             COMMENT '失效日期',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_bom_code (tenant_id, bom_code),
    KEY idx_material (material_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM清单头';
```

### 3.5 mes_bom_item（BOM 明细）

```sql
CREATE TABLE mes_bom_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    bom_id              BIGINT          NOT NULL                 COMMENT 'BOM头ID',
    child_material_id   BIGINT          NOT NULL                 COMMENT '子物料ID',
    quantity            DECIMAL(20,6)   NOT NULL                 COMMENT '用量',
    unit_id             BIGINT          NOT NULL                 COMMENT '单位ID',
    scrap_rate          DECIMAL(5,4)    DEFAULT 0                COMMENT '损耗率(如0.02=2%)',
    sort                INT             DEFAULT 0                COMMENT '排序',
    remark              VARCHAR(256)    DEFAULT NULL             COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bom_child (bom_id, child_material_id),
    KEY idx_bom (bom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM清单明细';
```

---

## 四、订单管理（mes_order）

### 4.1 mes_order（生产订单）—— 分片表

```sql
CREATE TABLE mes_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    order_no        VARCHAR(64)     NOT NULL                 COMMENT '订单号',
    order_type      VARCHAR(32)     NOT NULL DEFAULT 'PRODUCTION' COMMENT '订单类型(PRODUCTION=生产,REWORK=返工)',
    order_status    VARCHAR(32)     NOT NULL DEFAULT 'DRAFT'  COMMENT '状态(DRAFT=草稿,APPROVING=审批中,RELEASED=已下达,IN_PRODUCTION=生产中,PENDING_STORAGE=待入库,COMPLETED=已完成,CLOSED=已关闭)',
    material_id     BIGINT          NOT NULL                 COMMENT '产品物料ID',
    planned_qty     DECIMAL(20,4)   NOT NULL                 COMMENT '计划数量',
    completed_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '已完成数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    priority        VARCHAR(16)     DEFAULT 'NORMAL'         COMMENT '优先级(HIGH=高,NORMAL=普通,LOW=低)',
    plan_start_date DATE            NOT NULL                 COMMENT '计划开始日期',
    plan_end_date   DATE            NOT NULL                 COMMENT '计划结束日期',
    actual_start_date DATE          DEFAULT NULL             COMMENT '实际开始日期',
    actual_end_date DATE            DEFAULT NULL             COMMENT '实际结束日期',
    bom_id          BIGINT          DEFAULT NULL             COMMENT 'BOM ID',
    route_id        BIGINT          DEFAULT NULL             COMMENT '工艺路线ID（动态切换，空=使用物料默认路线）',
    customer_name   VARCHAR(256)    DEFAULT NULL             COMMENT '客户名称',
    remark          VARCHAR(1024)   DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片键',
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_order_no (tenant_id, order_no),
    KEY idx_status (order_status),
    KEY idx_create_time (create_time),
    KEY idx_plan_date (plan_start_date, plan_end_date),
    KEY idx_material (material_id),
    KEY idx_route (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产订单（分片表，按年分片）';
```

### 4.2 mes_order_item（订单明细）—— 分片表

```sql
CREATE TABLE mes_order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    material_id     BIGINT          NOT NULL                 COMMENT '物料ID',
    quantity        DECIMAL(20,6)   NOT NULL                 COMMENT '数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    line_no         INT             DEFAULT 0                COMMENT '行号',
    remark          VARCHAR(256)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片键',
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细（分片表，绑定mes_order）';
```

---

## 五、仓储管理（mes_wms_*）

### 5.1 mes_warehouse（仓库）

```sql
CREATE TABLE mes_warehouse (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    warehouse_code  VARCHAR(64)     NOT NULL                 COMMENT '仓库编码',
    warehouse_name  VARCHAR(128)    NOT NULL                 COMMENT '仓库名称',
    warehouse_type  VARCHAR(32)     NOT NULL DEFAULT 'NORMAL' COMMENT '类型(NORMAL=普通,COLD=冷藏,FROZEN=冷冻)',
    address         VARCHAR(256)    DEFAULT NULL             COMMENT '地址',
    manager         VARCHAR(64)     DEFAULT NULL             COMMENT '负责人',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_warehouse_code (tenant_id, warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';
```

### 5.2 mes_storage_location（库位）

```sql
CREATE TABLE mes_storage_location (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '仓库ID',
    location_code   VARCHAR(64)     NOT NULL                 COMMENT '库位编码(如A-01-01)',
    location_type   VARCHAR(32)     DEFAULT 'NORMAL'        COMMENT '类型(NORMAL=普通,RECEIVING=收货区,SHIPPING=发货区,RETURN=退货区)',
    max_capacity    DECIMAL(20,4)   DEFAULT NULL             COMMENT '最大容量',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=禁用,1=启用)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_location (warehouse_id, location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位';
```

### 5.3 mes_inventory（库存）

```sql
CREATE TABLE mes_inventory (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    material_id     BIGINT          NOT NULL                 COMMENT '物料ID',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '仓库ID',
    location_id     BIGINT          NOT NULL                 COMMENT '库位ID',
    quantity        DECIMAL(20,6)   NOT NULL DEFAULT 0       COMMENT '库存数量',
    locked_quantity DECIMAL(20,6)   NOT NULL DEFAULT 0       COMMENT '锁定数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态(0=不可用,1=可用)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory (tenant_id, material_id, warehouse_id, location_id),
    KEY idx_material (material_id),
    KEY idx_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';
```

### 5.4 mes_inbound_order（入库单）

```sql
CREATE TABLE mes_inbound_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    inbound_no      VARCHAR(64)     NOT NULL                 COMMENT '入库单号',
    inbound_type    VARCHAR(32)     NOT NULL                 COMMENT '类型(PURCHASE=采购入库,PRODUCTION=生产入库,RETURN=退货入库,TRANSFER=调拨入库)',
    source_order_no VARCHAR(64)     DEFAULT NULL             COMMENT '来源单号(生产订单号等)',
    source_order_id BIGINT          DEFAULT NULL             COMMENT '来源订单ID',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '目标仓库ID',
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING=待确认,CONFIRMED=已确认,COMPLETED=已完成,CANCELLED=已取消)',
    inbound_date    DATE            DEFAULT NULL             COMMENT '入库日期',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_inbound_no (tenant_id, inbound_no),
    KEY idx_source_order (source_order_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单';
```

### 5.5 mes_inbound_item（入库明细）

```sql
CREATE TABLE mes_inbound_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    inbound_id      BIGINT          NOT NULL                 COMMENT '入库单ID',
    material_id     BIGINT          NOT NULL                 COMMENT '物料ID',
    quantity        DECIMAL(20,6)   NOT NULL                 COMMENT '数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    location_id     BIGINT          NOT NULL                 COMMENT '库位ID',
    batch_no        VARCHAR(64)     DEFAULT NULL             COMMENT '批次号',
    remark          VARCHAR(256)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_inbound (inbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细';
```

### 5.6 mes_outbound_order（出库单）

```sql
CREATE TABLE mes_outbound_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    outbound_no     VARCHAR(64)     NOT NULL                 COMMENT '出库单号',
    outbound_type   VARCHAR(32)     NOT NULL                 COMMENT '类型(PICKING=生产领料,SALES=销售出库,TRANSFER=调拨出库,SCRAP=报废出库)',
    source_order_no VARCHAR(64)     DEFAULT NULL             COMMENT '来源单号',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '仓库ID',
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING=待确认,CONFIRMED=已确认,COMPLETED=已完成,CANCELLED=已取消)',
    outbound_date   DATE            DEFAULT NULL             COMMENT '出库日期',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_outbound_no (tenant_id, outbound_no),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';
```

### 5.7 mes_outbound_item（出库明细）

```sql
CREATE TABLE mes_outbound_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    outbound_id     BIGINT          NOT NULL                 COMMENT '出库单ID',
    material_id     BIGINT          NOT NULL                 COMMENT '物料ID',
    quantity        DECIMAL(20,6)   NOT NULL                 COMMENT '数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    location_id     BIGINT          NOT NULL                 COMMENT '库位ID',
    batch_no        VARCHAR(64)     DEFAULT NULL             COMMENT '批次号',
    remark          VARCHAR(256)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_outbound (outbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细';
```

### 5.8 mes_inventory_log（库存流水）—— 分片表

```sql
CREATE TABLE mes_inventory_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    material_id     BIGINT          NOT NULL                 COMMENT '物料ID',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '仓库ID',
    location_id     BIGINT          NOT NULL                 COMMENT '库位ID',
    biz_type        VARCHAR(32)     NOT NULL                 COMMENT '业务类型(INBOUND=入库,OUTBOUND=出库,ADJUST=调整,TRANSFER=调拨)',
    biz_no          VARCHAR(64)     NOT NULL                 COMMENT '业务单号',
    change_type     VARCHAR(16)     NOT NULL                 COMMENT '变更类型(INCREASE=增加,DECREASE=减少)',
    change_qty      DECIMAL(20,6)   NOT NULL                 COMMENT '变更数量',
    before_qty      DECIMAL(20,6)   NOT NULL                 COMMENT '变更前数量',
    after_qty       DECIMAL(20,6)   NOT NULL                 COMMENT '变更后数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片键',
    PRIMARY KEY (id),
    KEY idx_material (material_id),
    KEY idx_biz_no (biz_no),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水（分片表，按年分片）';
```

---

## 六、生产排程（mes_schedule_*）

### 6.1 mes_work_center（工作中心）

```sql
CREATE TABLE mes_work_center (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    center_code     VARCHAR(64)     NOT NULL                 COMMENT '工作中心编码',
    center_name     VARCHAR(128)    NOT NULL                 COMMENT '工作中心名称',
    center_type     VARCHAR(32)     DEFAULT 'PRODUCTION'     COMMENT '类型(PRODUCTION=生产,QC=质检,PACKING=包装)',
    manager         VARCHAR(64)     DEFAULT NULL             COMMENT '负责人',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_center_code (tenant_id, center_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作中心';
```

### 6.2 mes_workstation（工位）

```sql
CREATE TABLE mes_workstation (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    work_center_id  BIGINT          NOT NULL                 COMMENT '工作中心ID',
    station_code    VARCHAR(64)     NOT NULL                 COMMENT '工位编码',
    station_name    VARCHAR(128)    NOT NULL                 COMMENT '工位名称',
    capacity        DECIMAL(20,4)   DEFAULT NULL             COMMENT '产能(件/班次)',
    status          VARCHAR(16)     NOT NULL DEFAULT 'IDLE'  COMMENT '状态(DISABLED=禁用,IDLE=空闲,BUSY=忙碌)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_center_station (work_center_id, station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位';
```

### 6.3 mes_production_plan（生产计划）

```sql
CREATE TABLE mes_production_plan (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    plan_no         VARCHAR(64)     NOT NULL                 COMMENT '计划编号',
    order_id        BIGINT          NOT NULL                 COMMENT '关联订单ID',
    total_qty       DECIMAL(20,4)   NOT NULL                 COMMENT '计划总数量',
    completed_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '已完成数量',
    plan_status     VARCHAR(32)     NOT NULL DEFAULT 'PLANNED' COMMENT '状态(PLANNED=已计划,IN_PROGRESS=进行中,COMPLETED=已完成)',
    plan_date       DATE            NOT NULL                 COMMENT '计划日期',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plan_no (tenant_id, plan_no),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划';
```

### 6.4 mes_production_task（生产任务）

```sql
CREATE TABLE mes_production_task (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    task_no         VARCHAR(64)     NOT NULL                 COMMENT '任务编号',
    plan_id         BIGINT          NOT NULL                 COMMENT '计划ID',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    material_id     BIGINT          NOT NULL                 COMMENT '生产物料ID',
    step_id         BIGINT          DEFAULT NULL             COMMENT '工艺步骤ID(mes_process_step.id，为空=非工艺驱动)',
    workstation_id  BIGINT          NOT NULL                 COMMENT '工位ID',
    planned_qty     DECIMAL(20,4)   NOT NULL                 COMMENT '计划数量',
    actual_qty      DECIMAL(20,4)   DEFAULT 0                COMMENT '已完成数量',
    defective_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '不良数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    task_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING=待生产,IN_PROGRESS=生产中,PAUSED=已暂停,COMPLETED=已完成,CANCELLED=已取消)',
    priority        VARCHAR(16)     DEFAULT 'NORMAL'         COMMENT '优先级',
    plan_start_time DATETIME        DEFAULT NULL             COMMENT '计划开始时间',
    plan_end_time   DATETIME        DEFAULT NULL             COMMENT '计划结束时间',
    actual_start_time DATETIME      DEFAULT NULL             COMMENT '实际开始时间',
    actual_end_time DATETIME        DEFAULT NULL             COMMENT '实际结束时间',
    assignee        VARCHAR(64)     DEFAULT NULL             COMMENT '指派人',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_task_no (tenant_id, task_no),
    KEY idx_plan (plan_id),
    KEY idx_order (order_id),
    KEY idx_workstation (workstation_id),
    KEY idx_step (step_id),
    KEY idx_status (task_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产任务';
```

### 6.5 mes_task_report（任务报工记录）

```sql
CREATE TABLE mes_task_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    task_id         BIGINT          NOT NULL                 COMMENT '任务ID',
    report_type     VARCHAR(32)     NOT NULL                 COMMENT '类型(START=开工,REPORT=报工,PAUSE=暂停,RESUME=恢复,COMPLETE=完成)',
    report_qty      DECIMAL(20,4)   DEFAULT 0                COMMENT '报工数量',
    defective_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '不合格数量',
    work_hours      DECIMAL(10,2)   DEFAULT NULL             COMMENT '工时(小时)',
    operator        VARCHAR(64)     NOT NULL                 COMMENT '操作人',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务报工记录';
```

---

## 七、工艺流程编排（mes_process_*）

> 工艺路线定义了产品的生产工序顺序。排程时严格按照路线逐工序生成任务，替代原"BOM展开直排"方案。

### 7.1 mes_process_route（工艺路线头）

```sql
CREATE TABLE mes_process_route (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    route_code      VARCHAR(64)     NOT NULL                 COMMENT '路线编码',
    route_name      VARCHAR(256)    NOT NULL                 COMMENT '路线名称',
    material_id     BIGINT          NOT NULL                 COMMENT '关联物料ID（成品/半成品）',
    version         VARCHAR(32)     NOT NULL DEFAULT 'V1.0'  COMMENT '版本号',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT=草稿,PUBLISHED=已发布,EXPIRED=已失效)',
    effective_date  DATE            DEFAULT NULL             COMMENT '生效日期',
    expired_date    DATE            DEFAULT NULL             COMMENT '失效日期',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_route_code (tenant_id, route_code),
    KEY idx_material (material_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线头';

-- 业务规则：同一物料同时只能有一条 PUBLISHED 状态的路线
-- 发布新版本时，旧版本自动置为 EXPIRED
```

### 7.2 mes_process_step（工序步骤）

```sql
CREATE TABLE mes_process_step (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    route_id        BIGINT          NOT NULL                 COMMENT '工艺路线ID',
    step_seq        INT             NOT NULL                 COMMENT '顺序号(10/20/30...间隔便于插入)',
    step_code       VARCHAR(64)     NOT NULL                 COMMENT '工序编码',
    step_name       VARCHAR(256)    NOT NULL                 COMMENT '工序名称',
    work_center_id  BIGINT          NOT NULL                 COMMENT '执行工序的工作中心ID',
    operation_type  VARCHAR(32)     NOT NULL DEFAULT 'ASSEMBLY' COMMENT '工序类型(CUT=切割,WELD=焊接,ASSEMBLY=装配,QC=质检,PACK=包装,OTHER=其他)',
    standard_hours  DECIMAL(10,2)   DEFAULT NULL             COMMENT '标准工时(小时)',
    setup_hours     DECIMAL(10,2)   DEFAULT NULL             COMMENT '准备工时(小时)',
    prev_step_id    BIGINT          DEFAULT NULL             COMMENT '前置工序ID（顺序依赖）',
    next_step_id    BIGINT          DEFAULT NULL             COMMENT '后置工序ID（冗余字段便于双向查询）',
    parallel_group  VARCHAR(32)     DEFAULT NULL             COMMENT '并行组标识（同组工序可并行执行）',
    quality_check   TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否质检点(0=否,1=是)',
    parameters      JSON            DEFAULT NULL             COMMENT '工艺参数JSON({temperature:200,pressure:0.5})',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_route_step_code (route_id, step_code),
    KEY idx_route (route_id),
    KEY idx_prev_step (prev_step_id),
    KEY idx_parallel_group (parallel_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序步骤';
```

**`parameters` JSON 示例**：

```json
{
  "temperature": 200,
  "pressure": 0.5,
  "speed": 1200,
  "cooling_time_minutes": 30,
  "tolerance_mm": 0.05
}
```

**工序编排约束**：
- `parallel_group` 相同但 `step_seq` 不同的工序 → **并行执行**（如焊接A和焊接B同时进行）
- `prev_step_id` 标注前置 → **顺序依赖**（必须前序完成才能开始）
- `step_seq` 间隔 10 → 插入新工序不需全局重新编号
- `quality_check=1` 的工序必须经过质检确认才能推进到下一工序

---

## 八、审批运行实例（mes_approval_*）

### 8.1 mes_approval_process（审批流程实例）—— 分片表

```sql
CREATE TABLE mes_approval_process (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id           VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    template_id         BIGINT          NOT NULL                 COMMENT '审批模板ID(mes_approval_template.id)',
    biz_type            VARCHAR(64)     NOT NULL                 COMMENT '业务类型(ORDER=订单,MATERIAL=物料)',
    biz_id              BIGINT          NOT NULL                 COMMENT '业务ID',
    biz_no              VARCHAR(64)     NOT NULL                 COMMENT '业务单号',
    current_node_id     BIGINT          DEFAULT NULL             COMMENT '当前审批节点ID（加速查询）',
    status              VARCHAR(32)     NOT NULL DEFAULT 'RUNNING' COMMENT '状态(RUNNING=运行中,APPROVED=已通过,REJECTED=已驳回,TERMINATED=已终止)',
    applicant           VARCHAR(64)     NOT NULL                 COMMENT '申请人',
    start_time          DATETIME        NOT NULL                 COMMENT '提交时间',
    end_time            DATETIME        DEFAULT NULL             COMMENT '结束时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片键',
    PRIMARY KEY (id),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_applicant (applicant),
    KEY idx_template (template_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程实例（分片表，按年分片）';
```

> **说明**：不再依赖 Activiti 的 proc_inst_id，自建 `mes_approval_process` 作为审批实例主表，通过 `template_id` 关联审批模板，`current_node_id` 快速定位当前审批环节。

### 8.2 mes_approval_record（审批记录）—— 分片表

```sql
CREATE TABLE mes_approval_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    process_id          BIGINT          NOT NULL                 COMMENT '审批实例ID(mes_approval_process.id)',
    node_id             BIGINT          NOT NULL                 COMMENT '审批节点ID(mes_approval_node.id)',
    node_name           VARCHAR(256)    NOT NULL                 COMMENT '节点名称(如"部门主管审批")',
    assignee            VARCHAR(64)     NOT NULL                 COMMENT '审批人',
    action              VARCHAR(32)     NOT NULL                 COMMENT '动作(AGREE=同意,REJECT=驳回,DELEGATE=转办,ADD_SIGN=加签)',
    comment             VARCHAR(1024)   DEFAULT NULL             COMMENT '审批意见',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片键',
    PRIMARY KEY (id),
    KEY idx_process (process_id),
    KEY idx_assignee (assignee),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录（分片表，按年分片）';
```

### 8.3 mes_approval_task（审批待办任务）—— 分片表

```sql
CREATE TABLE mes_approval_task (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    process_id      BIGINT          NOT NULL                 COMMENT '审批实例ID(mes_approval_process.id)',
    template_id     BIGINT          NOT NULL                 COMMENT '审批模板ID',
    node_id         BIGINT          NOT NULL                 COMMENT '审批节点ID(mes_approval_node.id)',
    biz_type        VARCHAR(64)     NOT NULL                 COMMENT '业务类型',
    biz_id          BIGINT          NOT NULL                 COMMENT '业务ID',
    assignee        VARCHAR(64)     NOT NULL                 COMMENT '审批人',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING=待审批,COMPLETED=已审批,DELEGATED=已转办)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '分片键',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_process (process_id),
    KEY idx_assignee_status (assignee, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批待办任务（分片表，按年分片）';
```

> **说明**：`mes_approval_task` 是审批引擎的核心待办表。提交审批时按模板节点创建待办，审批通过后更新为 `COMPLETED` 并推进到下一节点。与 `mes_approval_record` 的区别：`task` 记录当前待办的**状态与分配**，`record` 记录每一次审批的**动作历史**。

---

## 九、审批模板（动态编排核心）

> 审批模板是热编排的基础——运营人员在界面配置审批节点，无需重新部署即可切换审批链。

### 9.1 mes_approval_template（审批模板）

```sql
CREATE TABLE mes_approval_template (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    template_code   VARCHAR(64)     NOT NULL                 COMMENT '模板编码',
    template_name   VARCHAR(256)    NOT NULL                 COMMENT '模板名称',
    biz_type        VARCHAR(64)     NOT NULL                 COMMENT '适用业务类型(ORDER=订单,MATERIAL=物料)',
    biz_category    VARCHAR(64)     DEFAULT NULL             COMMENT '业务分类(NORMAL=普通,URGENT=急单,REWORK=返工,OUTSOURCE=委外)',
    description     VARCHAR(512)    DEFAULT NULL             COMMENT '描述',
    priority        INT             NOT NULL DEFAULT 0       COMMENT '优先级(数值越大越优先匹配)',
    is_default      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否默认模板(0=否,1=是)',
    condition_expr  VARCHAR(512)    DEFAULT NULL             COMMENT '模板匹配条件(如 order.priority==\"HIGH\"&&order.totalAmount>10000)',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态(DRAFT=草稿,PUBLISHED=已发布,DISABLED=已禁用)',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_template_code (tenant_id, template_code),
    KEY idx_biz_type_status (biz_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批模板';
```

### 9.2 mes_approval_node（审批节点）

```sql
CREATE TABLE mes_approval_node (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    template_id     BIGINT          NOT NULL                 COMMENT '模版ID',
    node_seq        INT             NOT NULL                 COMMENT '节点顺序(10/20/30...间隔便于插入)',
    node_name       VARCHAR(256)    NOT NULL                 COMMENT '节点名称(如"部门主管审批")',
    assignee_type   VARCHAR(16)     NOT NULL DEFAULT 'ROLE'  COMMENT '审批人类型(ROLE=按角色,SPECIFIC=指定人,DYNAMIC=动态表达式)',
    assignee_id     BIGINT          DEFAULT NULL             COMMENT '审批人ID(assignee_type=SPECIFIC时使用)',
    assignee_expr   VARCHAR(512)    DEFAULT NULL             COMMENT '动态表达式(如 ${order.departmentManager})',
    condition_expr  VARCHAR(512)    DEFAULT NULL             COMMENT '跳过条件(如 ${order.totalAmount < 10000} 则跳过此节点)',
    timeout_hours   INT             DEFAULT NULL             COMMENT '超时时间(小时,NUL=不超时)',
    allow_delegate  TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否允许转办(0=否,1=是)',
    allow_add_sign  TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否允许加签(0=否,1=是)',
    sign_type       VARCHAR(16)     DEFAULT 'OR'             COMMENT '会签类型(OR=一人通过即通过,AND=全部通过才通过)',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_node_seq (template_id, node_seq),
    KEY idx_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点';
```

**字段说明**：

| 字段 | 示例值 | 含义 |
|------|--------|------|
| `assignee_type=ROLE` | 角色 `order_department_manager` | 查 `sys_user_role` 获取该角色所有用户 |
| `assignee_type=SPECIFIC` | `assignee_id=5` | 固定审批人，直接取值 |
| `assignee_type=DYNAMIC` | `${order.departmentManager}` | SpEL 表达式，运行时求值 |
| `condition_expr` | `${order.totalAmount > 50000}` | Aviator 表达式，false 则跳过 |
| `sign_type=OR` | 并行会签 | 任一人通过即可 |
| `sign_type=AND` | 并行会签 | 必须全部通过 |

**并行会签配置示例**：
```sql
-- 同一 node_seq 但不同 sign_type 表示并行会签
INSERT INTO mes_approval_node (template_id, node_seq, node_name, assignee_type, sign_type) VALUES
(1, 30, '财务审批', 'ROLE', 'OR'),
(1, 30, '质量审批', 'ROLE', 'OR');  -- 同 seq=30，任一人通过即可推进
```

---

## 十、系统配置（sys_config）

### 10.1 sys_config（系统参数）

```sql
CREATE TABLE sys_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    config_key      VARCHAR(128)    NOT NULL                 COMMENT '参数键',
    config_value    TEXT            NOT NULL                 COMMENT '参数值',
    config_desc     VARCHAR(256)    DEFAULT NULL             COMMENT '参数描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_key (tenant_id, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';
```

### 10.2 sys_serial_number（流水号生成器）

```sql
CREATE TABLE sys_serial_number (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    prefix          VARCHAR(8)      NOT NULL                 COMMENT '前缀(MO=订单,MP=计划,MT=任务,MI=入库,MOUT=出库)',
    biz_type        VARCHAR(64)     NOT NULL                 COMMENT '业务类型(ORDER=订单,PLAN=计划,TASK=任务,INBOUND=入库,OUTBOUND=出库)',
    date_part       VARCHAR(8)      NOT NULL                 COMMENT '日期(yyyyMMdd)',
    current_seq     INT             NOT NULL DEFAULT 0       COMMENT '当前序号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_biz_date (tenant_id, biz_type, date_part)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水号生成器';
```

**流水号规则**：`{prefix}{yyyyMMdd}{6位序号}` — 例订单 `MO20250808000001`，入库 `MI20250808000001`

**初始数据**：

```sql
INSERT INTO sys_serial_number (tenant_id, prefix, biz_type, date_part, current_seq) VALUES
('DEFAULT', 'MO',   'ORDER',    DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MP',   'PLAN',     DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MT',   'TASK',     DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MI',   'INBOUND',  DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MOUT', 'OUTBOUND', DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'PR',   'PROCESS_ROUTE', DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'AT',   'APPROVAL_TEMPLATE', DATE_FORMAT(NOW(),'%Y%m%d'), 0);
```

---

## 十一、ER 关系总图

```
                        ┌─────────────────┐
                        │  sys_user        │
                        │  sys_role        │  ◄── 权限管理（RBAC）
                        │  sys_permission  │
                        └─────────────────┘

┌──────────────────────┐        ┌──────────────────────┐
│  mes_material        │        │  mes_order            │
│  mes_material_category│       │  mes_order_item       │
│  mes_bom              │◄──────│  (引用 material_id)    │
│  mes_bom_item         │ 引用   │  (引用 bom_id)        │
└──────────────────────┘        │  (引用 route_id)      │
                                └──────────┬───────────┘
                                           │ 审批
                                           ▼
┌────────────────────────────┐  ┌──────────────────────┐
│  mes_approval_template      │  │  mes_approval_process │
│  mes_approval_node          │◄─│  mes_approval_task    │
│  (审批模板，动态编排核心)      │  │  mes_approval_record  │
└────────────────────────────┘  │  (审批实例+任务+记录)   │
                                └──────────────────────┘
                                           │ 审批通过
                                           ▼
┌──────────────────────┐        ┌──────────────────────┐
│  mes_process_route    │        │  mes_production_plan  │
│  mes_process_step     │◄──────│  mes_production_task   │
│  (工艺路线，工序编排)   │ 驱动   │  mes_task_report      │
│                       │ 排程   │                      │
└──────────────────────┘        └──────────┬───────────┘
                                           │ 报工完成
                                           ▼
┌──────────────────────┐        ┌──────────────────────┐
│  mes_work_center     │        │  mes_inbound_order    │
│  mes_workstation     │◄──────│  mes_inbound_item      │
└──────────────────────┘ 入库   │  mes_outbound_order    │
                                │  mes_outbound_item     │
┌──────────────────────┐        └──────────────────────┘
│  mes_warehouse       │
│  mes_storage_location│
│  mes_inventory       │
│  mes_inventory_log   │
└──────────────────────┘
```

---

## 十二、分片表汇总

| 表名 | 分片键 | 策略 | 绑定关系 |
|------|--------|------|----------|
| `mes_order` | `create_time` | 按年 (INTERVAL) | — |
| `mes_order_item` | `create_time` | 按年 (INTERVAL) | 绑定 `mes_order` |
| `mes_inventory_log` | `create_time` | 按年 (INTERVAL) | — |
| `mes_approval_process` | `create_time` | 按年 (INTERVAL) | — |
| `mes_approval_record` | `create_time` | 按年 (INTERVAL) | 绑定 `mes_approval_process` |
| `mes_approval_task` | `create_time` | 按年 (INTERVAL) | 绑定 `mes_approval_process` |

> **非分片表**（数量少、变更不频繁）：
> `mes_process_route`、`mes_process_step`、`mes_approval_template`、`mes_approval_node`

**总计 32 张表**（30 张业务表 + 2 张系统配置表），其中 6 张分片表。

### 创建物理表脚本（以 2024-2030 为例）

```sql
-- 每年为每个分片表创建物理表
-- mes_order_2024, mes_order_2025, ... mes_order_2030
-- mes_order_item_2024, ... mes_order_item_2030
-- mes_inventory_log_2024, ... mes_inventory_log_2030
-- mes_approval_process_2024, ... mes_approval_process_2030
-- mes_approval_record_2024, ... mes_approval_record_2030
-- mes_approval_task_2024, ... mes_approval_task_2030

-- 示例：创建 mes_order 各年度物理表（结构与逻辑表一致）
DELIMITER $$
CREATE PROCEDURE create_sharding_tables()
BEGIN
    DECLARE v_year INT DEFAULT 2024;
    WHILE v_year <= 2030 DO
        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS mes_order_', v_year, ' LIKE mes_order');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS mes_order_item_', v_year, ' LIKE mes_order_item');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS mes_inventory_log_', v_year, ' LIKE mes_inventory_log');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS mes_approval_process_', v_year, ' LIKE mes_approval_process');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS mes_approval_record_', v_year, ' LIKE mes_approval_record');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET @sql = CONCAT('CREATE TABLE IF NOT EXISTS mes_approval_task_', v_year, ' LIKE mes_approval_task');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        SET v_year = v_year + 1;
    END WHILE;
END$$
DELIMITER ;

CALL create_sharding_tables();
DROP PROCEDURE create_sharding_tables;
```

---

## 十三、索引设计原则

| 原则 | 说明 |
|------|------|
| 唯一索引 | 所有 `tenant_id + 业务编码` 组合建唯一索引 |
| 查询索引 | 按 `status`、`create_time`、外键字段建普通索引 |
| 避免冗余 | InnoDB 二级索引自动包含主键，不需要 `(a, id)` 覆盖索引 |
| 分片表注意 | 分片表索引在每张物理表上独立存在，ALTER 时要批量执行 |
| 避免函数索引 | 查询条件不要对索引列套函数（如 `DATE(create_time)`），会失效 |
