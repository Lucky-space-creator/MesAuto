-- ============================================================
-- MES Core 全链路演示数据
-- ============================================================
-- 依赖：先执行 schema.sql 与 init-data.sql
-- 特点：可重复执行（先清理业务数据再插入，主数据/权限不受影响）
-- 覆盖：物料 → BOM → 工艺路线 → 库存 → 订单（7种状态全覆盖）
--       → 审批流 → 生产计划 → 生产任务 → 报工 → 出入库
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 0. 清理既有业务数据（保留 init-data.sql 的主数据与权限）
-- ------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE mes_task_report;
TRUNCATE TABLE mes_production_task;
TRUNCATE TABLE mes_production_plan;
TRUNCATE TABLE mes_approval_record;
TRUNCATE TABLE mes_approval_task;
TRUNCATE TABLE mes_approval_process;
TRUNCATE TABLE mes_order_item;
TRUNCATE TABLE mes_order;
TRUNCATE TABLE mes_inbound_item;
TRUNCATE TABLE mes_inbound_order;
TRUNCATE TABLE mes_outbound_item;
TRUNCATE TABLE mes_outbound_order;
TRUNCATE TABLE mes_inventory;
TRUNCATE TABLE mes_process_step;
TRUNCATE TABLE mes_process_route;
TRUNCATE TABLE mes_bom_item;
TRUNCATE TABLE mes_bom;
TRUNCATE TABLE mes_material;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 1. 物料主数据（3成品 + 4半成品 + 8原材料 + 2包装 = 17条）
--    分类：1=RAW原材料 2=SEMI半成品 3=FINISHED成品 4=PACK包装
--    单位：1=PCS 2=KG 3=M 4=SET 5=BOX 6=L
--    类型：PRODUCT成品 / SEMI半成品 / MATERIAL原材料
-- ------------------------------------------------------------
INSERT INTO mes_material
(id, tenant_id, material_code, material_name, material_spec, drawing_no, category_id, primary_unit_id, material_type, min_stock, max_stock, status, remark) VALUES
-- 成品
(1,  'DEFAULT', 'FG-CHAIR-001',   '人体工学办公椅',   '黑色/网布/可升降', 'DWG-CH-001', 3, 1, 'PRODUCT',  10, 500,  1, 'ERP可下单成品'),
(2,  'DEFAULT', 'FG-DESK-001',    '升降办公桌',       '1400x700/电动',    'DWG-DK-001', 3, 1, 'PRODUCT',  10, 300,  1, 'ERP可下单成品'),
(3,  'DEFAULT', 'FG-CABINET-001', '三抽文件柜',       '400x500x600/钢制', 'DWG-CB-001', 3, 1, 'PRODUCT',  5,  200,  1, 'ERP可下单成品'),
-- 半成品
(11, 'DEFAULT', 'SF-FRAME-001',   '椅子金属骨架',     'Q235钢管焊接',     'DWG-FR-001', 2, 1, 'SEMI',     20, 800,  1, NULL),
(12, 'DEFAULT', 'SF-SEAT-001',    '座椅坐垫总成',     '高密度海绵+网布',  'DWG-ST-001', 2, 1, 'SEMI',     20, 800,  1, NULL),
(13, 'DEFAULT', 'SF-TABLETOP-001','桌面板总成',       '1400x700/密度板',  'DWG-TT-001', 2, 1, 'SEMI',     15, 400,  1, NULL),
(14, 'DEFAULT', 'SF-DRAWER-001',  '抽屉组件',         '钢制/带滑轨',      'DWG-DR-001', 2, 1, 'SEMI',     30, 600,  1, NULL),
-- 原材料
(21, 'DEFAULT', 'RM-STEEL-001',   'Q235钢板',         '2.0mm厚',          NULL, 1, 2, 'MATERIAL', 500, 20000, 1, NULL),
(22, 'DEFAULT', 'RM-TUBE-001',    '钢管',             'Φ25x1.5mm',        NULL, 1, 3, 'MATERIAL', 300, 10000, 1, NULL),
(23, 'DEFAULT', 'RM-FOAM-001',    '高密度海绵',       '50D',              NULL, 1, 2, 'MATERIAL', 100, 3000,  1, NULL),
(24, 'DEFAULT', 'RM-MESH-001',    '弹性网布',         '黑色/1.5m幅宽',    NULL, 1, 3, 'MATERIAL', 200, 5000,  1, NULL),
(25, 'DEFAULT', 'RM-BOARD-001',   '密度板',           '18mm/E1级',        NULL, 1, 1, 'MATERIAL', 100, 2000,  1, NULL),
(26, 'DEFAULT', 'RM-MOTOR-001',   '升降电机',         '24V/双段',         NULL, 1, 1, 'MATERIAL', 50,  1000,  1, NULL),
(27, 'DEFAULT', 'RM-SLIDE-001',   '抽屉滑轨',         '450mm/三节',       NULL, 1, 4, 'MATERIAL', 100, 3000,  1, NULL),
(28, 'DEFAULT', 'RM-SCREW-001',   '标准螺钉',         'M6x20',            NULL, 1, 1, 'MATERIAL', 2000,50000, 1, NULL),
-- 包装
(31, 'DEFAULT', 'PK-CARTON-001',  '纸箱',             '750x600x400',      NULL, 4, 1, 'MATERIAL', 200, 5000,  1, NULL),
(32, 'DEFAULT', 'PK-FILM-001',    '缠绕膜',           '500mm宽',          NULL, 4, 2, 'MATERIAL', 50,  1000,  1, NULL);

