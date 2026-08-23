package configgen.naming;

import configgen.schema.ForeignKeySchema;
import configgen.schema.KeySchema;
import configgen.schema.Nameable;
import configgen.schema.RefKey;
import configgen.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 跨语言生成代码的命名法则库：schema 对象在生成代码里"叫什么"，集中在此一处。
 * <p>
 * 职责边界：
 * <ul>
 * <li>每个方法返回一种风格下的成品名，默认 PascalCase 规范形（如 GetByNameId），
 *     snake_case 风格以 Snake 后缀标注（如 find_by_name_id，gd）；
 *     同一命名实体在 Pascal 与 Snake 两种风格下的差异只体现在这里，
 *     其余大小写与连接装饰（lower1、下划线前缀、'.'/'/'/'_' 连接）由各语言生成器自行处理。</li>
 * <li>只收由 schema 对象派生的标识符命名法则。不收依赖生成器配置的命名
 *     （genjava 的 beautifulName 双模式、refTitle 定制），
 *     也不收 refType 这类需要目标语言类型系统参与的映射；
 *     value.ValueRefCollector 的小写 ref（引用搜索索引名）与
 *     genbyai.SchemaToTs.RefName（.d.ts 路径）用途不同，同样不在此层。</li>
 * </ul>
 * <p>
 * 新增语言生成器时命名必须委托本类，禁止把命名 switch/拼接复制回生成器——历史上 gd 的 RefList
 * 前缀曾因各写一份而漂移，手工对齐见提交 ccc2381c。
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
                return "ListRef" + StringUtil.upper1(fk.name());
            }
            case RefKey.RefSimple refSimple -> {
                if (refSimple.nullable()) {
                    return "NullableRef" + StringUtil.upper1(fk.name());
                } else {
                    return "Ref" + StringUtil.upper1(fk.name());
                }
            }
        }
    }

    // ---------- 类名 / 包路径的段落拆分 ----------

    /**
     * 生成代码类名/包路径的名字段：schema 名按 '.' 拆分为段；
     * interface 内嵌 struct 时由 Nameable.fullName() 提供 interface 前缀段（如 [iface, pkg, struct]）。
     * 各语言再对段施自己的大小写规则与连接符（Pascal/小写/下划线/'.'/'/'）。
     */
    public static List<String> classNameSegments(Nameable nameable) {
        return List.of(nameable.fullName().split("\\."));
    }

    // ---------- 唯一键命名族：Pascal 规范形（java/cs/ts/lua/go） ----------

    /**
     * 键字段 Pascal 拼接词干（nameId → NameId）。仅供拼装嵌入表名等的复合名
     * （如 java ConfigMgr 的 getTableByNameId、go 的前缀风格 KeyNameId）；
     * 有成品方法时必须用成品，不要在生成器里再拼 Map/Key 等后缀。
     */
    public static String keyFieldsPascalName(List<String> keyFields) {
        return keyFields.stream().map(StringUtil::upper1).collect(Collectors.joining());
    }

    public static String keyFieldsPascalName(KeySchema key) {
        return keyFieldsPascalName(key.fields());
    }

    /**
     * 唯一键容器字段名，Pascal 规范形（NameIdMap）。
     * java/lua 原样使用；ts 再 lower1；cs 再 _lower1。
     */
    public static String uniqueKeyMapName(KeySchema key) {
        return keyFieldsPascalName(key) + "Map";
    }

    /**
     * 唯一键查找函数名，Pascal 规范形：空表（主键）→ Get，否则 GetByNameId。
     * cs/ts 原样使用；java/lua 再 lower1。
     */
    public static String uniqueKeyGetByName(List<String> keyFields) {
        if (keyFields.isEmpty()) {
            return "Get";
        }
        return "GetBy" + keyFieldsPascalName(keyFields);
    }

    public static String uniqueKeyGetByName(KeySchema key) {
        return uniqueKeyGetByName(key.fields());
    }

    public static String uniqueKeyGetByName(KeySchema key, boolean isPrimaryKey) {
        return isPrimaryKey ? "Get" : uniqueKeyGetByName(key.fields());
    }

    /**
     * 复合唯一键的键类名，Pascal 规范形（NameIdKey）。仅复合键有效——
     * 单字段键的类型由各语言 type() 决定，不在此层；go 是前缀风格 KeyNameId，用词干自行拼。
     */
    public static String compositeKeyClassName(KeySchema key) {
        return keyFieldsPascalName(key) + "Key";
    }

    // ---------- 唯一键命名族：snake_case 规范形（gd 及未来 snake_case 语言） ----------

    /**
     * 唯一键查找函数名，snake 规范形（find_by_name_id），gd 风格。
     */
    public static String uniqueKeyGetByNameSnake(List<String> keyFields) {
        return "find_by_" + keyFields.stream().map(StringUtil::lower1).collect(Collectors.joining("_"));
    }

    public static String uniqueKeyGetByNameSnake(KeySchema key) {
        return uniqueKeyGetByNameSnake(key.fields());
    }

    /**
     * 唯一键容器字段名，snake 规范形（_name_id_map），gd 风格。
     */
    public static String uniqueKeyMapNameSnake(KeySchema key) {
        return "_" + key.fields().stream().map(StringUtil::lower1).collect(Collectors.joining("_")) + "_map";
    }

}
