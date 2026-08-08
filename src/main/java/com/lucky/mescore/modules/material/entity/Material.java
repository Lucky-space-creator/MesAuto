package com.lucky.mescore.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
    /** 单位名称，非数据库字段，仅用于前端展示 */
    @TableField(exist = false)
    private String unitName;
    /** 分类名称，非数据库字段，仅用于前端展示 */
    @TableField(exist = false)
    private String categoryName;
    private Long auxiliaryUnitId;
    private BigDecimal conversionRate;
    private String materialType;
    private BigDecimal minStock;
    private BigDecimal maxStock;
    /** 默认放置仓库ID */
    private Long defaultWarehouseId;
    /** 默认放置库位ID */
    private Long defaultLocationId;
    private Integer status;
    private String remark;
    /** 默认仓库名称，非数据库字段，仅用于前端展示 */
    @TableField(exist = false)
    private String warehouseName;
    /** 默认库位编码，非数据库字段，仅用于前端展示 */
    @TableField(exist = false)
    private String locationCode;
}
