package configgen.genjava.code;

import org.jspecify.annotations.Nullable;

import configgen.schema.FieldType;
import configgen.schema.InterfaceSchema;
import configgen.schema.StructSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

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
            case StructRef structRef -> Name.fullName(cfg, structRef.resolvedObj());
            case FList fList -> "java.util.List<" + _type(cfg, fList.item(), true) + ">";
            case FMap fMap -> "java.util.Map<" + _type(cfg, fMap.key(), true) + ", " + _type(cfg, fMap.value(), true) + ">";
        };
    }

    public static @Nullable String readValue(GenCfg cfg, FieldType t) {
        return switch (t) {
            case BOOL -> "input.readBool()";
            case INT -> "input.readInt()";
            case LONG -> "input.readLong()";
            case FLOAT -> "input.readFloat()";
            case STRING -> "input.readStringInPool()";
            case TEXT -> cfg.isLangSwitch() ? cfg.codeTopPkg() + ".Text._create(input)" : "input.readTextInPool()";
            case StructRef structRef -> Name.fullName(cfg, structRef.resolvedObj()) + "._create(input)";
            case FList ignored -> null;
            case FMap ignored -> null;
        };
    }

    public static @Nullable String builderDefault(GenCfg cfg, FieldType type) {
        return builderDefault(cfg, type, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static @Nullable String builderDefault(GenCfg cfg, FieldType t, Set<StructSchema> active) {
        return switch (t) {
            case BOOL -> "false";
            case INT, LONG, FLOAT -> "0";
            case STRING -> "\"\"";
            case TEXT -> cfg.isLangSwitch()
                    ? "new " + cfg.codeTopPkg() + ".Text(" + String.join(", ", Collections.nCopies(cfg.languageCount(), "\"\"")) + ")"
                    : "\"\"";
            case FList ignored -> "new java.util.ArrayList<>()";
            case FMap ignored -> "new java.util.LinkedHashMap<>()";
            case StructRef ref -> switch (ref.resolvedObj()) {
                case StructSchema struct -> structDefault(cfg, struct, active);
                case InterfaceSchema schema -> schema.defaultImpl().isEmpty()
                        ? null : structDefault(cfg, schema.defaultImplStruct(), active);
            };
        };
    }

    private static @Nullable String structDefault(GenCfg cfg, StructSchema struct, Set<StructSchema> active) {
        if (!active.add(struct)) {
            return null;
        }
        try {
            var arguments = new ArrayList<String>(struct.fields().size());
            for (var field : struct.fields()) {
                String argument = builderDefault(cfg, field.type(), active);
                if (argument == null) {
                    return null;
                }
                arguments.add(argument);
            }
            return "new " + Name.fullName(cfg, struct) + "(" + String.join(", ", arguments) + ")";
        } finally {
            active.remove(struct);
        }
    }

    public static boolean isJavaPrimitive(FieldType t) {
        return switch (t) {
            case BOOL, INT, LONG, FLOAT -> true;
            default -> false;
        };
    }
}
