package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_outbound_order")
public class OutboundOrder extends BaseEntity {
    private String outboundNo;
    private String outboundType;
    private String sourceOrderNo;
    private Long warehouseId;
    private String status;
    private LocalDate outboundDate;
    private String remark;

    @TableField(exist = false)
    private String warehouseName;
}
