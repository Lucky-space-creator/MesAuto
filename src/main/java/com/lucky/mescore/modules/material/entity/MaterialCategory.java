package com.lucky.mescore.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_material_category")
public class MaterialCategory extends BaseEntity {
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer sort;
    private Integer status;
}
