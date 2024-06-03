package configgen.schema;

import java.util.*;

import static configgen.schema.FieldType.*;

/**
 * 预先计算每个结构是否有对外引用 或有block
 * 查询
 */
public class HasRefOrBlock {

    static void preCalculateAllHasRefAndHasBlocks(CfgSchema schema, SchemaErrs errs) {
        ForeachSchema.foreachNameable(HasRefOrBlock::calcHasRef, schema);
        ForeachSchema.foreachNameable((nameable -> calcHasBlock(nameable, errs)), schema);
    }

    private static boolean calcHasRef(Nameable nameable) {
        Metadata meta = nameable.meta();
        Metadata.MetaValue hasRefValue = meta.getHasRef();
        if (hasRefValue != null) {
            return hasRefValue instanceof Metadata.MetaInt mi && mi.value() == 1;
        }
        boolean hasRef = checkIncludedStructs(nameable, HasRefOrBlock::checkIfDirectFieldsHasRef);
        meta.putHasRef(hasRef);
        return hasRef;
    }

    private static boolean checkIfDirectFieldsHasRef(Nameable nameable) {
        switch (nameable) {
            case Structural structural -> {
                if (!structural.foreignKeys().isEmpty()) {
                    return true;
                }
            }
            default -> {
            }
        }
        return false;
    }

    private static void calcHasBlock(Nameable nameable, SchemaErrs errs) {
        Metadata meta = nameable.meta();
        Metadata.MetaValue hasBlockValue = meta.getHasBlock();
        if (hasBlockValue != null) {
            return;
        }
        boolean hasBlock = checkIncludedStructs(nameable, HasRefOrBlock::checkIfDirectFieldsHasBlock);
        meta.putHasBlock(hasBlock);
        if (hasBlock && nameable instanceof TableSchema table) {
            String firstField = table.fields().getFirst().name();
            if (!table.primaryKey().fields().contains(firstField)) {
                errs.addErr(new SchemaErrs.BlockTableFirstFieldNotInPrimaryKey(table.name()));
            }
        }
    }

    private static boolean checkIfDirectFieldsHasBlock(Nameable nameable) {
        switch (nameable) {
            case Structural structural -> {
                for (FieldSchema f : structural.fields()) {
                    if (f.fmt() instanceof FieldFormat.Block) {
                        return true;
                    }
                }
            }
            default -> {
            }
        }
        return false;
    }

    private static interface Checker {
        boolean check(Nameable nameable);
    }

    private static boolean checkIncludedStructs(Nameable nameable, Checker checker) {
        Set<String> checked = new HashSet<>();

        Map<String, Nameable> frontiers = new HashMap<>();
        frontiers.put(nameable.fullName(), nameable);


        while (!frontiers.isEmpty()) {
            for (Nameable frontier : frontiers.values()) {
                if (checker.check(frontier)) {
                    return true;
                }
            }
            checked.addAll(frontiers.keySet());

            Map<String, Nameable> newFrontiers = new HashMap<>();
            for (Nameable frontier : frontiers.values()) {
                // expand
                switch (frontier) {
                    case InterfaceSchema sInterface -> {
                        for (StructSchema impl : sInterface.impls()) {
                            String fn = impl.fullName();
                            if (!checked.contains(fn)) {
                                newFrontiers.put(fn, impl);
                            }
                        }
                    }
                    case Structural structural -> {
                        for (FieldSchema field : structural.fields()) {
                            switch (field.type()) {

                                case StructRef structRef -> {
                                    Fieldable obj = structRef.obj();
                                    String fn = obj.fullName();
                                    if (!checked.contains(fn)) {
                                        newFrontiers.put(fn, obj);
                                    }
                                }
                                case FList fList -> {
                                    SimpleType item = fList.item();
                                    if (item instanceof StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.contains(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                }

                                case FMap fMap -> {
                                    SimpleType key = fMap.key();
                                    if (key instanceof StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.contains(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                    SimpleType value = fMap.value();
                                    if (value instanceof StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.contains(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                }

                                default -> {
                                }
                            }
                        }
                    }
                }
            }

            frontiers = newFrontiers;
        }

        return false;
    }


    public static boolean hasRef(FieldType type) {
        switch (type) {
            case Primitive ignored -> {
                return false;
            }
            case StructRef structRef -> {
                return calcHasRef(structRef.obj());
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
        if (v == null) {
            throw new IllegalStateException(nameable.fullName() + " has no _hasRef meta value, schema not resolved!");
        }
        return v instanceof Metadata.MetaInt mi && mi.value() == 1;
    }

    public static boolean hasBlock(Nameable nameable) {
        Metadata.MetaValue v = nameable.meta().getHasBlock();
        if (v == null) {
            throw new IllegalStateException(nameable.fullName() + " has no _hasBlock meta value, schema not resolved!");
        }
        return v instanceof Metadata.MetaInt mi && mi.value() == 1;
    }
}
