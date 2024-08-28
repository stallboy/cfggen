package configgen.schema;

import static configgen.schema.FieldType.*;
import static configgen.schema.IncludedStructsChecker.*;

/**
 * 预先计算每个结构是否有对外引用
 * 查询
 */
public class HasRef {

    static void preCalculateAllHasRef(CfgSchema schema) {
        ForeachSchema.foreachNameable(HasRef::calcHasRef, schema);
    }

    private static void calcHasRef(Nameable nameable) {
        boolean hasRef = checkAnyOk(nameable, HasRef::checkIfDirectFieldsHasRef);
        nameable.meta().putHasRef(hasRef);
    }

    private static CheckResult checkIfDirectFieldsHasRef(Nameable nameable) {
        Metadata.MetaValue hasRefValue = nameable.meta().getHasRef();
        if (hasRefValue instanceof Metadata.MetaInt mi) {
            return mi.value() == 1 ? CheckResult.Ok : CheckResult.Fail;
        }

        if (nameable instanceof Structural structural && !structural.foreignKeys().isEmpty()) {
            return CheckResult.Ok;
        }
        return CheckResult.Unknown;
    }


    public static boolean hasRef(FieldType type) {
        switch (type) {
            case Primitive ignored -> {
                return false;
            }
            case StructRef structRef -> {
                return hasRef(structRef.obj());
            }
            case FList fList -> {
                return hasRef(fList.item());
            }

            case FMap fMap -> {
                return hasRef(fMap.key()) || hasRef(fMap.value());
            }
        }
    }

    public static boolean hasRef(Nameable nameable) {
        Metadata.MetaValue v = nameable.meta().getHasRef();
        if (v instanceof Metadata.MetaInt mi) {
            return mi.value() == 1;
        }
        throw new IllegalStateException(nameable.fullName() + " has no _hasRef meta value, schema not resolved!");
    }

}
