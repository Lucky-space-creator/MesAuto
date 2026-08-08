-- ============================================
-- MES Core 初始数据脚本
-- ============================================
USE mescore;

-- ============================================
-- 一、权限初始化
-- ============================================

-- 角色
INSERT INTO sys_role (id, tenant_id, role_code, role_name, description, status) VALUES
(1, 'DEFAULT', 'admin', '超级管理员', '系统超级管理员，拥有所有权限', 1),
(2, 'DEFAULT', 'production_manager', '生产经理', '生产管理权限', 1),
(3, 'DEFAULT', 'warehouse_manager', '仓库管理员', '仓库管理权限', 1),
(4, 'DEFAULT', 'operator', '操作员', '生产操作权限', 1),
(5, 'DEFAULT', 'quality_manager', '质量经理', '质量管理权限', 1);

-- 权限 (菜单/按钮/API)
INSERT INTO sys_permission (id, tenant_id, parent_id, perm_code, perm_name, perm_type, url, sort) VALUES
-- 系统管理
(1, 'DEFAULT', 0, 'system', '系统管理', 'MENU', '/system', 1),
(2, 'DEFAULT', 1, 'system:user', '用户管理', 'MENU', '/system/user', 10),
(3, 'DEFAULT', 2, 'system:user:add', '新增用户', 'BTN', '', 1),
(4, 'DEFAULT', 2, 'system:user:edit', '编辑用户', 'BTN', '', 2),
(5, 'DEFAULT', 2, 'system:user:del', '删除用户', 'BTN', '', 3),
(6, 'DEFAULT', 1, 'system:role', '角色管理', 'MENU', '/system/role', 20),
(7, 'DEFAULT', 6, 'system:role:add', '新增角色', 'BTN', '', 1),
(8, 'DEFAULT', 6, 'system:role:edit', '编辑角色', 'BTN', '', 2),
(9, 'DEFAULT', 1, 'system:perm', '权限管理', 'MENU', '/system/perm', 30),

-- 物料管理
(20, 'DEFAULT', 0, 'material', '物料管理', 'MENU', '/material', 2),
(21, 'DEFAULT', 20, 'material:category', '物料分类', 'MENU', '/material/category', 10),
(22, 'DEFAULT', 20, 'material:list', '物料主数据', 'MENU', '/material/list', 20),
(23, 'DEFAULT', 22, 'material:add', '新增物料', 'BTN', '', 1),
(24, 'DEFAULT', 22, 'material:edit', '编辑物料', 'BTN', '', 2),
(25, 'DEFAULT', 22, 'material:del', '删除物料', 'BTN', '', 3),
(26, 'DEFAULT', 22, 'material:import', '导入物料', 'BTN', '', 4),
(27, 'DEFAULT', 22, 'material:export', '导出物料', 'BTN', '', 5),

-- BOM管理
(30, 'DEFAULT', 0, 'bom', 'BOM管理', 'MENU', '/bom', 3),
(31, 'DEFAULT', 30, 'bom:list', 'BOM列表', 'MENU', '/bom/list', 10),
(32, 'DEFAULT', 31, 'bom:add', '新增BOM', 'BTN', '', 1),
(33, 'DEFAULT', 31, 'bom:edit', '编辑BOM', 'BTN', '', 2),
(34, 'DEFAULT', 31, 'bom:del', '删除BOM', 'BTN', '', 3),

-- 工艺路线
(40, 'DEFAULT', 0, 'process', '工艺路线', 'MENU', '/process', 4),
(41, 'DEFAULT', 40, 'process:route', '工艺路线管理', 'MENU', '/process/route', 10),
(42, 'DEFAULT', 41, 'process:route:add', '新增路线', 'BTN', '', 1),
(43, 'DEFAULT', 41, 'process:route:edit', '编辑路线', 'BTN', '', 2),
(44, 'DEFAULT', 41, 'process:route:del', '删除路线', 'BTN', '', 3),
(45, 'DEFAULT', 41, 'process:route:publish', '发布路线', 'BTN', '', 4),

-- 订单管理
(50, 'DEFAULT', 0, 'order', '订单管理', 'MENU', '/order', 5),
(51, 'DEFAULT', 50, 'order:list', '订单列表', 'MENU', '/order/list', 10),
(52, 'DEFAULT', 51, 'order:add', '新增订单', 'BTN', '', 1),
(53, 'DEFAULT', 51, 'order:edit', '编辑订单', 'BTN', '', 2),
(54, 'DEFAULT', 51, 'order:del', '删除订单', 'BTN', '', 3),
(55, 'DEFAULT', 51, 'order:submit', '提交审批', 'BTN', '', 4),
(56, 'DEFAULT', 51, 'order:release', '订单下达', 'BTN', '', 5),
(57, 'DEFAULT', 51, 'order:close', '订单关闭', 'BTN', '', 6),

