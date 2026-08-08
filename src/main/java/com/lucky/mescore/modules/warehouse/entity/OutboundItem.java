package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_outbound_item")
public class OutboundItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long outboundId;
    private Long materialId;
    private BigDecimal quantity;
    private Long unitId;
    private Long locationId;
    private String batchNo;
    private String remark;
    private LocalDateTime createTime;
}