-- ------------------------------------------------------------
-- 2. BOM（3个成品各一套，含多层结构）
-- ------------------------------------------------------------
INSERT INTO mes_bom (id, tenant_id, bom_code, bom_name, material_id, version, effective_date, status, remark) VALUES
(1, 'DEFAULT', 'BOM-CHAIR-001',   '办公椅BOM',   1,  'V1.0', CURDATE(), 1, '成品级BOM'),
(2, 'DEFAULT', 'BOM-DESK-001',    '升降桌BOM',   2,  'V1.0', CURDATE(), 1, '成品级BOM'),
(3, 'DEFAULT', 'BOM-CABINET-001', '文件柜BOM',   3,  'V1.0', CURDATE(), 1, '成品级BOM'),
(4, 'DEFAULT', 'BOM-FRAME-001',   '椅架BOM',     11, 'V1.0', CURDATE(), 1, '半成品级BOM'),
(5, 'DEFAULT', 'BOM-SEAT-001',    '坐垫总成BOM', 12, 'V1.0', CURDATE(), 1, '半成品级BOM');

INSERT INTO mes_bom_item (bom_id, child_material_id, quantity, unit_id, scrap_rate, sort, remark) VALUES
-- 办公椅 = 骨架 + 坐垫 + 螺钉 + 纸箱
(1, 11, 1,   1, 0.0100, 1, '金属骨架'),
(1, 12, 1,   1, 0.0100, 2, '坐垫总成'),
(1, 28, 12,  1, 0.0500, 3, '装配螺钉'),
(1, 31, 1,   1, 0.0200, 4, '包装纸箱'),
-- 升降桌 = 桌面 + 电机 + 钢管 + 螺钉 + 纸箱
(2, 13, 1,   1, 0.0100, 1, '桌面板'),
(2, 26, 2,   1, 0.0050, 2, '升降电机'),
(2, 22, 4.5, 3, 0.0300, 3, '支撑钢管'),
(2, 28, 16,  1, 0.0500, 4, '装配螺钉'),
(2, 31, 1,   1, 0.0200, 5, '包装纸箱'),
-- 文件柜 = 钢板 + 抽屉x3 + 螺钉 + 缠绕膜
(3, 21, 8.5, 2, 0.0800, 1, '柜体钢板'),
(3, 14, 3,   1, 0.0100, 2, '抽屉组件'),
(3, 28, 24,  1, 0.0500, 3, '装配螺钉'),
(3, 32, 0.3, 2, 0.0200, 4, '缠绕膜'),
-- 椅架 = 钢管 + 钢板
(4, 22, 3.2, 3, 0.0500, 1, '骨架钢管'),
(4, 21, 1.2, 2, 0.0600, 2, '连接板'),
-- 坐垫总成 = 海绵 + 网布
(5, 23, 1.8, 2, 0.0400, 1, '坐垫海绵'),
(5, 24, 1.5, 3, 0.0500, 2, '表面网布');

