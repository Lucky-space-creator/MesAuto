package com.lucky.mescore.modules.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.editable.EditableFieldGuard;
import com.lucky.mescore.common.enums.OrderStatusEnum;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.common.serial.SerialNumberService;
import com.lucky.mescore.modules.approval.service.ApprovalEngineService;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.entity.OrderItem;
import com.lucky.mescore.modules.order.mapper.OrderItemMapper;
import com.lucky.mescore.modules.order.mapper.OrderMapper;
import com.lucky.mescore.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemMapper orderItemMapper;
    private final ApprovalEngineService approvalEngine;
    private final SerialNumberService serialNumberService;
    private final EditableFieldGuard editableFieldGuard;

    private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> STATE_MACHINE = Map.of(
            OrderStatusEnum.DRAFT, Set.of(OrderStatusEnum.APPROVING, OrderStatusEnum.CLOSED),
            OrderStatusEnum.APPROVING, Set.of(OrderStatusEnum.RELEASED, OrderStatusEnum.DRAFT, OrderStatusEnum.CLOSED),
            OrderStatusEnum.RELEASED, Set.of(OrderStatusEnum.IN_PRODUCTION, OrderStatusEnum.CLOSED),
            OrderStatusEnum.IN_PRODUCTION, Set.of(OrderStatusEnum.PENDING_STORAGE, OrderStatusEnum.CLOSED),
            OrderStatusEnum.PENDING_STORAGE, Set.of(OrderStatusEnum.COMPLETED, OrderStatusEnum.CLOSED),
            OrderStatusEnum.COMPLETED, Set.of(OrderStatusEnum.CLOSED)
    );

    @Override
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrderWithItems(Order order, List<OrderItem> items) {
        validateOrder(order);
        // 订单号为空时自动生成，避免唯一非空约束报错
        if (!StringUtils.hasText(order.getOrderNo())) {
            order.setOrderNo(serialNumberService.generate("ORDER", "MO"));
        }
        order.setOrderStatus(OrderStatusEnum.DRAFT.getCode());
        if (order.getCompletedQty() == null) {
            order.setCompletedQty(BigDecimal.ZERO);
        }
        if (!StringUtils.hasText(order.getSourceType())) {
            order.setSourceType("MANUAL");
        }
        save(order);
        saveItems(order.getId(), items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderWithItems(Order order, List<OrderItem> items) {
        Order exist = getById(order.getId());
        if (exist == null) {
            throw new BusinessException("订单不存在");
        }
        // 字段级管控：逐字段比对，仅放行当前状态允许修改的字段
        Order merged = editableFieldGuard.applyEditable(order, exist, exist.getOrderStatus());
        validateOrder(merged);
        updateById(merged);

        if (items != null) {
            // 明细结构变更影响物料需求，仅草稿态允许
            if (!OrderStatusEnum.DRAFT.getCode().equals(exist.getOrderStatus())) {
                throw new BusinessException("仅草稿状态可修改订单明细");
            }
            orderItemMapper.deleteByOrderId(order.getId());
            saveItems(order.getId(), items);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitApproval(Long orderId, String applicant) {
        Order order = getById(orderId);
        validateStateTransition(order, OrderStatusEnum.APPROVING);
        approvalEngine.submit("ORDER", order.getId(), order.getOrderNo(), applicant);
        order.setOrderStatus(OrderStatusEnum.APPROVING.getCode());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long orderId) {
        Order order = getById(orderId);
        validateStateTransition(order, OrderStatusEnum.RELEASED);
        order.setOrderStatus(OrderStatusEnum.RELEASED.getCode());
        order.setActualStartDate(LocalDate.now());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProduction(Long orderId) {
        Order order = getById(orderId);
        validateStateTransition(order, OrderStatusEnum.IN_PRODUCTION);
        order.setOrderStatus(OrderStatusEnum.IN_PRODUCTION.getCode());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeProduction(Long orderId) {
        Order order = getById(orderId);
        validateStateTransition(order, OrderStatusEnum.PENDING_STORAGE);
        order.setOrderStatus(OrderStatusEnum.PENDING_STORAGE.getCode());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId) {
        Order order = getById(orderId);
        validateStateTransition(order, OrderStatusEnum.COMPLETED);
        order.setOrderStatus(OrderStatusEnum.COMPLETED.getCode());
        order.setCompletedQty(order.getPlannedQty());
        order.setActualEndDate(LocalDate.now());
        updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long orderId) {
        Order order = getById(orderId);
        validateStateTransition(order, OrderStatusEnum.CLOSED);
        order.setOrderStatus(OrderStatusEnum.CLOSED.getCode());
        updateById(order);
    }

    /**
     * 审批引擎回调：把审批结论落到订单状态上，打通审批与订单。
     * 通过 → 自动下达；驳回 → 退回草稿以便修改后重新提交。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApprovalFinished(Long orderId, boolean approved) {
        Order order = getById(orderId);
        if (order == null) {
            log.warn("审批回调找不到订单, orderId={}", orderId);
            return;
        }
        if (!OrderStatusEnum.APPROVING.getCode().equals(order.getOrderStatus())) {
            log.warn("订单[{}]当前状态[{}]非审批中，忽略审批回调", order.getOrderNo(), order.getOrderStatus());
            return;
        }
        if (approved) {
            order.setOrderStatus(OrderStatusEnum.RELEASED.getCode());
            order.setActualStartDate(LocalDate.now());
        } else {
            order.setOrderStatus(OrderStatusEnum.DRAFT.getCode());
        }
        updateById(order);
        log.info("订单[{}]审批{}，状态更新为{}", order.getOrderNo(), approved ? "通过" : "驳回", order.getOrderStatus());
    }

    @Override
    public List<String> editableFields(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return editableFieldGuard.editableFields(Order.class, order.getOrderStatus());
    }

    private void validateOrder(Order order) {
        if (order.getPlannedQty() == null || order.getPlannedQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("计划数量必须大于0");
        }
        if (order.getPlanStartDate() != null && order.getPlanEndDate() != null
                && order.getPlanStartDate().isAfter(order.getPlanEndDate())) {
            throw new BusinessException("计划开始日期不能晚于结束日期");
        }
    }

    private void validateStateTransition(Order order, OrderStatusEnum target) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderStatusEnum current = OrderStatusEnum.fromCode(order.getOrderStatus());
        if (current == null) {
            throw new BusinessException("未知订单状态: " + order.getOrderStatus());
        }
        if (!STATE_MACHINE.containsKey(current) || !STATE_MACHINE.get(current).contains(target)) {
            throw new BusinessException("不允许从 [" + current.getDesc() + "] 直接变更为 [" + target.getDesc() + "]");
        }
    }

    private void saveItems(Long orderId, List<OrderItem> items) {
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                OrderItem item = items.get(i);
                item.setOrderId(orderId);
                item.setLineNo(i + 1);
                orderItemMapper.insert(item);
            }
        }
    }
}
