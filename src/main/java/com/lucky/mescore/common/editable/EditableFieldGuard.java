package com.lucky.mescore.common.editable;

import com.lucky.mescore.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 字段级编辑守卫。
 *
 * 采用「白名单 + 默认拒绝」策略：把客户端提交的对象与数据库现有对象逐字段比对，
 * 凡是发生变化且当前状态不在 {@link EditableWhen} 允许范围内的字段，直接拒绝请求。
 *
 * 这样可以避免以往「整条记录要么全可改、要么全不可改」的粗粒度控制，
 * 也能防止客户端越权篡改 orderStatus、completedQty 等由系统维护的字段。
 */
@Slf4j
@Component
public class EditableFieldGuard {

    /** 无论何种状态都不允许客户端直接修改的基础字段 */
    private static final Set<String> ALWAYS_IGNORED = Set.of(
            "id", "tenantId", "createBy", "createTime", "updateBy", "updateTime", "deleted");

    /**
     * 校验并把「允许修改的字段」从 incoming 合并到 persisted 上。
     *
     * @param incoming      客户端提交的对象（可能只填了部分字段）
     * @param persisted     数据库中的当前对象
     * @param currentStatus 当前业务状态码
     * @param <T>           实体类型
     * @return 合并后可直接用于 update 的对象（即 persisted 本身）
     */
    public <T> T applyEditable(T incoming, T persisted, String currentStatus) {
        if (incoming == null || persisted == null) {
            throw new BusinessException("待更新对象不存在");
        }
        List<String> rejected = new ArrayList<>();

        for (Field field : collectFields(persisted.getClass())) {
            if (ALWAYS_IGNORED.contains(field.getName())) {
                continue;
            }
            ReflectionUtils.makeAccessible(field);
            Object newVal = ReflectionUtils.getField(field, incoming);
            // 未提交的字段（null）视为不修改，避免 PUT 半量更新误清空
            if (newVal == null) {
                continue;
            }
            Object oldVal = ReflectionUtils.getField(field, persisted);
            if (isEqual(newVal, oldVal)) {
                continue;
            }
            if (!isEditable(field, currentStatus)) {
                rejected.add(describe(field));
                continue;
            }
            ReflectionUtils.setField(field, persisted, newVal);
        }

        if (!rejected.isEmpty()) {
            throw new BusinessException("当前状态[" + currentStatus + "]下不允许修改字段：" + String.join("、", rejected));
        }
        return persisted;
    }

    /** 返回指定状态下允许编辑的字段名列表，供前端渲染表单时决定哪些控件可用 */
    public List<String> editableFields(Class<?> clazz, String currentStatus) {
        List<String> result = new ArrayList<>();
        for (Field field : collectFields(clazz)) {
            if (ALWAYS_IGNORED.contains(field.getName())) {
                continue;
            }
            if (isEditable(field, currentStatus)) {
                result.add(field.getName());
            }
        }
        return result;
    }

    private boolean isEditable(Field field, String currentStatus) {
        EditableWhen ann = field.getAnnotation(EditableWhen.class);
        if (ann == null) {
            // 默认拒绝：没有显式声明的字段不允许通过更新接口修改
            return false;
        }
        for (String allowed : ann.value()) {
            if ("*".equals(allowed) || allowed.equals(currentStatus)) {
                return true;
            }
        }
        return false;
    }

    /** 收集包含父类在内的全部实例字段 */
    private List<Field> collectFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        Class<?> cur = clazz;
        while (cur != null && cur != Object.class) {
            for (Field f : cur.getDeclaredFields()) {
                if (f.isSynthetic() || java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (seen.add(f.getName())) {
                    fields.add(f);
                }
            }
            cur = cur.getSuperclass();
        }
        return fields;
    }

    private String describe(Field field) {
        return field.getName();
    }

    private boolean isEqual(Object a, Object b) {
        if (a instanceof java.math.BigDecimal && b instanceof java.math.BigDecimal) {
            return ((java.math.BigDecimal) a).compareTo((java.math.BigDecimal) b) == 0;
        }
        return Objects.equals(a, b);
    }

    /** 便于单元测试与调试：打印某类的字段可编辑矩阵 */
    public String dumpMatrix(Class<?> clazz, String... statuses) {
        StringBuilder sb = new StringBuilder();
        for (String s : statuses) {
            sb.append(s).append(" -> ").append(Arrays.toString(editableFields(clazz, s).toArray())).append('\n');
        }
        return sb.toString();
    }
}