-- ------------------------------------------------------------
-- 3. 工艺路线（3条已发布，1条草稿）
--    工作中心：1=切割 2=焊接 3=装配 4=质检 5=包装
-- ------------------------------------------------------------
INSERT INTO mes_process_route (id, tenant_id, route_code, route_name, material_id, version, status, effective_date, remark) VALUES
(1, 'DEFAULT', 'PR-CHAIR-001',   '办公椅标准工艺',   1, 'V1.0', 'PUBLISHED', CURDATE(), '5道工序'),
(2, 'DEFAULT', 'PR-DESK-001',    '升降桌标准工艺',   2, 'V1.0', 'PUBLISHED', CURDATE(), '4道工序'),
(3, 'DEFAULT', 'PR-CABINET-001', '文件柜标准工艺',   3, 'V1.0', 'PUBLISHED', CURDATE(), '5道工序'),
(4, 'DEFAULT', 'PR-CHAIR-002',   '办公椅优化工艺',   1, 'V2.0', 'DRAFT',     NULL,      '草稿态，用于验证未发布不可排程');

INSERT INTO mes_process_step
(route_id, step_seq, step_code, step_name, work_center_id, operation_type, standard_hours, setup_hours, quality_check, remark) VALUES
-- 办公椅：切割→焊接→装配→质检→包装
(1, 10, 'OP10', '钢管下料',   1, 'CUTTING',   0.50, 0.20, 0, '按图纸切割钢管'),
(1, 20, 'OP20', '骨架焊接',   2, 'WELDING',   1.20, 0.30, 0, '焊接椅子骨架'),
(1, 30, 'OP30', '整椅装配',   3, 'ASSEMBLY',  0.80, 0.15, 0, '骨架与坐垫装配'),
(1, 40, 'OP40', '成品质检',   4, 'INSPECT',   0.30, 0.05, 1, '关键质检点'),
(1, 50, 'OP50', '包装入库',   5, 'PACKING',   0.25, 0.05, 0, '装箱'),
-- 升降桌：切割→装配→质检→包装
(2, 10, 'OP10', '板材开料',   1, 'CUTTING',   0.60, 0.20, 0, '桌面板开料'),
(2, 20, 'OP20', '桌体装配',   3, 'ASSEMBLY',  1.50, 0.25, 0, '安装电机与支架'),
(2, 30, 'OP30', '功能测试',   4, 'INSPECT',   0.40, 0.10, 1, '升降功能测试'),
(2, 40, 'OP40', '包装入库',   5, 'PACKING',   0.30, 0.05, 0, '装箱'),
-- 文件柜：切割→焊接→装配→质检→包装
(3, 10, 'OP10', '钢板剪切',   1, 'CUTTING',   0.70, 0.25, 0, '柜体钢板剪切'),
(3, 20, 'OP20', '柜体焊接',   2, 'WELDING',   1.40, 0.30, 0, '柜体成型焊接'),
(3, 30, 'OP30', '抽屉装配',   3, 'ASSEMBLY',  1.00, 0.20, 0, '安装抽屉与滑轨'),
(3, 40, 'OP40', '外观质检',   4, 'INSPECT',   0.35, 0.05, 1, '外观与滑动检查'),
(3, 50, 'OP50', '缠膜入库',   5, 'PACKING',   0.20, 0.05, 0, '缠绕膜包装'),
-- 草稿路线
(4, 10, 'OP10', '钢管下料',   1, 'CUTTING',   0.40, 0.15, 0, '优化后工时'),
(4, 20, 'OP20', '骨架焊接',   2, 'WELDING',   1.00, 0.25, 0, '优化后工时');

