package com.lucky.mescore.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessRouteStatusEnum {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    EXPIRED("EXPIRED", "已失效");

    private final String code;
    private final String desc;
}
