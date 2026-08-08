package com.lucky.mescore.modules.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_bom")
public class Bom extends BaseEntity {
    private String bomCode;
    private String bomName;
    private Long materialId;
    private String version;
    private LocalDate effectiveDate;
    private LocalDate expiredDate;
    private Integer status;
    private String remark;
}
