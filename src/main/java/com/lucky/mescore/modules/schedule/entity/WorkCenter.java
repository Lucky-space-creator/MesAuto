package com.lucky.mescore.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lucky.mescore.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_work_center")
public class WorkCenter extends BaseEntity {
    private String centerCode;
    private String centerName;
    private String centerType;
    private String manager;
    private Integer status;
}
