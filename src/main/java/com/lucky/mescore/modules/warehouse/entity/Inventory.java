package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tenantId;
    private Long materialId;
    private Long warehouseId;
    private Long locationId;
    private BigDecimal quantity;
    private BigDecimal lockedQuantity;
    private Long unitId;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String locationCode;
    @TableField(exist = false)
    private String unitName;
}
