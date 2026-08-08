package com.lucky.mescore.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_storage_location")
public class StorageLocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long warehouseId;
    private String locationCode;
    private String locationType;
    private BigDecimal maxCapacity;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
