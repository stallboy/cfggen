package configgen.tool;

import configgen.schema.FieldSchema;
import configgen.value.CfgValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static configgen.value.CfgValue.*;

public class ValueSearcher {
    private final CfgValue cfgValue;

    public ValueSearcher(CfgValue cfgValue) {
        this.cfgValue = cfgValue;
    }

    public interface PrimitiveValueVisitor {
        void accept(PrimitiveValue value, String table, List<String> fieldChain);
    }

    public void visitPrimitiveValues(PrimitiveValueVisitor visitor) {
        for (VTable vTable : cfgValue.tables()) {
            visitPrimitiveValues(visitor, vTable);
        }
    }

    public void visitPrimitiveValues(PrimitiveValueVisitor visitor, VTable vTable) {
        for (VStruct vStruct : vTable.valueList()) {
            visitPrimitiveValues(visitor, vStruct, vTable.name(), List.of());
        }
    }

    public void visitPrimitiveValues(PrimitiveValueVisitor visitor, Value value, String table, List<String> fieldChain) {
        switch (value) {
            case PrimitiveValue primitiveValue -> {
                visitor.accept(primitiveValue, table, fieldChain);
            }
            case VStruct vStruct -> {
                int i = 0;
                for (FieldSchema field : vStruct.schema().fields()) {
                    Value fv = vStruct.values().get(i);
                    visitPrimitiveValues(visitor, fv, table, listAddOf(fieldChain, field.name()));
                    i++;
                }
            }

            case VInterface vInterface -> {
                visitPrimitiveValues(visitor, vInterface.child(), table, fieldChain);
            }
            case VList vList -> {
                int i = 0;
                for (SimpleValue sv : vList.valueList()) {
                    visitPrimitiveValues(visitor, sv, table, listAddOf(fieldChain, STR. "\{ i }" ));
                    i++;
                }
            }
            case VMap vMap -> {
                int i = 0;
                for (Map.Entry<SimpleValue, SimpleValue> entry : vMap.valueMap().entrySet()) {
                    visitPrimitiveValues(visitor, entry.getKey(), table, listAddOf(fieldChain, STR. "\{ i }k" ));
                    visitPrimitiveValues(visitor, entry.getKey(), table, listAddOf(fieldChain, STR. "\{ i }v" ));
                    i++;
                }
            }
        }
    }

    private static List<String> listAddOf(List<String> old, String e) {
        List<String> res = new ArrayList<>(old.size() + 1);
        res.addAll(old);
        res.add(e);
        return res;
    }


    public void searchInt(Set<Integer> integers) {
        visitPrimitiveValues((value, table, fieldChain) -> {
            switch (value) {
                case VInt vInt -> {
                    if (integers.contains(vInt.value())) {
                        System.out.println(STR. "\{ table } \{ String.join("-", fieldChain) } \{ vInt.value() }" );
                    }
                }
                case VLong vLong -> {
                    if (integers.contains((int) vLong.value())) {
                        System.out.println(STR. "\{ table } \{ String.join("-", fieldChain) } \{ vLong.value() }" );
                    }
                }
                default -> {
                }
            }
        });
    }

    public void searchStr(String str) {
        visitPrimitiveValues((value, table, fieldChain) -> {
            if (Objects.requireNonNull(value) instanceof StringValue sv) {
                String v = sv.value();
                if (v.contains(str)) {
                    System.out.println(STR. "\{ table } \{ String.join("-", fieldChain) } \{ v }" );
                }
            }
        });
    }

    public void search(String input) {
        if (input.startsWith("i ")) {
            Set<Integer> integers = new HashSet<>();

            for (String s : input.substring(2).split(" ")) {
                try {
                    Integer i = Integer.decode(s.trim());
                    integers.add(i);
                } catch (Exception e) {
                    System.out.println(STR. "\{ s.trim() } not int ignore" );
                    return;
                }
            }

            searchInt(integers);
        } else {
            searchStr(input);
        }
    }

    public void loop() {
        System.out.println("i <int> <int> ...  : search integers");
        System.out.println("... : search string");
        System.out.println("q : quit");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("input>");
                String input = br.readLine();
                if (input.equals("q")) {
                    break;
                }
                search(input);
            } catch (IOException e) {
                break;
            }
        }
    }

}