-- ------------------------------------------------------------
-- 4. 库存（原材料备货充足，半成品少量，成品少量）
--    仓库：1=原材料仓 2=成品仓 3=半成品仓
--    库位：1,2=A区(原材料) 3,4=B区(成品) 5=C区(半成品)
-- ------------------------------------------------------------
INSERT INTO mes_inventory (tenant_id, material_id, warehouse_id, location_id, quantity, locked_quantity, unit_id, status) VALUES
-- 原材料仓
('DEFAULT', 21, 1, 1, 8500.000000,  200.000000, 2, 1),
('DEFAULT', 22, 1, 1, 4200.000000,  150.000000, 3, 1),
('DEFAULT', 23, 1, 2, 1200.000000,   50.000000, 2, 1),
('DEFAULT', 24, 1, 2, 2600.000000,   80.000000, 3, 1),
('DEFAULT', 25, 1, 1,  900.000000,    0.000000, 1, 1),
('DEFAULT', 26, 1, 2,  480.000000,   20.000000, 1, 1),
('DEFAULT', 27, 1, 2, 1500.000000,    0.000000, 4, 1),
('DEFAULT', 28, 1, 1, 32000.000000, 500.000000, 1, 1),
('DEFAULT', 31, 1, 1, 2400.000000,    0.000000, 1, 1),
('DEFAULT', 32, 1, 2,  620.000000,    0.000000, 2, 1),
-- 半成品仓
('DEFAULT', 11, 3, 5,  180.000000,   30.000000, 1, 1),
('DEFAULT', 12, 3, 5,  220.000000,   40.000000, 1, 1),
('DEFAULT', 13, 3, 5,   95.000000,    0.000000, 1, 1),
('DEFAULT', 14, 3, 5,  310.000000,   60.000000, 1, 1),
-- 成品仓
('DEFAULT', 1,  2, 3,   65.000000,   10.000000, 1, 1),
('DEFAULT', 2,  2, 3,   28.000000,    5.000000, 1, 1),
('DEFAULT', 3,  2, 4,   42.000000,    0.000000, 1, 1);

-- ------------------------------------------------------------
-- 5. 订单（10条，覆盖全部7种状态 + 手工/ERP两种来源）
--    状态：DRAFT / APPROVING / RELEASED / IN_PRODUCTION
--          PENDING_STORAGE / COMPLETED / CLOSED
-- ------------------------------------------------------------
INSERT INTO mes_order
(id, tenant_id, order_no, order_type, order_status, material_id, planned_qty, completed_qty, unit_id,
 priority, plan_start_date, plan_end_date, actual_start_date, actual_end_date,
 bom_id, route_id, customer_name, remark, source_type, source_no, sync_time, create_by) VALUES
-- 草稿：可编辑，可提交审批
(1, 'DEFAULT', 'MO20260101001', 'PRODUCTION', 'DRAFT', 1, 100, 0, 1,
 'NORMAL', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL, NULL,
 1, 1, '华东制造集团', '手工创建-草稿态演示', 'MANUAL', NULL, NULL, 'admin'),
-- 草稿：ERP同步进来的
(2, 'DEFAULT', 'MO20260101002', 'PRODUCTION', 'DRAFT', 2, 50, 0, 1,
 'URGENT', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 7 DAY), NULL, NULL,
 2, 2, '南方家居有限公司', 'ERP自动下发', 'ERP', 'ERP20260101888', NOW(), 'erp-sync'),
-- 审批中：等待审批，不可编辑主数据
(3, 'DEFAULT', 'MO20260101003', 'PRODUCTION', 'APPROVING', 3, 80, 0, 1,
 'NORMAL', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 12 DAY), NULL, NULL,
 3, 3, '北方办公科技', '审批中态演示', 'MANUAL', NULL, NULL, 'admin'),
-- 已下达：可排程、可调整计划日期
(4, 'DEFAULT', 'MO20260101004', 'PRODUCTION', 'RELEASED', 1, 120, 0, 1,
 'NORMAL', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY), CURDATE(), NULL,
 1, 1, '西部家具连锁', '已下达，可生成排程计划', 'MANUAL', NULL, NULL, 'admin'),
-- 已下达：ERP来源，留给用户点"生成排程"
(5, 'DEFAULT', 'MO20260101005', 'PRODUCTION', 'RELEASED', 2, 60, 0, 1,
 'URGENT', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 9 DAY), CURDATE(), NULL,
 2, 2, '华东制造集团', 'ERP下发并已审批下达', 'ERP', 'ERP20260101889', NOW(), 'erp-sync'),
-- 生产中：已有计划与任务
(6, 'DEFAULT', 'MO20260101006', 'PRODUCTION', 'IN_PRODUCTION', 1, 200, 80, 1,
 'NORMAL', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), NULL,
 1, 1, '南方家居有限公司', '生产中，已部分报工', 'MANUAL', NULL, NULL, 'admin'),
