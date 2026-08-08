package com.lucky.mescore.modules.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lucky.mescore.common.enums.OrderStatusEnum;
import com.lucky.mescore.common.exception.BusinessException;
import com.lucky.mescore.modules.approval.service.ApprovalEngineService;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.entity.OrderItem;
import com.lucky.mescore.modules.order.mapper.OrderItemMapper;
import com.lucky.mescore.modules.order.mapper.OrderMapper;
import com.lucky.mescore.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemMapper orderItemMapper;
    private final ApprovalEngineService approvalEngine;

    private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> STATE_MACHINE = Map.of(
            OrderStatusEnum.DRAFT, Set.of(OrderStatusEnum.APPROVING, OrderStatusEnum.CLOSED),
            OrderStatusEnum.APPROVING, Set.of(OrderStatusEnum.RELEASED, OrderStatusEnum.DRAFT, OrderStatusEnum.CLOSED),
            OrderStatusEnum.RELEASED, Set.of(OrderStatusEnum.IN_PRODUCTION, OrderStatusEnum.CLOSED),
            OrderStatusEnum.IN_PRODUCTION, Set.of(OrderStatusEnum.PENDING_STORAGE, OrderStatusEnum.CLOSED),
            OrderStatusEnum.PENDING_STORAGE, Set.of(OrderStatusEnum.COMPLETED),
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
        order.setOrderStatus(OrderStatusEnum.DRAFT.getCode());
        save(order);
        saveItems(order.getId(), items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderWithItems(Order order, List<OrderItem> items) {
        Order exist = getById(order.getId());
        if (!OrderStatusEnum.DRAFT.getCode().equals(exist.getOrderStatus())) {
            throw new BusinessException("仅草稿状态可编辑");
        }
        order.setOrderStatus(null);
        updateById(order);
        if (items != null) {
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
