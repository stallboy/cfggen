package configgen.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import configgen.editorserver.SchemaService;
import configgen.schema.CfgSchema;
import configgen.schema.Nameable;
import configgen.schema.TableSchema;
import configgen.util.UTF8Writer;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import configgen.value.RefSearcher;

import java.io.*;
import java.util.*;

import static configgen.value.CfgValue.*;

public class ValueSearcher {
    private final CfgValue cfgValue;
    private final UTF8Writer fileWriter;

    public ValueSearcher(CfgValue cfgValue, String searchTo) {
        this.cfgValue = cfgValue;
        if (searchTo != null) {
            try {
                fileWriter = new UTF8Writer(new FileOutputStream(searchTo));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            fileWriter = null;
        }
    }

    private void println(String fmt, Object... args) {
        String str;
        if (args.length == 0) {
            str = fmt;
        } else {
            str = String.format(fmt, args);
        }

        if (fileWriter != null) {
            fileWriter.write(str + System.lineSeparator());
        } else {
            System.out.println(str);
        }
    }

    private void print(String str) {
        if (fileWriter != null) {
            fileWriter.write(str);
        } else {
            System.out.print(str);
        }
    }

    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }


    public void searchInt(Set<Integer> integers) {
        ForeachValue.searchCfgValue((primitiveValue, table, pk, fieldChain) -> {
            switch (primitiveValue) {
                case VInt vInt -> {
                    if (integers.contains(vInt.value())) {
                        println("%s[%s], %s, %d", table, pk.packStr(),
                                String.join(".", fieldChain), vInt.value());
                    }
                }
                case VLong vLong -> {
                    if (integers.contains((int) vLong.value())) {
                        println("%s[%s], %s, %d", table, pk.packStr(),
                                String.join(".", fieldChain), vLong.value());
                    }
                }
                default -> {
                }
            }
        }, cfgValue);
    }

    public void searchStr(String str) {
        ForeachValue.searchCfgValue((primitiveValue, table, pk, fieldChain) -> {
            if (Objects.requireNonNull(primitiveValue) instanceof StringValue sv) {
                String v = sv.value();
                if (v.contains(str)) {
                    println("%s[%s], %s, %s", table, pk.packStr(),
                            String.join(".", fieldChain), v);
                }
            }
        }, cfgValue);
    }

    public void searchRef(String refTable, List<String> nullableUniqKeys, Set<String> ignoredTables) {
        RefSearcher.RefSearchResult res = RefSearcher.search(cfgValue, refTable, nullableUniqKeys, ignoredTables);
        switch (res.err()) {
            case Ok -> {
                for (Map.Entry<Value, Set<String>> e : res.value2tables().entrySet()) {
                    Set<String> tables = e.getValue();
                    println("%s, %d, %s", e.getKey().packStr(), tables.size(), tables);
                }
            }
            case TableNotFound -> {
                println("table %s not found", refTable);
            }
            case UniqueKeyNotFound -> {
                println("table %s unique key %s not found", refTable, nullableUniqKeys);
            }
        }
    }

    public void listNameOfSchemas(String want) {
        for (Nameable nameable : cfgValue.schema().items()) {
            String name = nameable.name();
            if (name.contains(want)) {
                println(name);
            }
        }
    }

    public void listSchemas(String want) {
        for (Nameable nameable : cfgValue.schema().items()) {
            String name = nameable.name();
            if (name.contains(want)) {
                print(nameable.stringify());

                if (nameable instanceof TableSchema) {
                    VTable vTable = cfgValue.vTableMap().get(name);

                    println("count: " + vTable.valueList().size());
                    int c = 0;
                    for (VStruct vStruct : vTable.valueList()) {
                        println(vStruct.packStr());
                        c++;
                        if (c >= 100) {
                            break;
                        }
                    }
                }
            }
        }
    }


    public void listSchemasUseJson(String want) {
        CfgSchema schema = CfgSchema.of();
        for (Nameable nameable : cfgValue.schema().items()) {
            String name = nameable.name();
            if (name.contains(want)) {
                schema.add(nameable);
            }
        }
        println(JSON.toJSONString(SchemaService.fromCfgSchema(schema), JSONWriter.Feature.PrettyFormat));
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
                        println("%s not int ignore", s.trim());
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
                    String refTable = params.getFirst();
                    List<String> nullableUniqKeys = null;
                    int idx = refTable.indexOf('[');
                    if (idx != -1) {
                        String full = refTable;
                        refTable = full.substring(0, idx);
                        String[] keys = full.substring(idx + 1, full.length() - 1).split("\\s*,\\s*");
                        nullableUniqKeys = Arrays.asList(keys);
                    }
                    Set<String> ignoredTables = new LinkedHashSet<>(params.subList(1, params.size()));
                    searchRef(refTable, nullableUniqKeys, ignoredTables);
                }
            }
            case "sl" -> {
                String want = "";
                if (!params.isEmpty()) {
                    want = params.getFirst();
                }
                listNameOfSchemas(want);
            }
            case "sll" -> {
                String want = "";
                if (!params.isEmpty()) {
                    want = params.getFirst();
                }
                listSchemas(want);
            }
            case "slljson" -> {
                String want = "";
                if (!params.isEmpty()) {
                    want = params.getFirst();
                }
                listSchemasUseJson(want);
            }

            default -> println("%s unknown", func);
        }
    }

    public static void printUsage(String prefix) {
        System.out.println(prefix + "int <int> <int> ...: search integers");
        System.out.println(prefix + "str <str>: search string");
        System.out.println(prefix + "ref <refTable<[uniqKeys]>?> <IgnoredTables>: search ref");

        System.out.println(prefix + "sl  <name>?: list name of schemas");
        System.out.println(prefix + "sll <name>?: list schemas");
        System.out.println(prefix + "slljson <name>?: list schemas. use json");


        System.out.println(prefix + "h: help");
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
                if (func.equals("h")) {
                    printUsage("");
                } else {
                    search(func, Arrays.asList(args).subList(1, args.length));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

}
