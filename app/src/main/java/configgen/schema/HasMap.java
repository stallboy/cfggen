package configgen.schema;

import static configgen.schema.IncludedStructsChecker.*;

public class HasMap {

    static void preCalculateAllHasMap(CfgSchema schema, SchemaErrs errs) {
        ForeachSchema.foreachNameable((nameable -> calcHasMap(nameable, errs)), schema);
    }

    private static void calcHasMap(Nameable nameable, SchemaErrs errs) {
        boolean hasMap = checkAnyOk(nameable, HasMap::checkIfDirectFieldsHasMap);
        nameable.meta().putHasMap(hasMap);
        if (hasMap && nameable instanceof TableSchema tableSchema && nameable.meta().isJson()) {
            errs.addErr(new SchemaErrs.JsonTableNotSupportMap(tableSchema.name()));
        }
    }

    private static CheckResult checkIfDirectFieldsHasMap(Nameable nameable) {
        Metadata.MetaValue hasMapValue = nameable.meta().getHasMap();
        if (hasMapValue instanceof Metadata.MetaInt mi) {
            return mi.value() == 1 ? CheckResult.Ok : CheckResult.Fail;
        }

        if (nameable instanceof Structural structural) {
            for (FieldSchema f : structural.fields()) {
                if (f.type() instanceof FieldType.FMap) {
                    return CheckResult.Ok;
                }
            }
        }
        return CheckResult.Unknown;
    }

    public static boolean hasMap(Nameable nameable) {
        Metadata.MetaValue v = nameable.meta().getHasMap();
        if (v instanceof Metadata.MetaInt mi) {
            return mi.value() == 1;
        }
        throw new IllegalStateException(nameable.fullName() + " has no _hasMap meta value, schema not resolved!");
    }

}