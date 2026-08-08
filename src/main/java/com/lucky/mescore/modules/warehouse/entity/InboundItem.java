package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_inbound_item")
public class InboundItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inboundId;
    private Long materialId;
    private BigDecimal quantity;
    private Long unitId;
    private Long locationId;
    private String batchNo;
    private String remark;
    private LocalDateTime createTime;
}
