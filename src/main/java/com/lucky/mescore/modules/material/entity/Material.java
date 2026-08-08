package com.lucky.mescore.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material")
public class Material extends BaseEntity {
    private String materialCode;
    private String materialName;
    private String materialSpec;
    private String drawingNo;
    private Long categoryId;
    private Long primaryUnitId;
    private Long auxiliaryUnitId;
    private BigDecimal conversionRate;
    private String materialType;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    private Integer status;
    private String remark;
}
