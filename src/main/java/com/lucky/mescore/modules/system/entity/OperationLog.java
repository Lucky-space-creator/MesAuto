package com.lucky.mescore.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("sys_operation_log")
public class OperationLog {

    private Long id;
    private String tenantId;
    private String username;
    private String module;
    private String action;
    private String target;
    private String requestUrl;
    private String requestMethod;
    private String requestParam;
    private Integer responseCode;
    private Long costTime;
    private String ip;
    private java.time.LocalDateTime createTime;
}
