package com.lucky.mescore.modules.order.listener;

import com.lucky.mescore.modules.approval.event.ApprovalFinishedEvent;
import com.lucky.mescore.modules.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监听审批结果，驱动订单状态机。
 *
 * 用 @EventListener 同步执行，与审批操作处于同一事务，
 * 保证「审批通过」与「订单下达」的原子性。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderApprovalListener {

    private static final String BIZ_TYPE_ORDER = "ORDER";

    private final OrderService orderService;

    @EventListener
    public void onApprovalFinished(ApprovalFinishedEvent event) {
        if (!BIZ_TYPE_ORDER.equals(event.getBizType())) {
            return;
        }
        log.info("收到订单审批结果事件, orderId={}, approved={}", event.getBizId(), event.isApproved());
        orderService.onApprovalFinished(event.getBizId(), event.isApproved());
    }
}
