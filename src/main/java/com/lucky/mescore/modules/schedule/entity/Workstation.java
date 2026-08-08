package com.lucky.mescore.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("mes_workstation")
public class Workstation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workCenterId;
    private String stationCode;
    private String stationName;
    private BigDecimal capacity;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
