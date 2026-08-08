package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_warehouse")
public class Warehouse extends BaseEntity {
    private String warehouseCode;
    private String warehouseName;
    private String warehouseType;
    private String address;
    private String manager;
    private Integer status;
}
