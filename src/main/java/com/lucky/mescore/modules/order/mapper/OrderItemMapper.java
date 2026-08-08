package com.lucky.mescore.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.mescore.modules.order.entity.OrderItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Delete("DELETE FROM mes_order_item WHERE order_id = #{orderId}")
    void deleteByOrderId(Long orderId);
}
