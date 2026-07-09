package configgen.util.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 替代 fastjson2 的 @JSONField。
 * - 标注在 record 的普通方法上：把该方法返回值作为额外字段输出（如 SStruct.type() -> "type":"struct"）。
 * - 标注在 record 组件上：用 name 重命名输出的 key（如 BriefRecord.refs -> "$refs"）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface JsonField {
    String name() default "";
}
