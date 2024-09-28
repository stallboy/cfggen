package configgen.schema;

import java.util.*;

public class IncludedStructs {

    public enum CheckResult {
        Ok,
        Fail,
        Unknown
    }

    public interface Checker {
        CheckResult check(Nameable nameable);
    }

    private final static Checker unknownChecker = nameable -> CheckResult.Unknown;

    /**
     * @param nameable 目标
     * @return 目标包含的所有子结构，包括自己，包括interface下的struct，key为fullName
     */
    public static Map<String, Nameable> findAllIncludedStructs(Nameable nameable) {
        Map<String, Nameable> result = new LinkedHashMap<>();
        checkAnyOk(nameable, unknownChecker, result);
        return result;
    }

    /**
     * @param nameable 目标
     * @param checker  针对单个结构进行检查
     * @return 目标包含的所有子结构是否有任意一个返回ok
     */
    public static boolean checkAnyOk(Nameable nameable, Checker checker) {
        return checkAnyOk(nameable, checker, new HashMap<>(4));
    }

    public static boolean checkAnyOk(Nameable nameable, Checker checker, Map<String, Nameable> checked) {
        Map<String, Nameable> frontiers = new LinkedHashMap<>();
        frontiers.put(nameable.fullName(), nameable);

        while (!frontiers.isEmpty()) {
            List<Nameable> unknownFrontiers = new ArrayList<>(4);
            for (Nameable frontier : frontiers.values()) {
                CheckResult res = checker.check(frontier);
                switch (res) {
                    case Ok -> {
                        return true;
                    }
                    case Fail -> {
                    }
                    case Unknown -> {
                        unknownFrontiers.add(frontier);
                    }
                }
            }
            checked.putAll(frontiers);

            Map<String, Nameable> newFrontiers = new HashMap<>();
            for (Nameable frontier : unknownFrontiers) {
                // expand
                switch (frontier) {
                    case InterfaceSchema sInterface -> {
                        for (StructSchema impl : sInterface.impls()) {
                            String fn = impl.fullName();
                            if (!checked.containsKey(fn)) {
                                newFrontiers.put(fn, impl);
                            }
                        }
                    }
                    case Structural structural -> {
                        for (FieldSchema field : structural.fields()) {
                            switch (field.type()) {

                                case FieldType.StructRef structRef -> {
                                    Fieldable obj = structRef.obj();
                                    String fn = obj.fullName();
                                    if (!checked.containsKey(fn)) {
                                        newFrontiers.put(fn, obj);
                                    }
                                }
                                case FieldType.FList fList -> {
                                    FieldType.SimpleType item = fList.item();
                                    if (item instanceof FieldType.StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.containsKey(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                }

                                case FieldType.FMap fMap -> {
                                    FieldType.SimpleType key = fMap.key();
                                    if (key instanceof FieldType.StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.containsKey(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                    FieldType.SimpleType value = fMap.value();
                                    if (value instanceof FieldType.StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.containsKey(fn)) {
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

}
