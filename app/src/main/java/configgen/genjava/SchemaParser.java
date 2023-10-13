package configgen.genjava;

import configgen.gen.LangSwitch;
import configgen.schema.*;
import configgen.value.CfgValue;

import java.util.Map;

import static configgen.genjava.SchemaPrimitive.*;
import static configgen.schema.FieldType.*;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.FieldType.Primitive.BOOL;
import static configgen.schema.FieldType.Primitive.FLOAT;
import static configgen.schema.FieldType.Primitive.INT;
import static configgen.schema.FieldType.Primitive.LONG;
import static configgen.value.CfgValue.*;

public final class SchemaParser {

    public static SchemaInterface parse(CfgValue cfgValue, LangSwitch langSwitch) {
        SchemaInterface root = new SchemaInterface();
        if (langSwitch != null) {
            root.addImp("Text", parseLangSwitch(langSwitch)); //这里国际化的字段当作一个Bean
        }

        CfgSchema schema = cfgValue.schema();
        for (Fieldable fieldable : schema.sortedFieldables()) {
            Schema sc = switch (fieldable) {
                case InterfaceSchema interfaceSchema -> parseInterface(interfaceSchema);
                case StructSchema structSchema -> parseStructural(structSchema);
            };
            root.addImp(fieldable.name(), sc);
        }

        for (VTable vTable : cfgValue.sortedTables()) {
            TableSchema tableSchema = vTable.schema();
            String name = tableSchema.name();
            EntryType entry = tableSchema.entry();

            if (entry instanceof EntryType.EEnum) {
                root.addImp(name, parseEntry(vTable, false));
                if (!GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(tableSchema)) {
                    root.addImp(name + "_Detail", parseStructural(tableSchema));
                }
            } else {
                root.addImp(name, parseStructural(tableSchema));
                if (entry instanceof EntryType.EEntry) {
                    root.addImp(name + "_Entry", parseEntry(vTable, true));
                }
            }
        }
        return root;
    }

    private static Schema parseLangSwitch(LangSwitch ls) {
        SchemaBean sb = new SchemaBean(false);
        for (String lang : ls.languages()) {
            sb.addColumn(lang, SStr);
        }
        return sb;
    }

    private static Schema parseEntry(VTable vTable, boolean isEnumPart) {
        boolean hasIntValue = vTable.enumNameToIntegerValueMap() != null;
        SchemaEnum se = new SchemaEnum(isEnumPart, hasIntValue);

        if (hasIntValue) {
            for (Map.Entry<String, Integer> e : vTable.enumNameToIntegerValueMap().entrySet()) {
                se.addValue(e.getKey(), e.getValue());
            }
        } else {
            for (String enumName : vTable.enumNames()) {
                se.addValue(enumName);
            }
        }
        return se;
    }

    private static Schema parseInterface(InterfaceSchema interfaceSchema) {
        SchemaInterface si = new SchemaInterface();
        for (StructSchema impl : interfaceSchema.impls()) {
            si.addImp(impl.name(), parseStructural(impl));
        }
        return si;

    }

    private static Schema parseStructural(Structural structural) {
        boolean isTable = structural instanceof TableSchema;
        SchemaBean sb = new SchemaBean(isTable);
        for (FieldSchema fs : structural.fields()) {
            sb.addColumn(fs.name(), parseType(fs.type()));
        }
        return sb;
    }

    private static Schema parseType(FieldType t) {
        return switch (t) {
            case BOOL -> SBool;
            case INT -> SInt;
            case LONG -> SLong;
            case FLOAT -> SFloat;
            case STRING, TEXT -> SStr;
            case StructRef structRef -> new SchemaRef(structRef.nameNormalized());
            case FList fList -> new SchemaList(parseType(fList.item()));
            case FMap fMap -> new SchemaMap(parseType(fMap.key()), parseType(fMap.value()));
        };
    }

}
