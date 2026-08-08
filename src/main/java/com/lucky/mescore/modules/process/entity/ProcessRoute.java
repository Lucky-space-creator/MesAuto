package com.lucky.mescore.modules.process.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_process_route")
public class ProcessRoute extends BaseEntity {
    private String routeCode;
    private String routeName;
    private Long materialId;
    private String version;
    private String status;
    private LocalDate effectiveDate;
    private LocalDate expiredDate;
    private String remark;
}
