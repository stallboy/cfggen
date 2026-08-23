package configgen.genjava.code;

import configgen.genjava.GenJavaUtil;
import configgen.naming.GenNaming;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

import static configgen.schema.FieldType.*;

public class Name {

    /**
     * 生成 enum/entry 常量的 Java 字段名。声明处（GenEntryOrEnumClass）和引用处
     * （GenStructuralClass 里 interface impl 的 type()）必须用同一个方法，保证一致。
     */
    public static String enumFieldName(GenCfg cfg, String enumName) {
        return cfg.beautifulName() ? StringUtil.toScreamingSnakeCase(enumName) : enumName.toUpperCase();
    }

    /**
     * 把单个名字段（schema 名按 '.' 拆出的一段，可能含 postfix）转成 PascalCase 标识符的一部分。
     * beautifulName 开启时合并下划线并首字母大写（foo_bar -> FooBar），否则仅 upper1 保持老行为。
     * className / sealed permits / getter / all 函数名等均走此方法，保证 snake_case 表名派生出的
     * 各类标识符风格一致。
     */
    public static String pascalName(GenCfg cfg, String part) {
        return cfg.beautifulName() ? StringUtil.underscoreToPascalCase(part) : StringUtil.upper1(part);
    }

    public static String GetByKeyFunctionNameInConfigMgr(GenCfg cfg, KeySchema keySchema, boolean isPrimaryKey, Nameable nameable) {
        String name = "get" + Arrays.stream(nameable.name().split("\\.")).map(s -> pascalName(cfg, s)).collect(Collectors.joining());

        if (isPrimaryKey){
            return name;
        }
        return name + "By" + GenNaming.keyFieldsPascalName(keySchema.fields());
    }

    public static String GetByKeyFunctionName(KeySchema keySchema, boolean isPrimaryKey) {
        return StringUtil.lower1(GenNaming.uniqueKeyGetByName(keySchema, isPrimaryKey));
    }

    public static String uniqueKeyMapName(KeySchema keySchema) {
        return GenNaming.uniqueKeyMapName(keySchema);
    }

    public static String keyClassName(GenCfg cfg, KeySchema keySchema){
        return keyClassName(cfg, keySchema, null);
    }

    public static String keyClassName(GenCfg cfg, KeySchema keySchema, NameableName nullableName) {
        if (keySchema.fields().size() > 1) {
            String klsName = GenNaming.compositeKeyClassName(keySchema);
            if (nullableName != null) {
                return nullableName.fullName + "." + klsName;
            } else {
                return klsName;
            }

        } else {
            try {
                return TypeStr.boxType(cfg, keySchema.fieldSchemas().getFirst().type());
            } catch (Exception e) {
                return null;
            }
        }
    }


    public static String fullName(GenCfg cfg, Nameable nameable) {
        return new NameableName(cfg, nameable).fullName;
    }

    public static String tableDataFullName(GenCfg cfg, TableSchema table) {
        // 与 JavaCodeGenerator.generateTableClass 里 dataName 的构造保持一致：postfix 走 NameableName，
        // 这样 beautifulName 时 "_Detail" 会被一并 pascal 化（ai_action_Detail -> AiActionDetail），
        // 而不是在这里拼出与实际类名不一致的 "AiAction_Detail"。
        String postfix = (table.entry() instanceof EntryType.EEnum
                && !GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(table)) ? "_Detail" : "";
        return new NameableName(cfg, table, postfix).fullName;
    }


    public static String refType(GenCfg cfg, TableSchema table) {
        return new NameableName(cfg, table).fullName;
    }

    public static String refType(GenCfg cfg, ForeignKeySchema fk) {
        switch (fk.refKey()) {
            case RefKey.RefList ignored -> {
                return "java.util.List<" + refType(cfg, fk.refTableSchema()) + ">";
            }
            case RefKey.RefSimple ignored -> {
                FieldSchema firstLocal = fk.key().fieldSchemas().getFirst();
                switch (firstLocal.type()) {

                    case SimpleType ignored2 -> {
                        return refType(cfg, fk.refTableSchema());
                    }
                    case FList ignored2 -> {
                        return "java.util.List<" + refType(cfg, fk.refTableSchema()) + ">";
                    }
                    case FMap fMap -> {
                        return "java.util.Map<"
                                + (TypeStr.boxType(cfg, (fMap.key()))) + ", "
                                + refType(cfg, fk.refTableSchema()) + ">";
                    }
                }
            }
        }
    }

    public static String refName(ForeignKeySchema fk) {
        return GenNaming.refFieldName(fk);
    }

}
