package configgen.genjava.code;

import configgen.schema.FieldType;

import static configgen.schema.FieldType.Primitive.*;


public class TypeStr {

    public static String type(GenCfg cfg, FieldType t) {
        return _type(cfg, t, false);
    }

    public static String boxType(GenCfg cfg, FieldType t) {
        return _type(cfg, t, true);
    }

    private static String _type(GenCfg cfg, FieldType t, boolean box) {
        return switch (t) {
            case BOOL -> box ? "Boolean" : "boolean";
            case INT -> box ? "Integer" : "int";
            case LONG -> box ? "Long" : "long";
            case FLOAT -> box ? "Float" : "float";
            case STRING -> "String";
            case TEXT -> cfg.isLangSwitch() ? cfg.codeTopPkg() + ".Text" : "String";
            case StructRef structRef -> Name.fullName(cfg, structRef.obj());
            case FList fList -> "java.util.List<" + _type(cfg, fList.item(), true) + ">";
            case FMap fMap -> "java.util.Map<" + _type(cfg, fMap.key(), true) + ", " + _type(cfg, fMap.value(), true) + ">";
        };
    }

    public static String readValue(GenCfg cfg, FieldType t) {
        return switch (t) {
            case BOOL -> "input.readBool()";
            case INT -> "input.readInt()";
            case LONG -> "input.readLong()";
            case FLOAT -> "input.readFloat()";
            case STRING -> "input.readStringInPool()";
            case TEXT -> cfg.isLangSwitch() ? cfg.codeTopPkg() + ".Text._create(input)" : "input.readTextInPool()";
            case StructRef structRef -> Name.fullName(cfg, structRef.obj()) + "._create(input)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }

    public static String defaultValue(FieldType t) {
        return switch (t) {
            case BOOL -> "false";
            case INT, LONG, FLOAT -> "0";
            case STRING, TEXT -> "\"\"";
            case FList ignored -> "new java.util.ArrayList<>()";
            case FMap ignored -> "new java.util.LinkedHashMap<>()";
            case StructRef ignored -> "null";
        };
    }


    public static boolean isJavaPrimitive(FieldType t) {
        return switch (t) {
            case BOOL, INT, LONG, FLOAT -> true;
            default -> false;
        };
    }
}
