package com.lucky.mescore.modules.material.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_bom_item")
public class BomItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bomId;
    private Long childMaterialId;
    private BigDecimal quantity;
    private Long unitId;
    private BigDecimal scrapRate;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;
}