-- 审批管理
(60, 'DEFAULT', 0, 'approval', '审批管理', 'MENU', '/approval', 6),
(61, 'DEFAULT', 60, 'approval:todo', '待审批', 'MENU', '/approval/todo', 10),
(62, 'DEFAULT', 60, 'approval:done', '已审批', 'MENU', '/approval/done', 20),
(63, 'DEFAULT', 60, 'approval:template', '审批模板', 'MENU', '/approval/template', 30),

-- 排程管理
(70, 'DEFAULT', 0, 'schedule', '生产排程', 'MENU', '/schedule', 7),
(71, 'DEFAULT', 70, 'schedule:plan', '计划管理', 'MENU', '/schedule/plan', 10),
(72, 'DEFAULT', 70, 'schedule:task', '任务管理', 'MENU', '/schedule/task', 20),
(73, 'DEFAULT', 70, 'schedule:workstation', '工位管理', 'MENU', '/schedule/workstation', 30),

-- 仓储管理
(80, 'DEFAULT', 0, 'warehouse', '仓储管理', 'MENU', '/warehouse', 8),
(81, 'DEFAULT', 80, 'warehouse:inventory', '库存查询', 'MENU', '/warehouse/inventory', 10),
(82, 'DEFAULT', 80, 'warehouse:inbound', '入库管理', 'MENU', '/warehouse/inbound', 20),
(83, 'DEFAULT', 80, 'warehouse:outbound', '出库管理', 'MENU', '/warehouse/outbound', 30);

-- 角色-权限关联 (管理员拥有所有权限)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 生产经理权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 20), (2, 21), (2, 22), (2, 23), (2, 24), (2, 25), (2, 26), (2, 27),
(2, 30), (2, 31), (2, 32), (2, 33), (2, 34),
(2, 40), (2, 41), (2, 42), (2, 43), (2, 44), (2, 45),
(2, 50), (2, 51), (2, 52), (2, 53), (2, 54), (2, 55), (2, 56), (2, 57),
(2, 60), (2, 61), (2, 62),
(2, 70), (2, 71), (2, 72), (2, 73);

-- 仓库管理员权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 80), (3, 81), (3, 82), (3, 83);

-- 操作员权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(4, 20), (4, 22),
(4, 70), (4, 71), (4, 72);

-- ============================================
-- 二、admin 用户初始化 (密码: admin123)
-- SHA-256(userId + salt + rawPassword)
-- 首次保存时自动计算
-- ============================================
-- 由于密码加密需要先有userId，需要分两步:
INSERT INTO sys_user (id, tenant_id, username, password, salt, real_name, status)
VALUES (1, 'DEFAULT', 'admin', 'TEMP', 'INIT', '系统管理员', 1);

-- 更新为正确的密码 (userId=1, salt=INIT, password=admin123)
UPDATE sys_user SET password = SHA2(CONCAT('1', 'INIT', 'admin123'), 256), salt = 'INIT' WHERE id = 1;

-- 管理员角色分配
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- ============================================
-- 三、系统配置
-- ============================================

INSERT INTO sys_config (tenant_id, config_key, config_value, config_desc) VALUES
('DEFAULT', 'system.name', 'MES Core', '系统名称'),
('DEFAULT', 'system.version', '1.0.0', '系统版本'),
('DEFAULT', 'system.logo', '', '系统Logo');

-- ============================================
-- 四、流水号初始化
-- ============================================

