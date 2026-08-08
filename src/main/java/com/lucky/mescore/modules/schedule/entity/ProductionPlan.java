package com.lucky.mescore.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_production_plan")
public class ProductionPlan extends BaseEntity {
    private String planNo;
    private Long orderId;
    private BigDecimal totalQty;
    private BigDecimal completedQty;
    private String planStatus;
    private LocalDate planDate;
}
