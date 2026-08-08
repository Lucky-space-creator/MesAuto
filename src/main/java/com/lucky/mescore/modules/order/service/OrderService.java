package com.lucky.mescore.modules.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lucky.mescore.modules.order.entity.Order;
import com.lucky.mescore.modules.order.entity.OrderItem;

import java.util.List;

public interface OrderService extends IService<Order> {

    List<OrderItem> getOrderItems(Long orderId);

    void saveOrderWithItems(Order order, List<OrderItem> items);

    void updateOrderWithItems(Order order, List<OrderItem> items);

    void submitApproval(Long orderId, String applicant);

    void release(Long orderId);

    void startProduction(Long orderId);

    void completeProduction(Long orderId);

    void complete(Long orderId);

    void close(Long orderId);
}
