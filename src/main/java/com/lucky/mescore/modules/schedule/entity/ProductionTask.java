package com.lucky.mescore.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_production_task")
public class ProductionTask extends BaseEntity {
    private String taskNo;
    private Long planId;
    private Long orderId;
    private Long materialId;
    private Long stepId;
    private Long workstationId;
    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private BigDecimal defectiveQty;
    private Long unitId;
    private String taskStatus;
    private String priority;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String assignee;
    private String remark;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String workstationName;
    @TableField(exist = false)
    private String orderNo;
}
