package com.lucky.mescore.modules.order.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lucky.mescore.common.enums.OrderStatusEnum;
import com.lucky.mescore.common.serial.SerialNumberService;
import com.lucky.mescore.modules.material.entity.Material;
import com.lucky.mescore.modules.material.entity.Unit;
import com.lucky.mescore.modules.material.mapper.MaterialMapper;
import com.lucky.mescore.modules.material.mapper.UnitMapper;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.mapper.OrderMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 上游 ERP 订单同步服务。
 *
 * 真实项目中这里应通过 HTTP/MQ/中间表对接 ERP；本实现用 {@link ErpOrderMockGateway}
 * 模拟上游数据源，但同步逻辑本身（幂等、主数据翻译、异常隔离）与生产实现一致。
 *
 * 同步进来的订单：
 * - source_type = ERP，source_no 记录 ERP 单号并建唯一索引保证幂等；
 * - 初始状态为 DRAFT，仍需走 MES 内部审批 → 下达流程；
 * - 已存在的 ERP 单号只更新计划日期等可变字段，不重复建单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpOrderSyncService {

    private final ErpOrderMockGateway erpGateway;
    private final OrderMapper orderMapper;
    private final MaterialMapper materialMapper;
    private final UnitMapper unitMapper;
    private final SerialNumberService serialNumberService;

    /**
     * 执行一次同步。
     *
     * @param batchSize 本次从 ERP 拉取的订单数量
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult sync(int batchSize) {
        List<ErpOrderDTO> pulled = erpGateway.pullOrders(batchSize);
        SyncResult result = new SyncResult();
        result.setPulled(pulled.size());

        for (ErpOrderDTO dto : pulled) {
            try {
                if (handleOne(dto)) {
                    result.created++;
                } else {
                    result.skipped++;
                }
            } catch (Exception e) {
                // 单条失败不影响整批，记录原因便于运维排查
                result.failed++;
                result.messages.add(dto.getErpOrderNo() + ": " + e.getMessage());
                log.warn("ERP订单同步失败, erpOrderNo={}, reason={}", dto.getErpOrderNo(), e.getMessage());
            }
        }
        log.info("ERP订单同步完成: {}", result);
        return result;
    }

    /**
     * @return true 表示新建了订单，false 表示已存在被跳过
     */
    private boolean handleOne(ErpOrderDTO dto) {
        if (!StringUtils.hasText(dto.getErpOrderNo())) {
            throw new IllegalArgumentException("ERP单号为空");
        }
        Order exist = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getSourceNo, dto.getErpOrderNo())
                .last("LIMIT 1"));
        if (exist != null) {
            // 幂等：已同步过的单据仅在草稿态刷新计划信息
            if (OrderStatusEnum.DRAFT.getCode().equals(exist.getOrderStatus())) {
                exist.setPlanStartDate(dto.getPlanStartDate());
                exist.setPlanEndDate(dto.getPlanEndDate());
                exist.setSyncTime(LocalDateTime.now());
                orderMapper.updateById(exist);
            }
            return false;
        }

        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialCode, dto.getMaterialCode())
                .last("LIMIT 1"));
        if (material == null) {
            throw new IllegalStateException("MES中不存在物料编码 " + dto.getMaterialCode());
        }

        Long unitId = resolveUnitId(dto.getUnitCode(), material.getPrimaryUnitId());

        Order order = new Order();
        order.setOrderNo(serialNumberService.generate("ORDER", "MO"));
        order.setOrderType("PRODUCTION");
        order.setOrderStatus(OrderStatusEnum.DRAFT.getCode());
        order.setMaterialId(material.getId());
        order.setPlannedQty(dto.getQuantity());
        order.setCompletedQty(BigDecimal.ZERO);
        order.setUnitId(unitId);
        order.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "NORMAL");
        order.setPlanStartDate(dto.getPlanStartDate());
        order.setPlanEndDate(dto.getPlanEndDate());
        order.setCustomerName(dto.getCustomerName());
        order.setRemark(dto.getRemark());
        order.setSourceType("ERP");
        order.setSourceNo(dto.getErpOrderNo());
        order.setSyncTime(LocalDateTime.now());
        orderMapper.insert(order);
        return true;
    }

    private Long resolveUnitId(String unitCode, Long fallback) {
        if (StringUtils.hasText(unitCode)) {
            Unit unit = unitMapper.selectOne(new LambdaQueryWrapper<Unit>()
                    .eq(Unit::getUnitCode, unitCode)
                    .last("LIMIT 1"));
            if (unit != null) {
                return unit.getId();
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("无法确定计量单位: " + unitCode);
        }
        return fallback;
    }

    /** 同步结果统计 */
    @Getter
    public static class SyncResult {
        private int pulled;
        private int created;
        private int skipped;
        private int failed;
        private final List<String> messages = new ArrayList<>();

        void setPulled(int pulled) {
            this.pulled = pulled;
        }

        @Override
        public String toString() {
            return "拉取" + pulled + "条, 新建" + created + "条, 跳过" + skipped + "条, 失败" + failed + "条";
        }
    }
}
