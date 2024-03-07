package configgen.schema;

import java.util.*;

import static configgen.schema.FieldType.*;

public class HasRef {

    public static void preCalculateAllHasRef(CfgSchema schema) {
        ForeachSchema.foreachNameable(HasRef::calcHasRef, schema);
    }

    private static boolean calcHasRef(Nameable nameable) {
        Metadata meta = nameable.meta();
        Metadata.MetaValue hasRefValue = meta.getHasRef();
        if (hasRefValue != null) {
            return hasRefValue == Metadata.MetaTag.BOOL_TRUE;
        }
        boolean hasRef = checkIfIncludedStructsHasRef(nameable);
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


    private static boolean checkIfIncludedStructsHasRef(Nameable nameable) {
        Set<String> checked = new HashSet<>();

        Map<String, Nameable> frontiers = new HashMap<>();
        frontiers.put(nameable.fullName(), nameable);


        while (!frontiers.isEmpty()) {
            for (Nameable frontier : frontiers.values()) {
                if (checkIfDirectFieldsHasRef(frontier)) {
                    return true;
                }
            }
            checked.addAll(frontiers.keySet());

            Map<String, Nameable> newFrontiers = new HashMap<>();
            // expand
            switch (nameable) {
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
        return nameable.meta().getHasRef() == Metadata.MetaTag.BOOL_TRUE;
    }
}
