package configgen.genjava.code;

import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.util.StringUtil.lower1;
import static configgen.schema.RefKey.*;

public class MethodStr {

    public static String formalParams(GenCfg cfg, List<FieldSchema> fs) {
        return fs.stream().map(f -> TypeStr.type(cfg, f.type()) + " " + lower1(f.name())).collect(Collectors.joining(", "));
    }

    public static String actualParams(List<String> keys) {
        return keys.stream().map(StringUtil::lower1).collect(Collectors.joining(", "));
    }

    public static String actualParamsKey(GenCfg cfg, KeySchema keySchema, String pre, NameableName nullableName) {
        String p = actualParamsKeyRaw(keySchema, pre);
        return keySchema.fields().size() > 1 ? "new " + Name.keyClassName(cfg, keySchema, nullableName) + "(" + p + ")" : p;
    }

    public static String actualParamsKeyRaw(KeySchema keySchema, String pre) {
        return keySchema.fields().stream().map(e -> pre + lower1(e)).collect(Collectors.joining(", "));
    }

    public static String referenceKey(KeySchema keySchema) {
        String fields = actualParamsKeyRaw(keySchema, "this.");
        return keySchema.fields().size() == 1 ? fields : "java.util.List.of(" + fields + ")";
    }

    public static String keyDisplayExpr(KeySchema keySchema) {
        return keySchema.fields().stream().map(StringUtil::lower1).collect(Collectors.joining(" + \",\" + "));
    }

    public static String hashCodes(List<FieldSchema> fs) {
        String paramList = fs.stream().map(f -> lower1(f.name())).collect(Collectors.joining(", "));
        return String.format("java.util.Objects.hash(%s)", paramList) ;
    }

    public static String equals(List<FieldSchema> fs) {
        return fs.stream().map(f -> equal(lower1(f.name()), "o." + lower1(f.name()), f.type())).collect(Collectors.joining(" && "));
    }

    public static String equal(String a, String b, FieldType t) {
        return TypeStr.isJavaPrimitive(t) ? a + " == " + b : a + ".equals(" + b + ")";
    }

    public static String tableGet(GenCfg cfg, TableSchema refTable, RefSimple refSimple, String actualParam) {
        if (refTable.entry() instanceof EntryType.EEnum) {
            return new NameableName(cfg, refTable).fullName + ".get(" + actualParam + ")";
        }
        KeySchema key = refSimple instanceof RefUniq unique ? unique.key() : refTable.primaryKey();
        return "mgr." + Name.GetByKeyFunctionNameInConfigMgr(cfg, key, !(refSimple instanceof RefUniq), refTable)
                + "(" + actualParam + ")";
    }
}
