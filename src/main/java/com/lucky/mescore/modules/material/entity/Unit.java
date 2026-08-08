package com.lucky.mescore.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_unit")
public class Unit extends BaseEntity {
    private String unitCode;
    private String unitName;
    private Integer status;
}
