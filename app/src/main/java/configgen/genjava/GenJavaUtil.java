package configgen.genjava;

import configgen.schema.FieldSchema;
import configgen.schema.TableSchema;

import java.util.List;

import static configgen.schema.EntryType.EEnum;

public class GenJavaUtil {

    public static boolean isEnumAndHasOnlyPrimaryKeyAndEnumStr(TableSchema tableSchema) {
        if (tableSchema.entry() instanceof EEnum _) {
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

    public static boolean isEnumAsPrimaryKey(TableSchema tableSchema) {
        if (tableSchema.entry() instanceof EEnum eEnum) {
            List<FieldSchema> pks = tableSchema.primaryKey().fieldSchemas();
            return pks.size() == 1 && pks.get(0) == eEnum.fieldSchema();
        }
        return false;
    }


}
