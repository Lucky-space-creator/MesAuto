package com.lucky.mescore.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApprovalStatusEnum {

    RUNNING("RUNNING", "运行中"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    TERMINATED("TERMINATED", "已终止");

    private final String code;
    private final String desc;
}
