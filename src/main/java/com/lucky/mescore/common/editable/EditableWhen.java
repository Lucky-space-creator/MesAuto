package com.lucky.mescore.common.editable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级可编辑控制注解。
 *
 * 标注在实体字段上，声明该字段允许被修改的业务状态集合。
 * 未标注该注解的字段一律视为「不可通过常规更新接口修改」，
 * 即默认拒绝，必须显式开放，避免全字段裸奔。
 *
 * 示例：{@code @EditableWhen({"DRAFT"})} 表示仅草稿态可改。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EditableWhen {

    /** 允许编辑该字段的状态码集合；值为 "*" 表示任意状态均可编辑 */
    String[] value();
}
