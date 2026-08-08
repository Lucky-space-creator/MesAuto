-- ============================================
-- MES Core 数据库初始化脚本
-- 数据库名: mescore
-- 字符集: utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS mescore DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE mescore;

-- ============================================
-- 数据库迁移：为已有表添加 Activiti 关联列（幂等操作）
-- ============================================
SET @dbname = DATABASE();

-- mes_approval_process: 添加 proc_inst_id（Activiti 流程实例ID）
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @dbname AND table_name = 'mes_approval_process' AND column_name = 'proc_inst_id') = 0,
    'ALTER TABLE mes_approval_process ADD COLUMN proc_inst_id VARCHAR(64) DEFAULT NULL COMMENT ''Activiti流程实例ID'' AFTER status',
    'SELECT 1' );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- mes_approval_task: 添加 activiti_task_id（Activiti 任务ID）
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = @dbname AND table_name = 'mes_approval_task' AND column_name = 'activiti_task_id') = 0,
    'ALTER TABLE mes_approval_task ADD COLUMN activiti_task_id VARCHAR(64) DEFAULT NULL COMMENT ''Activiti任务ID'' AFTER status',
    'SELECT 1' );
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================
-- 一、权限管理系统表
-- ============================================

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    username        VARCHAR(64)     NOT NULL                 COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL                 COMMENT '密码(加密)',
    salt            VARCHAR(32)     NOT NULL                 COMMENT '盐值',
    real_name       VARCHAR(64)     DEFAULT NULL             COMMENT '真实姓名',
    phone           VARCHAR(20)     DEFAULT NULL             COMMENT '手机号',
    email           VARCHAR(128)    DEFAULT NULL             COMMENT '邮箱',
    avatar          VARCHAR(512)    DEFAULT NULL             COMMENT '头像URL',
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