-- 生产中：文件柜
(7, 'DEFAULT', 'MO20260101007', 'PRODUCTION', 'IN_PRODUCTION', 3, 150, 45, 1,
 'NORMAL', DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY), NULL,
 3, 3, '北方办公科技', '生产中', 'ERP', 'ERP20260101890', NOW(), 'erp-sync'),
-- 待入库：生产完工，等待入库确认
(8, 'DEFAULT', 'MO20260101008', 'PRODUCTION', 'PENDING_STORAGE', 2, 40, 40, 1,
 'NORMAL', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), NULL,
 2, 2, '西部家具连锁', '生产完工待入库', 'MANUAL', NULL, NULL, 'admin'),
-- 已完成
(9, 'DEFAULT', 'MO20260101009', 'PRODUCTION', 'COMPLETED', 1, 90, 90, 1,
 'NORMAL', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),
 1, 1, '华东制造集团', '已完成', 'MANUAL', NULL, NULL, 'admin'),
-- 已关闭（终态）
(10,'DEFAULT', 'MO20260101010', 'PRODUCTION', 'CLOSED', 3, 30, 30, 1,
 'LOW', DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 40 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 41 DAY),
 3, 3, '南方家居有限公司', '已关闭归档', 'ERP', 'ERP20251201001', NOW(), 'erp-sync');

-- 订单明细
INSERT INTO mes_order_item (order_id, material_id, quantity, unit_id, line_no, remark) VALUES
(1, 11, 100, 1, 1, '骨架需求'),
(1, 12, 100, 1, 2, '坐垫需求'),
(4, 11, 120, 1, 1, '骨架需求'),
(4, 12, 120, 1, 2, '坐垫需求'),
(6, 11, 200, 1, 1, '骨架需求'),
(6, 12, 200, 1, 2, '坐垫需求'),
(7, 14, 450, 1, 1, '抽屉需求(3件/台)');

-- ------------------------------------------------------------
-- 6. 审批流程（订单3处于审批中，有待办任务）
--    模板1=ORDER_NORMAL(3节点)，节点1,2,3
-- ------------------------------------------------------------
INSERT INTO mes_approval_process
(id, tenant_id, template_id, biz_type, biz_id, biz_no, status, current_node_id, applicant, start_time) VALUES
(1, 'DEFAULT', 1, 'ORDER', 3, 'MO20260101003', 'RUNNING',  1, 'admin', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(2, 'DEFAULT', 1, 'ORDER', 4, 'MO20260101004', 'APPROVED', 3, 'admin', DATE_SUB(NOW(), INTERVAL 3 DAY));

UPDATE mes_approval_process SET end_time = DATE_SUB(NOW(), INTERVAL 2 DAY) WHERE id = 2;

-- 待办任务：订单3停在第1个节点，登录 admin 可在"待办审批"看到
INSERT INTO mes_approval_task
(id, process_id, template_id, node_id, biz_type, biz_id, assignee, status) VALUES
(1, 1, 1, 1, 'ORDER', 3, 'sysadmin', 'PENDING');

-- 订单4的历史审批任务（已全部完成）
INSERT INTO mes_approval_task
(id, process_id, template_id, node_id, biz_type, biz_id, assignee, status) VALUES
(2, 2, 1, 1, 'ORDER', 4, 'sysadmin', 'COMPLETED'),
(3, 2, 1, 2, 'ORDER', 4, 'sysadmin', 'COMPLETED'),
(4, 2, 1, 3, 'ORDER', 4, 'sysadmin', 'COMPLETED');

INSERT INTO mes_approval_record
(process_id, node_id, node_name, assignee, action, comment) VALUES
(2, 1, '部门主管审批', 'sysadmin', 'AGREE', '同意，物料齐套'),
(2, 2, '生产经理审批', 'sysadmin', 'AGREE', '产能可满足'),
(2, 3, '质量经理审批', 'sysadmin', 'AGREE', '质量要求明确，同意下达');

-- ------------------------------------------------------------
-- 7. 生产计划与任务（订单6、7在产）
-- ------------------------------------------------------------
INSERT INTO mes_production_plan
(id, tenant_id, plan_no, order_id, total_qty, completed_qty, plan_status, plan_date) VALUES
(1, 'DEFAULT', 'MP20260101001', 6, 200, 80, 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 5 DAY)),
(2, 'DEFAULT', 'MP20260101002', 7, 150, 45, 'IN_PROGRESS', DATE_SUB(CURDATE(), INTERVAL 3 DAY));

