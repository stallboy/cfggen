package configgen.schema;

import static configgen.schema.IncludedStructs.*;

/**
 * 预先计算每个结构是否有block
 * 查询
 */
public class HasBlock {

    static void preCalculateAllHasBlock(CfgSchema schema, CfgSchemaErrs errs) {
        ForeachSchema.foreachNameable((nameable -> calcHasBlock(nameable, errs)), schema);
    }

    private static void calcHasBlock(Nameable nameable, CfgSchemaErrs errs) {
        boolean hasBlock = checkAnyOk(nameable, HasBlock::checkIfDirectFieldsHasBlock);
        nameable.meta().putHasBlock(hasBlock);
        if (hasBlock && nameable instanceof TableSchema table) {
            String firstField = table.fields().getFirst().name();
            if (!table.primaryKey().fields().contains(firstField)) {
                errs.addErr(new CfgSchemaErrs.BlockTableFirstFieldNotInPrimaryKey(table.name()));
            }
        }
    }

    private static CheckResult checkIfDirectFieldsHasBlock(Nameable nameable) {
        Metadata meta = nameable.meta();
        Metadata.MetaValue hasBlockValue = meta.getHasBlock();
        if (hasBlockValue instanceof Metadata.MetaInt(int value)) {
            return value == 1 ? CheckResult.Ok : CheckResult.Fail;
        }

        if (nameable instanceof Structural structural) {
            for (FieldSchema f : structural.fields()) {
                if (f.fmt() instanceof FieldFormat.Block) {
                    return CheckResult.Ok;
                }
            }
        }
        return CheckResult.Unknown;
    }

    public static boolean hasBlock(Nameable nameable) {
        Metadata.MetaValue v = nameable.meta().getHasBlock();
        if (v instanceof Metadata.MetaInt(int value)) {
            return value == 1;
        }
        throw new IllegalStateException(nameable.fullName() + " has no _hasBlock meta value, schema not resolved!");
    }
}