INSERT INTO sys_serial_number (tenant_id, prefix, biz_type, date_part, current_seq) VALUES
('DEFAULT', 'MO',   'ORDER',    DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MP',   'PLAN',     DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MT',   'TASK',     DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MI',   'INBOUND',  DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'MOUT', 'OUTBOUND', DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'PR',   'PROCESS_ROUTE', DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'AT',   'APPROVAL_TEMPLATE', DATE_FORMAT(NOW(),'%Y%m%d'), 0),
('DEFAULT', 'PRQ',  'PURCHASE', DATE_FORMAT(NOW(),'%Y%m%d'), 0);

-- ============================================
-- 五、基础数据初始化
-- ============================================

-- 计量单位
INSERT INTO mes_unit (id, tenant_id, unit_code, unit_name) VALUES
(1, 'DEFAULT', 'PCS', '个'),
(2, 'DEFAULT', 'KG', '千克'),
(3, 'DEFAULT', 'M', '米'),
(4, 'DEFAULT', 'SET', '套'),
(5, 'DEFAULT', 'BOX', '箱'),
(6, 'DEFAULT', 'L', '升');

-- 物料分类
INSERT INTO mes_material_category (id, tenant_id, parent_id, category_code, category_name, sort) VALUES
(1, 'DEFAULT', 0, 'RAW', '原材料', 1),
(2, 'DEFAULT', 0, 'SEMI', '半成品', 2),
(3, 'DEFAULT', 0, 'FINISHED', '成品', 3),
(4, 'DEFAULT', 0, 'PACK', '包装材料', 4);

-- 工作中心
INSERT INTO mes_work_center (id, tenant_id, center_code, center_name, center_type) VALUES
(1, 'DEFAULT', 'WC001', '切割中心', 'PRODUCTION'),
(2, 'DEFAULT', 'WC002', '焊接中心', 'PRODUCTION'),
(3, 'DEFAULT', 'WC003', '装配中心', 'PRODUCTION'),
(4, 'DEFAULT', 'WC004', '质检中心', 'QC'),
(5, 'DEFAULT', 'WC005', '包装中心', 'PACKING');

-- 工位
INSERT INTO mes_workstation (id, work_center_id, station_code, station_name, capacity) VALUES
(1, 1, 'WS-CUT-01', '切割工位1', 500),
(2, 1, 'WS-CUT-02', '切割工位2', 500),
(3, 2, 'WS-WELD-01', '焊接工位1', 400),
(4, 2, 'WS-WELD-02', '焊接工位2', 400),
(5, 3, 'WS-ASM-01', '装配工位1', 300),
(6, 3, 'WS-ASM-02', '装配工位2', 300),
(7, 4, 'WS-QC-01', '质检工位1', 800),
(8, 5, 'WS-PACK-01', '包装工位1', 1000);

-- 仓库
INSERT INTO mes_warehouse (id, tenant_id, warehouse_code, warehouse_name, warehouse_type) VALUES
(1, 'DEFAULT', 'WH001', '原材料仓库', 'NORMAL'),
(2, 'DEFAULT', 'WH002', '成品仓库', 'NORMAL'),
(3, 'DEFAULT', 'WH003', '半成品仓库', 'NORMAL');

-- 库位
INSERT INTO mes_storage_location (id, warehouse_id, location_code, location_type) VALUES
(1, 1, 'A-01-01', 'NORMAL'),
(2, 1, 'A-01-02', 'NORMAL'),
(3, 2, 'B-01-01', 'NORMAL'),
(4, 2, 'B-01-02', 'NORMAL'),
(5, 3, 'C-01-01', 'NORMAL');

-- ============================================
-- 六、审批模板初始化
-- ============================================

-- 标准生产订单审批模板
INSERT INTO mes_approval_template (id, tenant_id, template_code, template_name, biz_type, biz_category, priority, is_default, status) VALUES
(1, 'DEFAULT', 'ORDER_NORMAL', '标准生产订单审批', 'ORDER', 'NORMAL', 10, 1, 'PUBLISHED');

INSERT INTO mes_approval_node (template_id, node_seq, node_name, assignee_type, assignee_id) VALUES
(1, 10, '部门主管审批', 'ROLE', 2),
(1, 20, '生产经理审批', 'ROLE', 2),
(1, 30, '质量经理审批', 'ROLE', 5);

-- 急单审批模板
INSERT INTO mes_approval_template (id, tenant_id, template_code, template_name, biz_type, biz_category, priority, is_default, status) VALUES
(2, 'DEFAULT', 'ORDER_URGENT', '急单快速审批', 'ORDER', 'URGENT', 100, 0, 'PUBLISHED');

INSERT INTO mes_approval_node (template_id, node_seq, node_name, assignee_type, assignee_id) VALUES
(2, 10, '生产经理审批', 'ROLE', 2),
(2, 20, '质量经理审批', 'ROLE', 5);

-- 采购申请审批模板（默认发布）
INSERT INTO mes_approval_template (id, tenant_id, template_code, template_name, biz_type, biz_category, priority, is_default, status) VALUES
(3, 'DEFAULT', 'PURCHASE_NORMAL', '标准采购申请审批', 'PURCHASE', 'NORMAL', 10, 1, 'PUBLISHED');

INSERT INTO mes_approval_node (template_id, node_seq, node_name, assignee_type, assignee_id) VALUES
(3, 10, '采购主管审批', 'ROLE', 2),
(3, 20, '财务审批', 'ROLE', 3);

-- ============================================
-- 七、补充前端页面菜单权限（erp同步/发起审批/采购申请）
-- ============================================
INSERT INTO sys_permission (id, tenant_id, parent_id, perm_code, perm_name, perm_type, url, sort) VALUES
(90, 'DEFAULT', 50, 'order:erp', 'ERP订单同步', 'MENU', '/order/erp-sync', 30),
(91, 'DEFAULT', 60, 'approval:launch', '发起审批', 'MENU', '/approval/launch', 15),
(92, 'DEFAULT', 0, 'purchase', '采购管理', 'MENU', '/purchase', 9),
(93, 'DEFAULT', 92, 'purchase:requisition', '采购申请', 'MENU', '/purchase/requisition', 10),
(94, 'DEFAULT', 92, 'purchase:add', '新增采购申请', 'BTN', '', 1),
(95, 'DEFAULT', 92, 'purchase:submit', '提交采购审批', 'BTN', '', 2),
(96, 'DEFAULT', 92, 'purchase:del', '删除采购申请', 'BTN', '', 3);

-- 管理员拥有全部权限（含新增菜单）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 90 AND 96;

-- 生产经理额外授权：ERP同步、发起审批、采购申请
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 90), (2, 91), (2, 92), (2, 93), (2, 94), (2, 95), (2, 96);

