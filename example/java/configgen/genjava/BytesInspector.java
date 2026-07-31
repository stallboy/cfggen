package configgen.genjava;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class BytesInspector {
    private final String bytesFilename;
    private SchemaInterface rootSchema;

    public BytesInspector(String bytesFilename) {
        this.bytesFilename = bytesFilename;
    }

    /**
     * 交互式查看：{@code bytes <表名子串>}（startsWith 匹配），打印该表的 schema 与数据；q 退出。
     */
    public void loop() throws IOException {
        Repl repl = new Repl("input>");
        registerCommands(repl);
        repl.run();
    }

    /**
     * 把本查看器的 {@code bytes <表名子串>} 命令注册进给定 {@link Repl}。
     * 命令定义集中在此；{@link #loop()} 复用它，外部也可把多组命令注册到同一个 REPL。
     */
    public void registerCommands(Repl repl) {
        repl.command("bytes", "bytes <表名子串>（查看匹配该前缀的表 schema 与数据）",
                args -> match(args.length > 0 ? args[0] : ""));
    }

    /**
     * 按表名前缀查看 bytes 文件里匹配的表（schema + 数据），返回渲染好的字符串。
     * 由 {@link Repl} 在交互式 loop 里调用，也被 {@code BytesViewTool} 一次性调用后打印。
     */
    public String match(String tableMatch) {
        out.setLength(0);
        try (ConfigInput input = new ConfigInput(Path.of(bytesFilename))) {
            // 1. 读取 Schema 长度标记
            int schemaLength = input.readInt();
            if (schemaLength > 0) {
                byte[] schemaBytes = input.readRawBytes(schemaLength);
                rootSchema = (SchemaInterface) SchemaDeserializer.deserialize(new ConfigInput(schemaBytes));
            } else {
                println("no schema in data file");
                return out.toString();
            }

            // 2. 读取 StringPool
            input.readStringPool();

            // 3. 读取 LangTextPool
            input.readLangTextPool();

            // 4. 读取表数据
            int tableCount = input.readInt();
            for (int i = 0; i < tableCount; i++) {
                String tableName = input.readString();
                int tableSize = input.readInt();
                if (tableMatch == null || tableName.startsWith(tableMatch)) {
                    boolean read = printTableInfo(tableName, tableSize, input);
                    if (read) {
                        println("");
                    } else {
                        input.skipBytes(tableSize);
                    }

                } else {
                    input.skipBytes(tableSize);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }


    private boolean printTableInfo(String tableName, int tableSize, ConfigInput input) {
        Schema schema = rootSchema.implementations.get(tableName);

        switch (schema) {
            case SchemaBean schemaBean -> {
                initDepSchemas();
                printSchemaBean(tableName, schemaBean);
                printDepSchemas();

                String schemaName = tableName + "_Entry";
                Schema entrySchema = rootSchema.implementations.get(schemaName);
                if (entrySchema instanceof SchemaEnum) {
                    printSchemaEnum(schemaName, (SchemaEnum) entrySchema);
                }

                println("%s data(size=%d):", tableName, tableSize);
                printTableData(input, schemaBean);
                return true;
            }
            case SchemaEnum schemaEnum -> {
                initDepSchemas();
                printSchemaEnum(tableName, schemaEnum);
                printDepSchemas();

                String schemaName = tableName + "_Detail";
                Schema realSchema = rootSchema.implementations.get(schemaName);
                if (realSchema instanceof SchemaBean schemaBean) {
                    println("%s data(size=%d):", tableName, tableSize);
                    printTableData(input, schemaBean);
                    return true;
                }
            }
            default -> {
            }
        }

        return false; // 忽略读，因为schema里信息已经是全的了

    }

    private void printTableData(ConfigInput input, SchemaBean tableSchema) {
        for (int c = input.readInt(); c > 0; c--) {
            StringBuilder sb = new StringBuilder();
            visitSchemaToReadData(tableSchema, input, sb);
            println(sb.toString());
        }
    }


    private final Set<Schema> printedSchemas = new HashSet<>();
    private HashMap<String, Schema> needSchemas = new LinkedHashMap<>();

    private void initDepSchemas() {
        needSchemas = new LinkedHashMap<>();
    }

    private void printDepSchemas() {
        HashMap<String, Schema> old = needSchemas;
        needSchemas = new LinkedHashMap<>();

        for (Map.Entry<String, Schema> entry : old.entrySet()) {
            visitSchemaToPrintSchema(entry.getKey(), entry.getValue());
        }
    }

    private void printSchemaBean(String name, SchemaBean schemaBean) {
        if (printedSchemas.contains(schemaBean)) {
            return;
        }

        printedSchemas.add(schemaBean);
        println(name + " {");
        indent++;

        for (SchemaBean.Column column : schemaBean.columns) {
            println("%s: %s", column.name(), column.schema());
            if (column.schema() instanceof SchemaRef ref) {
                Schema rs = rootSchema.implementations.get(ref.type);
                if (!printedSchemas.contains(rs)) {
                    needSchemas.put(ref.type, rs);
                }
            }
        }

        indent--;
        println("}");
    }

    private void printSchemaInterface(String name, SchemaInterface schemaInterface) {
        boolean isNotRoot = name != null;
        if (isNotRoot) {
            if (printedSchemas.contains(schemaInterface)) {
                return;
            }

            printedSchemas.add(schemaInterface);
            println(name + " {");

        }

        for (Map.Entry<String, Schema> stringSchemaEntry : schemaInterface.implementations.entrySet()) {
            if (isNotRoot) {
                indent++;
            }

            visitSchemaToPrintSchema(stringSchemaEntry.getKey(), stringSchemaEntry.getValue());

            if (isNotRoot) {
                indent--;
            }
        }

        if (isNotRoot) {
            println("}");
        }
    }


    private void printSchemaEnum(String name, SchemaEnum schemaEnum) {
        println("%s(isEnumPart=%s, hasIntValue=%s) {", name, schemaEnum.isEnumPart, schemaEnum.hasIntValue);
        indent++;


        for (Map.Entry<String, Integer> stringIntegerEntry : schemaEnum.values.entrySet()) {
            if (schemaEnum.hasIntValue) {
                println("%s: %d", stringIntegerEntry.getKey(), stringIntegerEntry.getValue());
            } else {
                println("%s", stringIntegerEntry.getKey());
            }
        }

        indent--;
        println("}");
    }


    private void visitSchemaToPrintSchema(String name, Schema schema) {
        switch (schema) {
            case SchemaBean schemaBean -> {
                printSchemaBean(name, schemaBean);
            }
            case SchemaEnum schemaEnum -> {
                printSchemaEnum(name, schemaEnum);
            }
            case SchemaInterface schemaInterface -> {
                printSchemaInterface(name, schemaInterface);
            }
            default -> {
            }
        }
    }


    private void visitSchemaToReadData(Schema sc, ConfigInput input, StringBuilder sb) {
        switch (sc) {
            case SchemaBean schemaBean -> {
                sb.append("(");
                int cnt = schemaBean.columns.size();
                int idx = 0;
                for (SchemaBean.Column column : schemaBean.columns) {
                    idx++;
                    visitSchemaToReadData(column.schema(), input, sb);
                    if (idx < cnt) {
                        sb.append(",");
                    }
                }
                sb.append(")");
            }
            case SchemaEnum ignored -> {
            }
            case SchemaInterface schemaInterface -> {
                String type = input.readStringInPool();
                sb.append(type);
                Schema implSchema = schemaInterface.implementations.get(type);
                visitSchemaToReadData(implSchema, input, sb);
            }
            case SchemaList schemaList -> {
                sb.append("(");
                int cnt = input.readInt();
                for (int i = 0; i < cnt; i++) {
                    visitSchemaToReadData(schemaList.ele(), input, sb);
                    if (i < cnt - 1) {
                        sb.append(",");
                    }
                }
                sb.append(")");
            }
            case SchemaMap schemaMap -> {
                sb.append("(");
                int cnt = input.readInt();
                for (int i = 0; i < cnt; i++) {
                    visitSchemaToReadData(schemaMap.key(), input, sb);
                    sb.append("=");
                    visitSchemaToReadData(schemaMap.value(), input, sb);
                    if (i < cnt - 1) {
                        sb.append(",");
                    }
                }
                sb.append(")");
            }
            case SchemaPrimitive schemaPrimitive -> {
                switch (schemaPrimitive) {
                    case SBool:
                        sb.append(input.readBool());
                        break;
                    case SInt:
                        sb.append(input.readInt());
                        break;
                    case SLong:
                        sb.append(input.readLong());
                        break;
                    case SFloat:
                        sb.append(input.readFloat());
                        break;
                    case SStr:
                        sb.append(input.readStringInPool());
                        break;
                    case SText:
                        sb.append(input.readTextInPool());
                        break;
                }
            }
            case SchemaRef schemaRef -> {
                Schema deRef = rootSchema.implementations.get(schemaRef.type);
                visitSchemaToReadData(deRef, input, sb);
            }
        }

    }


    private int indent = 0;
    private final StringBuilder tmp = new StringBuilder();
    private final StringBuilder out = new StringBuilder();

    private void println(String fmt, Object... args) {
        tmp.setLength(0);
        prefix(fmt);
        if (args.length > 0) {
            out.append(String.format(tmp.toString(), args));
        } else {
            out.append(tmp);
        }
    }

    private void prefix(String fmt) {
        tmp.repeat("    ", Math.max(0, indent));
        tmp.append(fmt);
        tmp.append('\n');
    }

}