-- 订单6（办公椅，5道工序）→ 工位 1=切割1 3=焊接1 5=装配1 7=质检1 8=包装1
INSERT INTO mes_production_task
(id, tenant_id, task_no, plan_id, order_id, material_id, step_id, workstation_id,
 planned_qty, actual_qty, defective_qty, unit_id, task_status, priority,
 plan_start_time, plan_end_time, actual_start_time, actual_end_time, assignee) VALUES
(1, 'DEFAULT', 'MT20260101001', 1, 6, 1, 1, 1, 200, 200, 2, 1, 'COMPLETED',   'NORMAL',
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), '张三'),
(2, 'DEFAULT', 'MT20260101002', 1, 6, 1, 2, 3, 200, 150, 3, 1, 'IN_PROGRESS', 'NORMAL',
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, '李四'),
(3, 'DEFAULT', 'MT20260101003', 1, 6, 1, 3, 5, 200, 80, 0, 1, 'IN_PROGRESS', 'NORMAL',
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, '王五'),
(4, 'DEFAULT', 'MT20260101004', 1, 6, 1, 4, 7, 200, 0, 0, 1, 'PENDING', 'NORMAL',
 DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), NULL, NULL, NULL),
(5, 'DEFAULT', 'MT20260101005', 1, 6, 1, 5, 8, 200, 0, 0, 1, 'PENDING', 'NORMAL',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), NULL, NULL, NULL),
-- 订单7（文件柜，5道工序），含一个暂停任务用于演示恢复
(6, 'DEFAULT', 'MT20260101006', 2, 7, 3, 10, 1, 150, 150, 1, 1, 'COMPLETED',   'NORMAL',
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), '赵六'),
(7, 'DEFAULT', 'MT20260101007', 2, 7, 3, 11, 3, 150, 45, 2, 1, 'PAUSED', 'NORMAL',
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, '孙七'),
(8, 'DEFAULT', 'MT20260101008', 2, 7, 3, 12, 5, 150, 0, 0, 1, 'PENDING', 'NORMAL',
 DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), NULL, NULL, NULL),
(9, 'DEFAULT', 'MT20260101009', 2, 7, 3, 13, 7, 150, 0, 0, 1, 'PENDING', 'NORMAL',
 DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), NULL, NULL, NULL),
(10,'DEFAULT', 'MT20260101010', 2, 7, 3, 14, 8, 150, 0, 0, 1, 'PENDING', 'NORMAL',
 DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), NULL, NULL, NULL);

-- 报工流水
INSERT INTO mes_task_report (task_id, report_type, report_qty, defective_qty, work_hours, operator, remark) VALUES
(1, 'START',    0,   0, 0.00, '张三', '开始下料'),
(1, 'REPORT',   120, 1, 3.50, '张三', '首批下料完成'),
(1, 'REPORT',   80,  1, 2.40, '张三', '剩余下料完成'),
(1, 'COMPLETE', 0,   0, 0.00, '张三', '下料工序完成'),
(2, 'START',    0,   0, 0.00, '李四', '开始焊接'),
(2, 'REPORT',   90,  2, 5.20, '李四', '第一批焊接'),
(2, 'REPORT',   60,  1, 3.60, '李四', '第二批焊接'),
(3, 'START',    0,   0, 0.00, '王五', '开始装配'),
(3, 'REPORT',   80,  0, 4.10, '王五', '装配进行中'),
(6, 'START',    0,   0, 0.00, '赵六', '开始剪切'),
(6, 'REPORT',   150, 1, 6.30, '赵六', '钢板剪切完成'),
(6, 'COMPLETE', 0,   0, 0.00, '赵六', '剪切工序完成'),
(7, 'START',    0,   0, 0.00, '孙七', '开始焊接'),
(7, 'REPORT',   45,  2, 2.80, '孙七', '部分完成'),
(7, 'PAUSE',    0,   0, 0.00, '孙七', '设备维护，暂停');

