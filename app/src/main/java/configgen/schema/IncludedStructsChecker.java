package configgen.schema;

import java.util.*;

public class IncludedStructsChecker {

    public enum CheckResult {
        Ok,
        Fail,
        Unknown
    }

    public interface Checker {
        CheckResult check(Nameable nameable);
    }

    public static boolean checkAnyOk(Nameable nameable, Checker checker) {
        Set<String> checked = new HashSet<>();

        Map<String, Nameable> frontiers = new HashMap<>();
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
            checked.addAll(frontiers.keySet());

            Map<String, Nameable> newFrontiers = new HashMap<>();
            for (Nameable frontier : unknownFrontiers) {
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

                                case FieldType.StructRef structRef -> {
                                    Fieldable obj = structRef.obj();
                                    String fn = obj.fullName();
                                    if (!checked.contains(fn)) {
                                        newFrontiers.put(fn, obj);
                                    }
                                }
                                case FieldType.FList fList -> {
                                    FieldType.SimpleType item = fList.item();
                                    if (item instanceof FieldType.StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.contains(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                }

                                case FieldType.FMap fMap -> {
                                    FieldType.SimpleType key = fMap.key();
                                    if (key instanceof FieldType.StructRef structRef) {
                                        Fieldable obj = structRef.obj();
                                        String fn = obj.fullName();
                                        if (!checked.contains(fn)) {
                                            newFrontiers.put(fn, obj);
                                        }
                                    }
                                    FieldType.SimpleType value = fMap.value();
                                    if (value instanceof FieldType.StructRef structRef) {
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

}
