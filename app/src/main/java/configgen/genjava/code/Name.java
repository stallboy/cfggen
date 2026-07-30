package configgen.genjava.code;

import configgen.genjava.GenJavaUtil;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import static configgen.util.StringUtil.upper1;
import static configgen.schema.FieldType.*;

public class Name {

    static String codeTopPkg;

    /**
     * 美化命名开关。开启后，由 snake_case schema 名派生的标识符统一美化：
     * 类名/getter/all 函数名等转 PascalCase（factory_animation_type -> FactoryAnimationType），
     * enum/entry 常量转 SCREAMING_SNAKE_CASE。默认 false 保持老行为（upper1 / toUpperCase）。
     * 由 JavaCodeGenerator.generate() 在并发渲染前一次性赋值。
     */
    static boolean beautifulName = false;

    /**
     * 生成 enum/entry 常量的 Java 字段名。声明处（GenEntryOrEnumClass）和引用处
     * （GenStructuralClass 里 interface impl 的 type()）必须用同一个方法，保证一致。
     */
    public static String enumFieldName(String enumName) {
        return beautifulName ? StringUtil.toScreamingSnakeCase(enumName) : enumName.toUpperCase();
    }

    /**
     * 把单个名字段（schema 名按 '.' 拆出的一段，可能含 postfix）转成 PascalCase 标识符的一部分。
     * beautifulName 开启时合并下划线并首字母大写（foo_bar -> FooBar），否则仅 upper1 保持老行为。
     * className / sealed permits / getter / all 函数名等均走此方法，保证 snake_case 表名派生出的
     * 各类标识符风格一致。
     */
    public static String pascalName(String part) {
        return beautifulName ? StringUtil.underscoreToPascalCase(part) : StringUtil.upper1(part);
    }

    public static String GetByKeyFunctionNameInConfigMgr(KeySchema keySchema, boolean isPrimaryKey, Nameable nameable) {
        String name = "get" + Arrays.stream(nameable.name().split("\\.")).map(Name::pascalName).collect(Collectors.joining());

        if (isPrimaryKey){
            return name;
        }
        return name + "By" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
    }

    public static String GetByKeyFunctionName(KeySchema keySchema, boolean isPrimaryKey) {
        if (isPrimaryKey){
            return "get";
        }
        return "getBy" + keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining());
    }

    public static String uniqueKeyMapName(KeySchema keySchema) {
        return keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Map";
    }

    public static String keyClassName(KeySchema keySchema){
        return keyClassName(keySchema, null);
    }

    public static String keyClassName(KeySchema keySchema, NameableName nullableName) {
        if (keySchema.fields().size() > 1) {
            String klsName = keySchema.fields().stream().map(StringUtil::upper1).collect(Collectors.joining()) + "Key";
            if (nullableName != null) {
                return nullableName.fullName + "." + klsName;
            } else {
                return klsName;
            }

        } else {
            try {
                return TypeStr.boxType(keySchema.fieldSchemas().getFirst().type());
            } catch (Exception e) {
                return null;
            }
        }
    }


    public static String fullName(Nameable nameable) {
        return new NameableName(nameable).fullName;
    }

    public static String tableDataFullName(TableSchema table) {
        // 与 JavaCodeGenerator.generateTableClass 里 dataName 的构造保持一致：postfix 走 NameableName，
        // 这样 beautifulName 时 "_Detail" 会被一并 pascal 化（ai_action_Detail -> AiActionDetail），
        // 而不是在这里拼出与实际类名不一致的 "AiAction_Detail"。
        String postfix = (table.entry() instanceof EntryType.EEnum
                && !GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(table)) ? "_Detail" : "";
        return new NameableName(table, postfix).fullName;
    }


    public static String refType(TableSchema table) {
        return new NameableName(table).fullName;
    }

    public static String refType(ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "java.util.List<" + refType(fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {

                    case SimpleType ignored2 -> {
                        return refType(fk.refTableSchema());
                    }
                    case FList ignored2 -> {
                        return "java.util.List<" + refType(fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "java.util.Map<"
                                + (TypeStr.boxType((fMap.key()))) + ", "
                                + refType(fk.refTableSchema()) + ">";
                    }
                }
            }
        }
    }

    public static String refName(ForeignKeySchema fk) {
        String prefix = switch (fk.refKey()) {
            case RefKey.RefList ignored -> "ListRef";
            case RefKey.RefSimple refSimple -> refSimple.nullable() ? "NullableRef" : "Ref";
        };
        return prefix + upper1(fk.name());
    }

}
