package configgen.schema;

import static configgen.schema.FieldType.*;
import static configgen.schema.IncludedStructs.CheckResult;
import static configgen.schema.IncludedStructs.checkAnyOk;

public class HasText {

    static void preCalculateAllHasText(CfgSchema schema) {
        ForeachSchema.foreachNameable((HasText::calcHasText), schema);
    }

    private static void calcHasText(Nameable nameable) {
        boolean hasText = checkAnyOk(nameable, HasText::checkIfDirectFieldsHasText);
        nameable.meta().putHasText(hasText);
    }

    private static CheckResult checkIfDirectFieldsHasText(Nameable nameable) {
        Metadata.MetaValue hasText = nameable.meta().getHasText();
        if (hasText instanceof Metadata.MetaInt(int value)) {
            return value == 1 ? CheckResult.Ok : CheckResult.Fail;
        }

        if (nameable instanceof Structural structural) {
            for (FieldSchema f : structural.fields()) {
                if (f.type() == Primitive.TEXT) {
                    return CheckResult.Ok;
                }
            }
        }
        return CheckResult.Unknown;
    }


    public static boolean hasText(FieldType type) {
        switch (type) {
            case Primitive primitive -> {
                return primitive == Primitive.TEXT;
            }
            case StructRef structRef -> {
                return hasText(structRef.obj());
            }
            case FList fList -> {
                return hasText(fList.item());
            }

            case FMap fMap -> {
                return hasText(fMap.key()) || hasText(fMap.value());
            }
        }
    }

    public static boolean hasText(Nameable nameable) {
        Metadata.MetaValue v = nameable.meta().getHasText();
        if (v instanceof Metadata.MetaInt(int value)) {
            return value == 1;
        }
        throw new IllegalStateException(nameable.fullName() + " has no _hasText meta value, schema not resolved!");
    }

}
