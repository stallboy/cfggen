package configgen.genjava;

import configgen.schema.FieldSchema;
import configgen.schema.FieldType;
import configgen.schema.HasRef;
import configgen.schema.IncludedStructs;
import configgen.schema.Nameable;
import configgen.schema.Structural;
import configgen.schema.TableSchema;

import java.util.List;

import static configgen.schema.EntryType.EEnum;

public class GenJavaUtil {

    /** Only embedded values participate; resolved foreign keys are not ownership edges. */
    public static boolean hasExtension(Nameable type) {
        return IncludedStructs.checkAnyOk(type, value -> value instanceof Structural && value.meta().get("javaBase") != null
                ? IncludedStructs.CheckResult.Ok : IncludedStructs.CheckResult.Unknown);
    }

    public static boolean hasExtension(FieldType type) {
        return switch (type) {
            case FieldType.Primitive ignored -> false;
            case FieldType.StructRef ref -> hasExtension(ref.obj());
            case FieldType.FList list -> hasExtension(list.item());
            case FieldType.FMap map -> hasExtension(map.key()) || hasExtension(map.value());
        };
    }

    public static boolean needsTableData(TableSchema table) {
        return !isEnumAndHasOnlyPrimaryKeyAndEnumStr(table) || table.meta().get("javaBase") != null;
    }

    public static boolean needsIndex(TableSchema table) {
        return !table.uniqueKeys().isEmpty() || table.fields().stream().anyMatch(FieldSchema::isSeq);
    }

    public static boolean needsLoadValueErrs(Structural structural) {
        return HasRef.hasRef(structural) || structural instanceof TableSchema table && needsIndex(table);
    }

    public static String loadValueErrsClass(String className) {
        return className.equals("LoadValueErrs") ? "configgen.genjava.LoadValueErrs" : "LoadValueErrs";
    }

    public static boolean isEnumAndHasOnlyPrimaryKeyAndEnumStr(TableSchema tableSchema) {
        if (tableSchema.entry() instanceof EEnum ignored) {
            int fz = tableSchema.fields().size();
            if (fz > 2) {
                return false;
            }

            if (isEnumAsPrimaryKey(tableSchema) && fz > 1) {
                return false;
            }

            return tableSchema.foreignKeys().isEmpty();
        }
        return false;
    }

    private static boolean isEnumAsPrimaryKey(TableSchema tableSchema) {
        if (tableSchema.entry() instanceof EEnum eEnum) {
            List<FieldSchema> pks = tableSchema.primaryKey().fieldSchemas();
            return pks.size() == 1 && pks.get(0) == eEnum.fieldSchema();
        }
        return false;
    }


}
