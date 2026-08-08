package com.lucky.mescore.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_process_step")
public class ProcessStep {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Integer stepSeq;
    private String stepCode;
    private String stepName;
    private Long workCenterId;
    private String operationType;
    private BigDecimal standardHours;
    private BigDecimal setupHours;
    private Long prevStepId;
    private Long nextStepId;
    private String parallelGroup;
    private Integer qualityCheck;
    private String parameters;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