-- ============================================
-- 八、补充按钮级权限（仓库/审批模板/物料分类/权限/角色/排程）
-- ============================================
INSERT INTO sys_permission (id, tenant_id, parent_id, perm_code, perm_name, perm_type, url, sort) VALUES
-- 审批模板
(100, 'DEFAULT', 63, 'approval:template:add', '新增模板', 'BTN', '', 1),
(101, 'DEFAULT', 63, 'approval:template:edit', '编辑模板', 'BTN', '', 2),
(102, 'DEFAULT', 63, 'approval:template:del', '删除模板', 'BTN', '', 3),
(103, 'DEFAULT', 63, 'approval:template:publish', '发布模板', 'BTN', '', 4),
-- 仓储管理
(104, 'DEFAULT', 80, 'warehouse:add', '新增仓库', 'BTN', '', 1),
(105, 'DEFAULT', 80, 'warehouse:del', '删除仓库', 'BTN', '', 2),
-- 物料分类
(106, 'DEFAULT', 21, 'material:category:add', '新增分类', 'BTN', '', 1),
(107, 'DEFAULT', 21, 'material:category:edit', '编辑分类', 'BTN', '', 2),
(108, 'DEFAULT', 21, 'material:category:del', '删除分类', 'BTN', '', 3),
-- 角色管理
(109, 'DEFAULT', 6, 'system:role:del', '删除角色', 'BTN', '', 3),
-- 权限管理
(110, 'DEFAULT', 9, 'system:perm:add', '新增权限', 'BTN', '', 1),
(111, 'DEFAULT', 9, 'system:perm:edit', '编辑权限', 'BTN', '', 2),
(112, 'DEFAULT', 9, 'system:perm:del', '删除权限', 'BTN', '', 3),
-- 排程管理
(113, 'DEFAULT', 70, 'schedule:station:add', '新增工位', 'BTN', '', 1),
(114, 'DEFAULT', 70, 'schedule:center:add', '新增工作中心', 'BTN', '', 2);

-- 管理员拥有全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 100 AND 114;

-- 生产经理授权（不含权限管理）
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 100), (2, 101), (2, 102), (2, 103),
(2, 104), (2, 105),
(2, 106), (2, 107), (2, 108),
(2, 109),
(2, 113), (2, 114);

-- ============================================
-- 九、补充订单操作按钮权限
-- ============================================
INSERT INTO sys_permission (id, tenant_id, parent_id, perm_code, perm_name, perm_type, url, sort) VALUES
(115, 'DEFAULT', 51, 'order:start', '订单开工', 'BTN', '', 7),
(116, 'DEFAULT', 51, 'order:finish', '订单完工', 'BTN', '', 8);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 115 AND 116;

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 115), (2, 116);