-- ------------------------------------------------------------
-- 8. 出入库单据
-- ------------------------------------------------------------
INSERT INTO mes_inbound_order
(id, tenant_id, inbound_no, inbound_type, source_order_no, source_order_id, warehouse_id, status, inbound_date, remark) VALUES
(1, 'DEFAULT', 'MI20260101001', 'PURCHASE',   NULL,            NULL, 1, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 10 DAY), '原材料采购入库'),
(2, 'DEFAULT', 'MI20260101002', 'PRODUCTION', 'MO20260101009', 9,    2, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 9 DAY),  '成品生产入库'),
(3, 'DEFAULT', 'MI20260101003', 'PRODUCTION', 'MO20260101008', 8,    2, 'PENDING',   NULL, '待入库，对应订单MO20260101008');

INSERT INTO mes_inbound_item (inbound_id, material_id, quantity, unit_id, location_id, remark) VALUES
(1, 21, 3000, 2, 1, '钢板入库'),
(1, 22, 2000, 3, 1, '钢管入库'),
(1, 28, 15000,1, 1, '螺钉入库'),
(2, 1,  90,   1, 3, '办公椅成品入库'),
(3, 2,  40,   1, 3, '升降桌待入库');

INSERT INTO mes_outbound_order
(id, tenant_id, outbound_no, outbound_type, source_order_no, warehouse_id, status, outbound_date, remark) VALUES
(1, 'DEFAULT', 'MOUT20260101001', 'PRODUCTION', 'MO20260101006', 1, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 5 DAY), '生产领料'),
(2, 'DEFAULT', 'MOUT20260101002', 'PRODUCTION', 'MO20260101007', 1, 'COMPLETED', DATE_SUB(CURDATE(), INTERVAL 3 DAY), '生产领料'),
(3, 'DEFAULT', 'MOUT20260101003', 'SALE',       NULL,            2, 'PENDING',   NULL, '销售出库待处理');

INSERT INTO mes_outbound_item (outbound_id, material_id, quantity, unit_id, location_id, remark) VALUES
(1, 22, 640,  3, 1, '椅架钢管领料'),
(1, 21, 240,  2, 1, '连接板领料'),
(1, 28, 2400, 1, 1, '螺钉领料'),
(2, 21, 1275, 2, 1, '柜体钢板领料'),
(2, 27, 450,  4, 2, '滑轨领料'),
(3, 1,  20,   1, 3, '办公椅销售出库');

-- ------------------------------------------------------------
-- 9. 重置流水号，避免与上面写死的单号冲突
-- ------------------------------------------------------------
UPDATE sys_serial_number SET current_seq = 100 WHERE biz_type IN ('ORDER','PLAN','TASK','INBOUND','OUTBOUND');

-- ------------------------------------------------------------
-- 完成
-- ------------------------------------------------------------
SELECT '物料' AS 数据表, COUNT(*) AS 条数 FROM mes_material
UNION ALL SELECT 'BOM头',    COUNT(*) FROM mes_bom
UNION ALL SELECT 'BOM明细',  COUNT(*) FROM mes_bom_item
UNION ALL SELECT '工艺路线', COUNT(*) FROM mes_process_route
UNION ALL SELECT '工序',     COUNT(*) FROM mes_process_step
UNION ALL SELECT '库存',     COUNT(*) FROM mes_inventory
UNION ALL SELECT '订单',     COUNT(*) FROM mes_order
UNION ALL SELECT '订单明细', COUNT(*) FROM mes_order_item
UNION ALL SELECT '审批流程', COUNT(*) FROM mes_approval_process
UNION ALL SELECT '审批任务', COUNT(*) FROM mes_approval_task
UNION ALL SELECT '生产计划', COUNT(*) FROM mes_production_plan
UNION ALL SELECT '生产任务', COUNT(*) FROM mes_production_task
UNION ALL SELECT '报工记录', COUNT(*) FROM mes_task_report
UNION ALL SELECT '入库单',   COUNT(*) FROM mes_inbound_order
UNION ALL SELECT '出库单',   COUNT(*) FROM mes_outbound_order;
