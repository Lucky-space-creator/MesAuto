package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_inbound_order")
public class InboundOrder extends BaseEntity {
    private String inboundNo;
    private String inboundType;
    private String sourceOrderNo;
    private Long sourceOrderId;
    private Long warehouseId;
    private String status;
    private LocalDate inboundDate;
    private String remark;

    @TableField(exist = false)
    private String warehouseName;
}