CREATE TABLE IF NOT EXISTS sys_role (
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

CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id   VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    parent_id   BIGINT          DEFAULT 0                COMMENT '父权限ID',
    perm_code   VARCHAR(128)    NOT NULL                 COMMENT '权限标识',
    perm_name   VARCHAR(64)     NOT NULL                 COMMENT '权限名称',
    perm_type   VARCHAR(16)     NOT NULL DEFAULT 'API'   COMMENT '类型(MENU/BTN/API)',
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

CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    user_id     BIGINT      NOT NULL                 COMMENT '用户ID',
    role_id     BIGINT      NOT NULL                 COMMENT '角色ID',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id              BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    role_id         BIGINT      NOT NULL                 COMMENT '角色ID',
    permission_id   BIGINT      NOT NULL                 COMMENT '权限ID',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    username        VARCHAR(64)     DEFAULT NULL             COMMENT '操作用户',
    module          VARCHAR(64)     DEFAULT NULL             COMMENT '操作模块',
    action          VARCHAR(64)     DEFAULT NULL             COMMENT '操作类型',
    target          VARCHAR(256)    DEFAULT NULL             COMMENT '操作对象',
    request_url     VARCHAR(512)    DEFAULT NULL             COMMENT '请求URL',
    request_method  VARCHAR(16)     DEFAULT NULL             COMMENT '请求方法',
    request_param   TEXT            DEFAULT NULL             COMMENT '请求参数',
    response_code   INT             DEFAULT NULL             COMMENT '响应状态码',
    cost_time       BIGINT          DEFAULT NULL             COMMENT '耗时(ms)',
    ip              VARCHAR(64)     DEFAULT NULL             COMMENT '客户端IP',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tenant_time (tenant_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ============================================
-- 二、流水号生成表
-- ============================================

CREATE TABLE IF NOT EXISTS sys_serial_number (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    prefix          VARCHAR(8)      NOT NULL                 COMMENT '前缀',
    biz_type        VARCHAR(64)     NOT NULL                 COMMENT '业务类型',
    date_part       VARCHAR(8)      NOT NULL                 COMMENT '日期(yyyyMMdd)',
    current_seq     INT             NOT NULL DEFAULT 0       COMMENT '当前序号',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_biz_date (tenant_id, biz_type, date_part)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水号生成器';

-- ============================================
-- 三、物料管理表
-- ============================================

CREATE TABLE IF NOT EXISTS mes_material_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    parent_id       BIGINT          NOT NULL DEFAULT 0       COMMENT '父分类ID',
    category_code   VARCHAR(64)     NOT NULL                 COMMENT '分类编码',
    category_name   VARCHAR(128)    NOT NULL                 COMMENT '分类名称',
    sort            INT             DEFAULT 0                COMMENT '排序',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_category_code (tenant_id, category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料分类';

CREATE TABLE IF NOT EXISTS mes_unit (
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

CREATE TABLE IF NOT EXISTS mes_material (
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
    material_type   VARCHAR(16)     NOT NULL DEFAULT 'MATERIAL' COMMENT '类型',
    min_stock       DECIMAL(20,4)   DEFAULT 0                COMMENT '最低库存',
    max_stock       DECIMAL(20,4)   DEFAULT 0                COMMENT '最高库存',
    default_warehouse_id BIGINT     DEFAULT NULL             COMMENT '默认放置仓库ID',
    default_location_id  BIGINT     DEFAULT NULL             COMMENT '默认放置库位ID',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_material_code (tenant_id, material_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料主数据';

CREATE TABLE IF NOT EXISTS mes_bom (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    bom_code        VARCHAR(64)     NOT NULL                 COMMENT 'BOM编码',
    bom_name        VARCHAR(256)    NOT NULL                 COMMENT 'BOM名称',
    material_id     BIGINT          NOT NULL                 COMMENT '父物料ID',
    version         VARCHAR(32)     NOT NULL DEFAULT 'V1.0'  COMMENT '版本号',
    effective_date  DATE            DEFAULT NULL             COMMENT '生效日期',
    expired_date    DATE            DEFAULT NULL             COMMENT '失效日期',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_bom_code (tenant_id, bom_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM清单头';

CREATE TABLE IF NOT EXISTS mes_bom_item (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    bom_id              BIGINT          NOT NULL                 COMMENT 'BOM头ID',
    child_material_id   BIGINT          NOT NULL                 COMMENT '子物料ID',
    quantity            DECIMAL(20,6)   NOT NULL                 COMMENT '用量',
    unit_id             BIGINT          NOT NULL                 COMMENT '单位ID',
    scrap_rate          DECIMAL(5,4)    DEFAULT 0                COMMENT '损耗率',
    sort                INT             DEFAULT 0                COMMENT '排序',
    remark              VARCHAR(256)    DEFAULT NULL             COMMENT '备注',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bom_child (bom_id, child_material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM清单明细';

-- ============================================
-- 四、生产工艺表
-- ============================================

CREATE TABLE IF NOT EXISTS mes_process_route (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    route_code      VARCHAR(64)     NOT NULL                 COMMENT '路线编码',
    route_name      VARCHAR(256)    NOT NULL                 COMMENT '路线名称',
    material_id     BIGINT          NOT NULL                 COMMENT '关联物料ID',
    version         VARCHAR(32)     NOT NULL DEFAULT 'V1.0'  COMMENT '版本号',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    effective_date  DATE            DEFAULT NULL             COMMENT '生效日期',
    expired_date    DATE            DEFAULT NULL             COMMENT '失效日期',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_route_code (tenant_id, route_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线头';

CREATE TABLE IF NOT EXISTS mes_process_step (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    route_id        BIGINT          NOT NULL                 COMMENT '工艺路线ID',
    step_seq        INT             NOT NULL                 COMMENT '顺序号',
    step_code       VARCHAR(64)     NOT NULL                 COMMENT '工序编码',
    step_name       VARCHAR(256)    NOT NULL                 COMMENT '工序名称',
    work_center_id  BIGINT          NOT NULL                 COMMENT '工作中心ID',
    operation_type  VARCHAR(32)     NOT NULL DEFAULT 'ASSEMBLY' COMMENT '工序类型',
    standard_hours  DECIMAL(10,2)   DEFAULT NULL             COMMENT '标准工时',
    setup_hours     DECIMAL(10,2)   DEFAULT NULL             COMMENT '准备工时',
    prev_step_id    BIGINT          DEFAULT NULL             COMMENT '前置工序ID',
    next_step_id    BIGINT          DEFAULT NULL             COMMENT '后置工序ID',
    parallel_group  VARCHAR(32)     DEFAULT NULL             COMMENT '并行组标识',
    quality_check   TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否质检点',
    parameters      JSON            DEFAULT NULL             COMMENT '工艺参数JSON',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_route_step_code (route_id, step_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序步骤';

-- ============================================
-- 五、生产排程表
-- ============================================

CREATE TABLE IF NOT EXISTS mes_work_center (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    center_code     VARCHAR(64)     NOT NULL                 COMMENT '工作中心编码',
    center_name     VARCHAR(128)    NOT NULL                 COMMENT '工作中心名称',
    center_type     VARCHAR(32)     DEFAULT 'PRODUCTION'     COMMENT '类型',
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

CREATE TABLE IF NOT EXISTS mes_workstation (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    work_center_id  BIGINT          NOT NULL                 COMMENT '工作中心ID',
    station_code    VARCHAR(64)     NOT NULL                 COMMENT '工位编码',
    station_name    VARCHAR(128)    NOT NULL                 COMMENT '工位名称',
    capacity        DECIMAL(20,4)   DEFAULT NULL             COMMENT '产能',
    status          VARCHAR(16)     NOT NULL DEFAULT 'IDLE'  COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_center_station (work_center_id, station_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工位';

CREATE TABLE IF NOT EXISTS mes_production_plan (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    plan_no         VARCHAR(64)     NOT NULL                 COMMENT '计划编号',
    order_id        BIGINT          NOT NULL                 COMMENT '关联订单ID',
    total_qty       DECIMAL(20,4)   NOT NULL                 COMMENT '计划总数量',
    completed_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '已完成数量',
    plan_status     VARCHAR(32)     NOT NULL DEFAULT 'PLANNED' COMMENT '状态',
    plan_date       DATE            NOT NULL                 COMMENT '计划日期',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plan_no (tenant_id, plan_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产计划';

CREATE TABLE IF NOT EXISTS mes_production_task (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    task_no         VARCHAR(64)     NOT NULL                 COMMENT '任务编号',
    plan_id         BIGINT          NOT NULL                 COMMENT '计划ID',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    material_id     BIGINT          NOT NULL                 COMMENT '生产物料ID',
    step_id         BIGINT          DEFAULT NULL             COMMENT '工艺步骤ID',
    workstation_id  BIGINT          NOT NULL                 COMMENT '工位ID',
    planned_qty     DECIMAL(20,4)   NOT NULL                 COMMENT '计划数量',
    actual_qty      DECIMAL(20,4)   DEFAULT 0                COMMENT '已完成数量',
    defective_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '不良数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    task_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态',
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
    UNIQUE KEY uk_tenant_task_no (tenant_id, task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产任务';

CREATE TABLE IF NOT EXISTS mes_task_report (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    task_id         BIGINT          NOT NULL                 COMMENT '任务ID',
    report_type     VARCHAR(32)     NOT NULL                 COMMENT '类型',
    report_qty      DECIMAL(20,4)   DEFAULT 0                COMMENT '报工数量',
    defective_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '不合格数量',
    work_hours      DECIMAL(10,2)   DEFAULT NULL             COMMENT '工时',
    operator        VARCHAR(64)     NOT NULL                 COMMENT '操作人',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务报工记录';

-- ============================================
-- 六、仓储管理表
-- ============================================

CREATE TABLE IF NOT EXISTS mes_warehouse (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    warehouse_code  VARCHAR(64)     NOT NULL                 COMMENT '仓库编码',
    warehouse_name  VARCHAR(128)    NOT NULL                 COMMENT '仓库名称',
    warehouse_type  VARCHAR(32)     NOT NULL DEFAULT 'NORMAL' COMMENT '类型',
    address         VARCHAR(256)    DEFAULT NULL             COMMENT '地址',
    manager         VARCHAR(64)     DEFAULT NULL             COMMENT '负责人',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_warehouse_code (tenant_id, warehouse_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库';

CREATE TABLE IF NOT EXISTS mes_storage_location (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '仓库ID',
    location_code   VARCHAR(64)     NOT NULL                 COMMENT '库位编码',
    location_type   VARCHAR(32)     DEFAULT 'NORMAL'        COMMENT '类型',
    max_capacity    DECIMAL(20,4)   DEFAULT NULL             COMMENT '最大容量',
    status          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_location (warehouse_id, location_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位';

CREATE TABLE IF NOT EXISTS mes_inventory (
    id               BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id        VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    material_id      BIGINT          NOT NULL                 COMMENT '物料ID',
    warehouse_id     BIGINT          NOT NULL                 COMMENT '仓库ID',
    location_id      BIGINT          NOT NULL                 COMMENT '库位ID',
    quantity         DECIMAL(20,6)   NOT NULL DEFAULT 0       COMMENT '库存数量',
    locked_quantity  DECIMAL(20,6)   NOT NULL DEFAULT 0       COMMENT '锁定数量',
    unit_id          BIGINT          NOT NULL                 COMMENT '单位ID',
    status           TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory (tenant_id, material_id, warehouse_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存';

CREATE TABLE IF NOT EXISTS mes_inbound_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    inbound_no      VARCHAR(64)     NOT NULL                 COMMENT '入库单号',
    inbound_type    VARCHAR(32)     NOT NULL                 COMMENT '类型',
    source_order_no VARCHAR(64)     DEFAULT NULL             COMMENT '来源单号',
    source_order_id BIGINT          DEFAULT NULL             COMMENT '来源订单ID',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '目标仓库ID',
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    inbound_date    DATE            DEFAULT NULL             COMMENT '入库日期',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_inbound_no (tenant_id, inbound_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单';

CREATE TABLE IF NOT EXISTS mes_inbound_item (
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

CREATE TABLE IF NOT EXISTS mes_outbound_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    outbound_no     VARCHAR(64)     NOT NULL                 COMMENT '出库单号',
    outbound_type   VARCHAR(32)     NOT NULL                 COMMENT '类型',
    source_order_no VARCHAR(64)     DEFAULT NULL             COMMENT '来源单号',
    warehouse_id    BIGINT          NOT NULL                 COMMENT '仓库ID',
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    outbound_date   DATE            DEFAULT NULL             COMMENT '出库日期',
    remark          VARCHAR(512)    DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_outbound_no (tenant_id, outbound_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单';

CREATE TABLE IF NOT EXISTS mes_outbound_item (
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

-- ============================================
-- 七、审批相关表
-- ============================================

CREATE TABLE IF NOT EXISTS mes_approval_template (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    template_code   VARCHAR(64)     NOT NULL                 COMMENT '模板编码',
    template_name   VARCHAR(256)    NOT NULL                 COMMENT '模板名称',
    biz_type        VARCHAR(64)     NOT NULL                 COMMENT '适用业务类型',
    biz_category    VARCHAR(64)     DEFAULT NULL             COMMENT '业务分类',
    description     VARCHAR(512)    DEFAULT NULL             COMMENT '描述',
    priority        INT             NOT NULL DEFAULT 0       COMMENT '优先级',
    is_default      TINYINT(1)      NOT NULL DEFAULT 0       COMMENT '是否默认模板',
    condition_expr  VARCHAR(512)    DEFAULT NULL             COMMENT '模板匹配条件',
    status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_template_code (tenant_id, template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批模板';

CREATE TABLE IF NOT EXISTS mes_approval_node (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    template_id     BIGINT          NOT NULL                 COMMENT '模版ID',
    node_seq        INT             NOT NULL                 COMMENT '节点顺序',
    node_name       VARCHAR(256)    NOT NULL                 COMMENT '节点名称',
    assignee_type   VARCHAR(16)     NOT NULL DEFAULT 'ROLE'  COMMENT '审批人类型',
    assignee_id     BIGINT          DEFAULT NULL             COMMENT '审批人ID',
    assignee_expr   VARCHAR(512)    DEFAULT NULL             COMMENT '动态表达式',
    condition_expr  VARCHAR(512)    DEFAULT NULL             COMMENT '跳过条件',
    timeout_hours   INT             DEFAULT NULL             COMMENT '超时时间',
    allow_delegate  TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否允许转办',
    allow_add_sign  TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否允许加签',
    sign_type       VARCHAR(16)     DEFAULT 'OR'             COMMENT '会签类型',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_node_seq (template_id, node_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批节点';

CREATE TABLE IF NOT EXISTS mes_approval_process (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id           VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    template_id         BIGINT          NOT NULL                 COMMENT '审批模板ID',
    biz_type            VARCHAR(64)     NOT NULL                 COMMENT '业务类型',
    biz_id              BIGINT          NOT NULL                 COMMENT '业务ID',
    biz_no              VARCHAR(64)     NOT NULL                 COMMENT '业务单号',
    current_node_id     BIGINT          DEFAULT NULL             COMMENT '当前审批节点ID',
    status              VARCHAR(32)     NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
    applicant           VARCHAR(64)     NOT NULL                 COMMENT '申请人',
    proc_inst_id        VARCHAR(64)     DEFAULT NULL             COMMENT 'Activiti流程实例ID',
    start_time          DATETIME        NOT NULL                 COMMENT '提交时间',
    end_time            DATETIME        DEFAULT NULL             COMMENT '结束时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程实例';

CREATE TABLE IF NOT EXISTS mes_approval_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    process_id          BIGINT          NOT NULL                 COMMENT '审批实例ID',
    node_id             BIGINT          NOT NULL                 COMMENT '审批节点ID',
    node_name           VARCHAR(256)    NOT NULL                 COMMENT '节点名称',
    assignee            VARCHAR(64)     NOT NULL                 COMMENT '审批人',
    action              VARCHAR(32)     NOT NULL                 COMMENT '动作',
    comment             VARCHAR(1024)   DEFAULT NULL             COMMENT '审批意见',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_process (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录';

CREATE TABLE IF NOT EXISTS mes_approval_task (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    process_id      BIGINT          NOT NULL                 COMMENT '审批实例ID',
    template_id     BIGINT          NOT NULL                 COMMENT '审批模板ID',
    node_id         BIGINT          NOT NULL                 COMMENT '审批节点ID',
    biz_type        VARCHAR(64)     NOT NULL                 COMMENT '业务类型',
    biz_id          BIGINT          NOT NULL                 COMMENT '业务ID',
    assignee        VARCHAR(64)     NOT NULL                 COMMENT '审批人',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    activiti_task_id VARCHAR(64)    DEFAULT NULL             COMMENT 'Activiti任务ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_assignee_status (assignee, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批待办任务';

-- ============================================
-- 八、订单管理表(分片表, 线创建逻辑表)
-- ============================================

CREATE TABLE IF NOT EXISTS mes_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    order_no        VARCHAR(64)     NOT NULL                 COMMENT '订单号',
    order_type      VARCHAR(32)     NOT NULL DEFAULT 'PRODUCTION' COMMENT '订单类型',
    order_status    VARCHAR(32)     NOT NULL DEFAULT 'DRAFT'  COMMENT '状态',
    material_id     BIGINT          NOT NULL                 COMMENT '产品物料ID',
    planned_qty     DECIMAL(20,4)   NOT NULL                 COMMENT '计划数量',
    completed_qty   DECIMAL(20,4)   DEFAULT 0                COMMENT '已完成数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    priority        VARCHAR(16)     DEFAULT 'NORMAL'         COMMENT '优先级',
    plan_start_date DATE            NOT NULL                 COMMENT '计划开始日期',
    plan_end_date   DATE            NOT NULL                 COMMENT '计划结束日期',
    actual_start_date DATE          DEFAULT NULL             COMMENT '实际开始日期',
    actual_end_date DATE            DEFAULT NULL             COMMENT '实际结束日期',
    bom_id          BIGINT          DEFAULT NULL             COMMENT 'BOM ID',
    route_id        BIGINT          DEFAULT NULL             COMMENT '工艺路线ID',
    customer_name   VARCHAR(256)    DEFAULT NULL             COMMENT '客户名称',
    remark          VARCHAR(1024)   DEFAULT NULL             COMMENT '备注',
    source_type     VARCHAR(16)     NOT NULL DEFAULT 'MANUAL' COMMENT '来源:MANUAL手工/ERP上游同步',
    source_no       VARCHAR(64)     DEFAULT NULL             COMMENT '上游ERP单号',
    sync_time       DATETIME        DEFAULT NULL             COMMENT '最近同步时间',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_order_no (tenant_id, order_no),
    UNIQUE KEY uk_tenant_source_no (tenant_id, source_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产订单';

CREATE TABLE IF NOT EXISTS mes_order_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    order_id        BIGINT          NOT NULL                 COMMENT '订单ID',
    material_id     BIGINT          NOT NULL                 COMMENT '物料ID',
    quantity        DECIMAL(20,6)   NOT NULL                 COMMENT '数量',
    unit_id         BIGINT          NOT NULL                 COMMENT '单位ID',
    line_no         INT             DEFAULT 0                COMMENT '行号',
    remark          VARCHAR(256)    DEFAULT NULL             COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- ============================================
-- 九、采购申请管理表
-- ============================================

CREATE TABLE IF NOT EXISTS mes_purchase_requisition (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键',
    tenant_id       VARCHAR(32)     NOT NULL DEFAULT 'DEFAULT' COMMENT '租户ID',
    req_no          VARCHAR(64)     NOT NULL                 COMMENT '采购申请单号',
    title           VARCHAR(256)    DEFAULT NULL             COMMENT '申请主题',
    material_id     BIGINT          NOT NULL                 COMMENT '采购物料ID',
    plan_qty        DECIMAL(20,4)   NOT NULL DEFAULT 0       COMMENT '计划采购数量',
    unit_id         BIGINT          DEFAULT NULL             COMMENT '单位ID',
    expect_date     DATE            DEFAULT NULL             COMMENT '期望到货日期',
    req_status      VARCHAR(32)     NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
    remark          VARCHAR(1024)   DEFAULT NULL             COMMENT '备注',
    create_by       VARCHAR(64)     DEFAULT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64)     DEFAULT NULL,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_req_no (tenant_id, req_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购申请';

-- ============================================
-- 十、Activiti 工作流引擎表（ACT_*）
-- 来源: activiti-engine 7.1.0.M6
-- ============================================

CREATE TABLE IF NOT EXISTS ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

INSERT IGNORE INTO ACT_GE_PROPERTY VALUES ('schema.version', '7.1.0-M6', 1);
INSERT IGNORE INTO ACT_GE_PROPERTY VALUES ('schema.history', 'create(7.1.0-M6)', 1);
INSERT IGNORE INTO ACT_GE_PROPERTY VALUES ('next.dbid', '1', 1);

CREATE TABLE IF NOT EXISTS ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ LONGBLOB,
    GENERATED_ TINYINT,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RE_DEPLOYMENT (
    ID_ varchar(64),
    NAME_ varchar(255),
    CATEGORY_ varchar(255),
    KEY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    DEPLOY_TIME_ timestamp(3) NULL,
    ENGINE_VERSION_ varchar(255),
    VERSION_ integer default 1,
    PROJECT_RELEASE_VERSION_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RE_MODEL (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(255),
    KEY_ varchar(255),
    CATEGORY_ varchar(255),
    CREATE_TIME_ timestamp(3) null,
    LAST_UPDATE_TIME_ timestamp(3) null,
    VERSION_ integer,
    META_INFO_ varchar(4000),
    DEPLOYMENT_ID_ varchar(64),
    EDITOR_SOURCE_VALUE_ID_ varchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_EXECUTION (
    ID_ varchar(64),
    REV_ integer,
    PROC_INST_ID_ varchar(64),
    BUSINESS_KEY_ varchar(255),
    PARENT_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SUPER_EXEC_ varchar(64),
    ROOT_PROC_INST_ID_ varchar(64),
    ACT_ID_ varchar(255),
    IS_ACTIVE_ TINYINT,
    IS_CONCURRENT_ TINYINT,
    IS_SCOPE_ TINYINT,
    IS_EVENT_SCOPE_ TINYINT,
    IS_MI_ROOT_ TINYINT,
    SUSPENSION_STATE_ integer,
    CACHED_ENT_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    START_TIME_ datetime(3),
    START_USER_ID_ varchar(255),
    LOCK_TIME_ timestamp(3) NULL,
    IS_COUNT_ENABLED_ TINYINT,
    EVT_SUBSCR_COUNT_ integer,
    TASK_COUNT_ integer,
    JOB_COUNT_ integer,
    TIMER_JOB_COUNT_ integer,
    SUSP_JOB_COUNT_ integer,
    DEADLETTER_JOB_COUNT_ integer,
    VAR_COUNT_ integer,
    ID_LINK_COUNT_ integer,
    APP_VERSION_ integer,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_TIMER_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_SUSPENDED_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_DEADLETTER_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    TYPE_ varchar(255) NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RE_PROCDEF (
    ID_ varchar(64) not null,
    REV_ integer,
    CATEGORY_ varchar(255),
    NAME_ varchar(255),
    KEY_ varchar(255) not null,
    VERSION_ integer not null,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    DESCRIPTION_ varchar(4000),
    HAS_START_FORM_KEY_ TINYINT,
    HAS_GRAPHICAL_NOTATION_ TINYINT,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    ENGINE_VERSION_ varchar(255),
    APP_VERSION_ integer,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_TASK (
    ID_ varchar(64),
    REV_ integer,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    NAME_ varchar(255),
    BUSINESS_KEY_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    TASK_DEF_KEY_ varchar(255),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    DELEGATION_ varchar(64),
    PRIORITY_ integer,
    CREATE_TIME_ timestamp(3) NULL,
    DUE_DATE_ datetime(3),
    CATEGORY_ varchar(255),
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    FORM_KEY_ varchar(255),
    CLAIM_TIME_ datetime(3),
    APP_VERSION_ integer,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_IDENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    TASK_ID_ varchar(64),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_EVENT_SUBSCR (
    ID_ varchar(64) not null,
    REV_ integer,
    EVENT_TYPE_ varchar(255) not null,
    EVENT_NAME_ varchar(255),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTIVITY_ID_ varchar(64),
    CONFIGURATION_ varchar(255),
    CREATED_ timestamp(3) not null DEFAULT CURRENT_TIMESTAMP(3),
    PROC_DEF_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_EVT_LOG (
    LOG_NR_ bigint auto_increment,
    TYPE_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    TIME_STAMP_ timestamp(3) not null,
    USER_ID_ varchar(255),
    DATA_ LONGBLOB,
    LOCK_OWNER_ varchar(255),
    LOCK_TIME_ timestamp(3) null,
    IS_PROCESSED_ tinyint default 0,
    primary key (LOG_NR_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_PROCDEF_INFO (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    REV_ integer,
    INFO_JSON_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_RU_INTEGRATION (
    ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    FLOW_NODE_ID_ varchar(64),
    CREATED_DATE_ timestamp(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

-- ============================================
-- 十一、Activiti 历史表（ACT_HI_*）
-- ============================================

CREATE TABLE IF NOT EXISTS ACT_HI_PROCINST (
    ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    BUSINESS_KEY_ varchar(255),
    PROC_DEF_ID_ varchar(64) not null,
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    START_USER_ID_ varchar(255),
    START_ACT_ID_ varchar(255),
    END_ACT_ID_ varchar(255),
    SUPER_PROCESS_INSTANCE_ID_ varchar(64),
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_ACTINST (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_TASKINST (
    ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_KEY_ varchar(255),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    CLAIM_TIME_ datetime(3),
    END_TIME_ datetime(3),
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    PRIORITY_ integer,
    DUE_DATE_ datetime(3),
    FORM_KEY_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_VARINST (
    ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(100),
    REV_ integer,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    CREATE_TIME_ datetime(3),
    LAST_UPDATED_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_DETAIL (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    ACT_INST_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(255),
    REV_ integer,
    TIME_ datetime(3) not null,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_COMMENT (
    ID_ varchar(64) not null,
    TYPE_ varchar(255),
    TIME_ datetime(3) not null,
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTION_ varchar(255),
    MESSAGE_ varchar(4000),
    FULL_MSG_ LONGBLOB,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_ATTACHMENT (
    ID_ varchar(64) not null,
    REV_ integer,
    USER_ID_ varchar(255),
    NAME_ varchar(255),
    DESCRIPTION_ varchar(4000),
    TYPE_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    URL_ varchar(4000),
    CONTENT_ID_ varchar(64),
    TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

CREATE TABLE IF NOT EXISTS ACT_HI_IDENTITYLINK (
    ID_ varchar(64),
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;

-- ============================================
-- Activiti 引擎索引
-- ============================================
CREATE INDEX IF NOT EXISTS ACT_IDX_EXEC_BUSKEY    ON ACT_RU_EXECUTION(BUSINESS_KEY_);
CREATE INDEX IF NOT EXISTS ACT_IDC_EXEC_ROOT      ON ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_TASK_CREATE    ON ACT_RU_TASK(CREATE_TIME_);
CREATE INDEX IF NOT EXISTS ACT_IDX_IDENT_LNK_USER ON ACT_RU_IDENTITYLINK(USER_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_IDENT_LNK_GROUP ON ACT_RU_IDENTITYLINK(GROUP_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_EVENT_SUBSCR_CONFIG_ ON ACT_RU_EVENT_SUBSCR(CONFIGURATION_);
CREATE INDEX IF NOT EXISTS ACT_IDX_VARIABLE_TASK_ID ON ACT_RU_VARIABLE(TASK_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_ATHRZ_PROCEDEF  ON ACT_RU_IDENTITYLINK(PROC_DEF_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_INFO_PROCDEF    ON ACT_PROCDEF_INFO(PROC_DEF_ID_);

-- ============================================
-- Activiti 历史表索引
-- ============================================
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_PRO_INST_END     ON ACT_HI_PROCINST(END_TIME_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_PRO_I_BUSKEY      ON ACT_HI_PROCINST(BUSINESS_KEY_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_ACT_INST_START    ON ACT_HI_ACTINST(START_TIME_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_ACT_INST_END      ON ACT_HI_ACTINST(END_TIME_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_DETAIL_PROC_INST  ON ACT_HI_DETAIL(PROC_INST_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_DETAIL_ACT_INST   ON ACT_HI_DETAIL(ACT_INST_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_DETAIL_TIME       ON ACT_HI_DETAIL(TIME_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_DETAIL_NAME       ON ACT_HI_DETAIL(NAME_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_DETAIL_TASK_ID    ON ACT_HI_DETAIL(TASK_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_PROCVAR_PROC_INST ON ACT_HI_VARINST(PROC_INST_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_PROCVAR_NAME_TYPE  ON ACT_HI_VARINST(NAME_, VAR_TYPE_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_PROCVAR_TASK_ID   ON ACT_HI_VARINST(TASK_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_ACT_INST_PROCINST ON ACT_HI_ACTINST(PROC_INST_ID_, ACT_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_ACT_INST_EXEC     ON ACT_HI_ACTINST(EXECUTION_ID_, ACT_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_IDENT_LNK_USER    ON ACT_HI_IDENTITYLINK(USER_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_IDENT_LNK_TASK    ON ACT_HI_IDENTITYLINK(TASK_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_IDENT_LNK_PROCINST ON ACT_HI_IDENTITYLINK(PROC_INST_ID_);
CREATE INDEX IF NOT EXISTS ACT_IDX_HI_TASK_INST_PROCINST ON ACT_HI_TASKINST(PROC_INST_ID_);
