package configgen.naming;

import configgen.schema.ForeignKeySchema;
import configgen.schema.RefKey;

import static configgen.util.StringUtil.upper1;

/**
 * 跨语言生成代码的命名契约层：schema 对象在生成代码里"叫什么"，六门语言共用一份。
 * <p>
 * 职责边界：
 * <ul>
 * <li>这里只产出命名的"语义"，一律返回 PascalCase 规范形（如 GetByNameId）；
 *     大小写风格与连接装饰（lower1、下划线前缀、'.'/'/'/'_' 连接）由各语言生成器自行处理。</li>
 * <li>只收跨语言逐字一致（或仅差装饰）的命名约定。refType 这类需要目标语言类型系统参与的
 *     映射不属于此层；value.ValueRefCollector 的小写 ref（引用搜索索引名）与
 *     genbyai.SchemaToTs.RefName（.d.ts 路径）用途不同，也不在此层。</li>
 * </ul>
 * <p>
 * 新增语言生成器时命名必须委托本类，禁止把 switch 复制回生成器——历史上 gd 的 RefList
 * 前缀曾与其余语言漂移，手工对齐见提交 ccc2381c。
 */
public class GenNaming {

    /**
     * 外键引用在生成代码中的成员字段名（各语言里的属性/字段/成员变量）：
     * Ref / NullableRef / ListRef + 首字母大写的外键名。六门语言逐字一致，直接使用；
     * RefKey 是 sealed 类型，新增变体时编译器会强制在此处穷尽处理。
     */
    public static String refFieldName(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "ListRef" + upper1(fk.name());
            }
            case RefKey.RefSimple refSimple -> {
                if (refSimple.nullable()) {
                    return "NullableRef" + upper1(fk.name());
                } else {
                    return "Ref" + upper1(fk.name());
                }
            }
        }
    }

}
