package com.lucky.mescore.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    DRAFT("DRAFT", "草稿"),
    APPROVING("APPROVING", "审批中"),
    RELEASED("RELEASED", "已下达"),
    IN_PRODUCTION("IN_PRODUCTION", "生产中"),
    PENDING_STORAGE("PENDING_STORAGE", "待入库"),
    COMPLETED("COMPLETED", "已完成"),
    CLOSED("CLOSED", "已关闭");

    @EnumValue
    @JsonValue
    private final String code;

    private final String desc;

    public static OrderStatusEnum fromCode(String code) {
        for (OrderStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
