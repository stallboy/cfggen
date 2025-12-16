package configgen.schema;

public class SchemaUtil {

    public static boolean isColumnMode(CfgSchema cfgSchema, String tableName) {
        boolean isColumnMode = false;
        if (cfgSchema != null) {
            cfgSchema.requireResolved();
            TableSchema schema = cfgSchema.findTable(tableName);
            if (schema != null) {
                isColumnMode = schema.isColumnMode();
            }
        }
        return isColumnMode;
    }
}
