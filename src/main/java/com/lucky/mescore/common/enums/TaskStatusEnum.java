package com.lucky.mescore.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    PENDING("PENDING", "待生产"),
    IN_PROGRESS("IN_PROGRESS", "生产中"),
    PAUSED("PAUSED", "已暂停"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;
}
