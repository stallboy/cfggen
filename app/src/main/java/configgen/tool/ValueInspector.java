package configgen.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import configgen.editorserver.SchemaService;
import configgen.schema.CfgSchema;
import configgen.schema.Nameable;
import configgen.schema.TableSchema;
import configgen.util.Logger;
import configgen.util.BomUtf8Writer;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import configgen.value.RefSearcher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static configgen.value.CfgValue.*;

/**
 * 核心逻辑类：负责具体的搜索实现与交互循环
 */
public class ValueInspector implements AutoCloseable {
    private final CfgValue cfgValue;
    private final BomUtf8Writer fileWriter;

    public ValueInspector(CfgValue cfgValue, String searchTo) {
        this.cfgValue = cfgValue;
        if (searchTo != null) {
            try {
                // 确保父目录存在
                Path path = Path.of(searchTo);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent()); // 自动处理已存在的情况，且支持多级目录
                }
                fileWriter = new BomUtf8Writer(path);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create output file: " + searchTo, e);
            }
        } else {
            fileWriter = null;
        }
    }

    // --- 打印辅助逻辑 ---

    private void println(String fmt, Object... args) {
        String str = (args.length == 0) ? fmt : String.format(fmt, args);
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

    @Override
    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
            Logger.log("Inspector output closed.");
        }
    }

    // --- 搜索核心逻辑 ---

    public void search(String func, List<String> params) {
        // 统一小写处理，增加容错性
        String cmd = func.toLowerCase();

        switch (cmd) {
            case "int" -> handleIntSearch(params);
            case "str" -> handleStrSearch(params);
            case "ref" -> handleRefSearch(params);
            case "sl" -> listNameOfSchemas(params.isEmpty() ? "" : params.getFirst());
            case "sll" -> listSchemas(params.isEmpty() ? "" : params.getFirst());
            case "slljson" -> listSchemasUseJson(params.isEmpty() ? "" : params.getFirst());
            default -> println("Unknown command '%s'. Type 'h' for help.", func);
        }
    }

    private void handleIntSearch(List<String> params) {
        if (params.isEmpty()) {
            println("Usage: int <num1> [num2]...");
            return;
        }
        Set<Integer> integers = new HashSet<>();
        for (String s : params) {
            try {
                integers.add(Integer.decode(s.trim()));
            } catch (NumberFormatException e) {
                println("'%s' is not a valid integer, ignored.", s);
            }
        }
        if (!integers.isEmpty()) searchInt(integers);
    }

    private void handleStrSearch(List<String> params) {
        if (params.isEmpty()) {
            println("Usage: str <substring>");
            return;
        }
        params.forEach(this::searchStr);
    }

    private void handleRefSearch(List<String> params) {
        if (params.isEmpty()) {
            println("Usage: ref <refTable[key]> [ignoredTable1]...");
            return;
        }
        String refTable = params.getFirst();
        List<String> nullableUniqKeys = null;
        int idx = refTable.indexOf('[');
        if (idx != -1) {
            String keysPart = refTable.substring(idx + 1, refTable.length() - 1);
            refTable = refTable.substring(0, idx);
            nullableUniqKeys = Arrays.asList(keysPart.split("\\s*,\\s*"));
        }
        Set<String> ignoredTables = new LinkedHashSet<>(params.subList(1, params.size()));
        searchRef(refTable, nullableUniqKeys, ignoredTables);
    }

    public void searchInt(Set<Integer> integers) {
        ForeachValue.searchCfgValue((primitiveValue, table, pk, fieldChain) -> {
            long val = 0;
            boolean match = false;
            if (primitiveValue instanceof VInt v) {
                val = v.value();
                match = integers.contains((int) val);
            } else if (primitiveValue instanceof VLong v) {
                val = v.value();
                match = integers.contains((int) val);
            }
            if (match) {
                println("%s[%s], %s, %d", table, pk.packStr(), String.join(".", fieldChain), val);
            }
        }, cfgValue);
    }

    public void searchStr(String str) {
        ForeachValue.searchCfgValue((primitiveValue, table, pk, fieldChain) -> {
            if (primitiveValue instanceof StringValue sv) {
                String v = sv.value();
                if (v != null && v.contains(str)) {
                    println("%s[%s], %s, %s", table, pk.packStr(), String.join(".", fieldChain), v);
                }
            }
        }, cfgValue);
    }

    public void searchRef(String refTable, List<String> nullableUniqKeys, Set<String> ignoredTables) {
        RefSearcher.RefSearchResult res = RefSearcher.search(cfgValue, refTable, nullableUniqKeys, ignoredTables);
        switch (res.err()) {
            case Ok -> res.value2tables().forEach((val, tables) ->
                    println("%s, %d, %s", val.packStr(), tables.size(), tables));
            case TableNotFound -> println("Table '%s' not found.", refTable);
            case UniqueKeyNotFound -> println("Unique key %s not found in table '%s'.", nullableUniqKeys, refTable);
        }
    }

    public void listNameOfSchemas(String want) {
        cfgValue.schema().items().stream()
                .map(Nameable::name)
                .filter(name -> name.contains(want))
                .forEach(this::println);
    }

    public void listSchemas(String want) {
        for (Nameable nameable : cfgValue.schema().items()) {
            String name = nameable.name();
            if (name.contains(want)) {
                print(nameable.stringify());
                if (nameable instanceof TableSchema) {
                    VTable vTable = cfgValue.vTableMap().get(name);
                    List<VStruct> list = vTable.valueList();
                    println(" (count: %d)", list.size());
                    list.stream().limit(100).forEach(v -> println("  " + v.packStr()));
                }
            }
        }
    }

    public void listSchemasUseJson(String want) {
        CfgSchema schema = CfgSchema.of();
        cfgValue.schema().items().stream()
                .filter(n -> n.name().contains(want))
                .forEach(schema::add);
        println(JSON.toJSONString(SchemaService.fromCfgSchema(schema), JSONWriter.Feature.PrettyFormat));
    }

    // --- 交互循环逻辑 ---

    public void loop() {
        printHelp();
        // 使用 try-with-resources 自动关闭输入流
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("input>");
                String line = br.readLine();
                if (line == null) break; // 处理 Ctrl+D

                line = line.trim();
                if (line.isEmpty()) {
                    continue; // 解决回车 unknown 的关键：直接跳过空行
                }

                String[] parts = line.split("\\s+");
                String func = parts[0].toLowerCase();

                if (func.equals("q") || func.equals("exit")) break;
                if (func.equals("h") || func.equals("help")) {
                    printHelp();
                    continue;
                }

                search(func, Arrays.asList(parts).subList(1, parts.length));
            }
        } catch (IOException e) {
            Logger.log("Read error: " + e.getMessage());
        }
    }

    private void printHelp() {
        help().forEach(Logger::log);
    }

    public static List<String> help() {
        return List.of(
                "int <num> [num]...      : Search for integers (supports hex 0x)",
                "str <substring>         : Search for strings containing substring",
                "ref <table[key]> [skip] : Search references to a table/key",
                "sl  [name]              : List matching schema names",
                "sll [name]              : List matching schemas with data (limit 100)",
                "slljson [name]          : List matching schemas in JSON format",
                "h / help                : Show this help",
                "q / exit                : Quit"
        );
    }
}