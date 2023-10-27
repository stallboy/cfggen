package configgen.tool;

import configgen.value.CfgValue;
import configgen.value.ForeachPrimitiveValue;
import configgen.value.RefSearcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static configgen.value.CfgValue.*;

public record ValueSearcher(CfgValue cfgValue) {

    public void searchInt(Set<Integer> integers) {
        ForeachPrimitiveValue.foreach((primitiveValue, table, fieldChain) -> {
            switch (primitiveValue) {
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
        }, cfgValue);
    }

    public void searchStr(String str) {
        ForeachPrimitiveValue.foreach((primitiveValue, table, fieldChain) -> {
            if (Objects.requireNonNull(primitiveValue) instanceof StringValue sv) {
                String v = sv.value();
                if (v.contains(str)) {
                    System.out.println(STR. "\{ table } \{ String.join("-", fieldChain) } \{ v }" );
                }
            }
        }, cfgValue);
    }

    public void searchRef(String refTable, List<String> nullableForeignKeys, Set<String> ignoredTables) {
        RefSearcher.RefSearchResult res = RefSearcher.search(cfgValue, refTable, nullableForeignKeys, ignoredTables);
        switch (res.err()) {
            case Ok -> {
                for (Map.Entry<Value, Set<String>> e : res.value2tables().entrySet()) {
                    Set<String> tables = e.getValue();
                    System.out.println(STR. "\{ e.getKey().repr() }, \{ tables.size() }, \{ tables }" );
                }
            }
            case TableNotFound -> {
                System.out.println(STR. "table \{ refTable } not found" );
            }
            case UniqueKeyNotFound -> {
                System.out.println(STR. "table \{ refTable } unique key \{ nullableForeignKeys } not found" );
            }
        }
    }

    public void search(String func, List<String> params) {
        switch (func) {
            case "int" -> {
                Set<Integer> integers = new HashSet<>();
                for (String s : params) {
                    try {
                        Integer i = Integer.decode(s.trim());
                        integers.add(i);
                    } catch (Exception e) {
                        System.out.println(STR. "\{ s.trim() } not int ignore" );
                        return;
                    }
                }
                searchInt(integers);
            }
            case "str" -> {
                for (String param : params) {
                    searchStr(param);
                }
            }

            case "ref" -> {
                if (!params.isEmpty()) {
                    String refTable = params.get(0);
                    List<String> nullableForeignKeys = null;
                    int idx = refTable.indexOf('[');
                    if (idx != -1) {
                        String full = refTable;
                        refTable = full.substring(0, idx);
                        String[] keys = full.substring(idx + 1, full.length() - 1).split("\\s*,\\s*");
                        nullableForeignKeys = Arrays.asList(keys);
                    }
                    Set<String> ignoredTables = new LinkedHashSet<>(params.subList(1, params.size()));
                    searchRef(refTable, nullableForeignKeys, ignoredTables);
                }
            }
            default -> System.out.println(STR. "\{ func } unknownqq" );
        }
    }

    public static void printUsage(String prefix){
        System.out.println(prefix + "int <int> <int> ...: search integers");
        System.out.println(prefix + "str <str>: search string");
        System.out.println(prefix + "ref <refTable> <IgnoredTables>: search ref");
        System.out.println(prefix + "q: quit");
    }
    public void loop() {
        printUsage("");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("input>");
                String input = br.readLine();
                String[] args = input.trim().split("\\s+");
                if (args.length == 0) {
                    continue;
                }

                String func = args[0];
                if (func.equals("q")) {
                    break;
                }
                search(func, Arrays.asList(args).subList(1, args.length));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

}
