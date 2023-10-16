package configgen.genlua;

import configgen.schema.*;

import java.util.ArrayList;
import java.util.List;

import static configgen.gen.Generator.lower1;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.value.CfgValue.VTable;

class TypeStr {


    // {{allName, getName=, keyIdx1=, keyIdx2=}, }
    static String getLuaUniqKeysString(Ctx ctx) {
        TableSchema table = ctx.vTable().schema();
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(getLuaOneUniqKeyString(ctx, table.primaryKey(), true));
        for (KeySchema uk : table.uniqueKeys()) {
            sb.append(getLuaOneUniqKeyString(ctx, uk, false));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String getLuaOneUniqKeyString(Ctx ctx, KeySchema keySchema, boolean isPrimaryKey) {
        String allname = isPrimaryKey ? Name.primaryKeyMapName : Name.uniqueKeyMapName(keySchema);
        String getname = isPrimaryKey ? Name.primaryKeyGetName : Name.uniqueKeyGetByName(keySchema);

        TableSchema table = ctx.vTable().schema();

        String keystr1 = getColumnStrOrIndex(keySchema.fieldSchemas().get(0), table);

        if (keySchema.fieldSchemas().size() > 1) {
            if (keySchema.fieldSchemas().size() != 2) {
                throw new RuntimeException("uniqkeys size != 2 " + table.name());
            }
            String keystr2 = getColumnStrOrIndex(keySchema.fieldSchemas().get(1), table);

            return String.format("{ '%s', '%s', %s, %s }, ", allname, getname, keystr1, keystr2);
        } else {
            return String.format("{ '%s', '%s', %s }, ", allname, getname, keystr1);
        }
    }


    private static String getColumnStrOrIndex(FieldSchema field, Structural structural) {
        int idx = findColumnIndex(field, structural);
        return String.valueOf(idx);
    }

    private static int findColumnIndex(FieldSchema field, Structural structural) {
        boolean doPack = isDoPackBool(structural);
        if (doPack) {
            boolean meetBool = false;
            int cnt = 0;
            for (FieldSchema column : structural.fields()) {
                if (column.type() == BOOL) {
                    if (column == field) {
                        throw new RuntimeException("现在不支持packbool的同时，bool引用到其他表");
                    }
                    if (!meetBool) {
                        meetBool = true;
                        cnt++;
                    }
                } else {
                    cnt++;
                    if (column == field) {
                        return cnt;
                    }
                }
            }
            throw new IllegalStateException("不该发生");
        } else {
            int cnt = 0;
            for (FieldSchema column : structural.fields()) {
                cnt++;
                if (column == field) {
                    return cnt;
                }
            }
        }
        throw new RuntimeException("未找到field");
    }

    static boolean isDoPackBool(Structural structural) {
        boolean doPack = AContext.getInstance().isPackBool();
        if (doPack) {
            int boolCnt = getBoolFieldCount(structural);
            if (boolCnt >= 50) {
                throw new RuntimeException("现在不支持pack多余50个bool字段的bean");
            }

            if (boolCnt < 2) {
                doPack = false;
            }
        }
        return doPack;
    }

    private static int getBoolFieldCount(Structural structural) {
        int c = 0;
        for (FieldSchema field : structural.fields()) {
            if (field.type() == BOOL) {
                c++;
            }
        }
        return c;
    }

    static String getLuaEnumString(Ctx ctx) {
        TableSchema table = ctx.vTable().schema();
        switch (table.entry()) {
            case EntryType.ENo.NO -> {
                return "nil";
            }
            case EntryType.EntryBase entryBase -> {
                return getColumnStrOrIndex(entryBase.fieldSchema(), table);
            }
        }
    }


    /**
     * {refName, 0, dstTable, dstGetName, thisColumnIdx, [thisColumnIdx2]}, -- 最常见类型
     * {refName, 1, dstTable, dstGetName, thisColumnIdx}, --本身是list
     * {refName, 2, dstTable, dstAllName, thisColumnIdx, dstColumnIdx}, --listRef到别的表
     * {refName, 3, dstTable, dstGetName, thisColumnIdx}, --本身是map
     */
    static String getLuaRefsString(Structural structural) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");

        for (ForeignKeySchema fk : structural.foreignKeys()) {
            String refName = Name.refName(fk);
            String dstTable = Name.fullName(fk.refTableSchema());

            switch (fk.refKey()) {
                case RefKey.RefList refList -> {
                    //{refName, 2, dstTable, dstAllName, thisColumnIdx, dstColumnIdx}, --listRef到别的表
                    String dstAllName = Name.primaryKeyMapName;
                    String thisColumnIdx = getColumnStrOrIndex(fk.key().fieldSchemas().get(0), structural);
                    String dstColumnIdx = getColumnStrOrIndex(refList.key().fieldSchemas().get(0), fk.refTableSchema());

                    sb.append(String.format("\n    { '%s', 2, %s, '%s', %s, %s }, ",
                            refName, dstTable, dstAllName, thisColumnIdx, dstColumnIdx));

                }
                case RefKey.RefSimple refSimple -> {
                    FieldSchema firstField = fk.key().fieldSchemas().get(0);
                    String dstGetName = Name.uniqueKeyGetByName(refSimple.keyNames());
                    String thisColumnIdx = getColumnStrOrIndex(firstField, structural);

                    switch (firstField.type()) {
                        case SimpleType _ -> {
                            if (fk.key().fieldSchemas().size() > 2) {
                                throw new RuntimeException("lua最多只支持两列做为索引！，" + structural.name());
                            }

                            // {refName, 0, dstTable, dstGetName, thisColumnIdx}  --最常见类型
                            if (fk.key().fieldSchemas().size() > 1) {
                                String thisColumnIdx2 = getColumnStrOrIndex(fk.key().fieldSchemas().get(1), structural);
                                sb.append(String.format("\n    { '%s', 0, %s, '%s', %s, %s }, ",
                                        refName, dstTable, dstGetName, thisColumnIdx, thisColumnIdx2));
                            } else {
                                sb.append(String.format("\n    { '%s', 0, %s, '%s', %s }, ",
                                        refName, dstTable, dstGetName, thisColumnIdx));
                            }

                        }
                        case FList _ -> {
                            // {refName, 1, dstTable, dstGetName, thisColumnIdx}, --本身是list
                            sb.append(String.format("\n    { '%s', 1, %s, '%s', %s }, ", refName, dstTable, dstGetName, thisColumnIdx));
                        }
                        case FMap _ -> {
                            // {refName, 3, dstTable, dstGetName, thisColumnIdx}, --本身是map
                            sb.append(String.format("\n    { '%s', 3, %s, '%s', %s }, ", refName, dstTable, dstGetName, thisColumnIdx));
                        }
                    }
                }
            }
        }
        sb.append("}");

        if (!structural.foreignKeys().isEmpty()) {
            return sb.toString();
        } else {
            return "nil";
        }
    }

    static String getLuaFieldsString(Structural structural) {
        StringBuilder sb = new StringBuilder();

        int cnt = structural.fields().size();
        int i = 0;


        boolean doPack = isDoPackBool(structural);
        boolean meetBool = false;

        for (FieldSchema field : structural.fields()) {
            if (doPack && field.type() == BOOL) { //从第一个遇到的bool开始搞
                if (!meetBool) {
                    meetBool = true;

                    sb.append("\n    {");
                    for (FieldSchema bf : structural.fields()) {
                        if (bf.type() == BOOL) {
                            i++;
                            String c = getCommaDescStr(bf.comment());
                            sb.append("\n    '").append(lower1(bf.name())).append("', -- ").append(typeToLuaType(field.type())).append(c);
                        }
                    }
                    if (i < cnt) {
                        sb.append("\n    },");
                    } else {
                        sb.append("\n    }");
                    }
                }

            } else { //正常的
                i++;
                String fieldName = String.format("'%s'", lower1(field.name()));
                String c = getCommaDescStr(field.comment());
                sb.append("\n    ").append(fieldName);

                if (i < cnt) {
                    sb.append(",");
                }
                sb.append(" -- ").append(typeToLuaType(field.type())).append(c);
            }

        }

        return sb.toString();
    }

    static String getLuaFieldsStringEmmyLua(Structural structural) {
        StringBuilder sb = new StringBuilder();
        boolean has = false;
        for (FieldSchema field : structural.fields()) {

            String c = getCommaDescStr(field.comment());
            sb.append("---@field ").append(lower1(field.name())).append(" ").append(typeToLuaType(field.type())).append(" ").append(c).append("\n");
            has = true;
        }
        if (has) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    static String getLuaUniqKeysStringEmmyLua(TableSchema table) {
        StringBuilder sb = new StringBuilder();
        String fullName = Name.fullName(table);
        sb.append(String.format("---@field %s fun(%s):%s\n",
                Name.primaryKeyGetName, getLuaGetParam(table.primaryKey()), fullName));
        for (KeySchema uk : table.uniqueKeys()) {
            sb.append(String.format("---@field %s fun(%s):%s\n",
                    Name.uniqueKeyGetByName(uk), getLuaGetParam(uk), fullName));
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private static String getLuaGetParam(KeySchema primaryOrUniqueKey) {
        StringBuilder sb = new StringBuilder();
        boolean has = false;
        for (FieldSchema field : primaryOrUniqueKey.fieldSchemas()) {
            sb.append(field.name()).append(":").append(typeToLuaType(field.type())).append(",");
            has = true;
        }
        if (has) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    static String getLuaEnumStringEmmyLua(VTable vTable) {
        StringBuilder sb = new StringBuilder();
        boolean has = false;
        if (vTable.enumNames() != null){
            for (String enumName : vTable.enumNames()) {
                sb.append("---@field ").append(enumName).append(" ").append(Name.fullName(vTable.schema())).append("\n");
                has = true;
            }
        }
        if (has) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    static String getLuaRefsStringEmmyLua(Structural structural) {
        StringBuilder sb = new StringBuilder();
        boolean hasRef = false;
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            String refName = Name.refName(fk);
            String dstTable = Name.fullName(fk.refTableSchema());

            boolean isList = (fk.refKey() instanceof RefKey.RefList);
            if (!isList) {
                isList = fk.key().fieldSchemas().get(0).type() instanceof FList;
            }
            if (isList) {
                sb.append(String.format("---@field %s table<number,%s>\n", refName, dstTable));
            } else {
                sb.append(String.format("---@field %s %s\n", refName, dstTable));
            }
            hasRef = true;
        }

        if (hasRef) {
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        } else {
            return "";
        }
    }

    private static String typeToLuaType(FieldType type) {
        return switch (type) {
            case BOOL -> "boolean";
            case INT, LONG, FLOAT -> "number";
            case STRING -> "string";
            case TEXT -> "text";
            case StructRef structRef -> Name.fullName(structRef.obj());
            case FList fList -> String.format("table<number,%s>", typeToLuaType(fList.item()));
            case FMap fMap -> String.format("table<%s,%s>", typeToLuaType(fMap.key()), typeToLuaType(fMap.value()));
        };
    }

    static String getLuaTextFieldsString(Structural structural) {
        List<String> texts = new ArrayList<>();
        for (FieldSchema field : structural.fields()) {
            switch (field.type()) {
                case TEXT -> {
                    texts.add(lower1(field.name()) + " = 1");
                }
                case FList fList -> {
                    if (fList.item() == TEXT) {
                        texts.add(lower1(field.name()) + " = 2");
                    }
                }
                default -> {
                }
            }
        }

        if (texts.isEmpty()) {
            return "";
        }

        return "\n    { " + String.join(", ", texts) + " },";
    }

    static String getCommaDescStr(String desc) {
        if (desc.isEmpty()) {
            return "";
        }
        return ", " + desc;
    }
}
