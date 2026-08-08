package com.lucky.mescore.modules.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_order")
public class Order extends BaseEntity {
    private String orderNo;
    private String orderType;
    private String orderStatus;
    private Long materialId;
    private BigDecimal plannedQty;
    private BigDecimal completedQty;
    private Long unitId;
    private String priority;
    private LocalDate planStartDate;
    private LocalDate planEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Long bomId;
    private Long routeId;
    private String customerName;
    private String remark;
}
