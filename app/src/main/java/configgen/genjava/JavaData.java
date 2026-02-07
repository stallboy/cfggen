package configgen.genjava;

import java.io.*;
import java.util.*;

public class JavaData {

    private final String javaDataFile;

    private SchemaInterface rootSchema;

    public JavaData(String javaDataFile) {
        this.javaDataFile = javaDataFile;
    }


    public void loop() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.print("input>");
                String input = br.readLine();
                if (input.equals("q")) {
                    break;
                }
                match(input);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void match(String match) {
        try (ConfigInput input = new ConfigInput(new DataInputStream(new BufferedInputStream(new FileInputStream(javaDataFile))))) {
            rootSchema = (SchemaInterface) new SchemaDeserializer(input).deserialize();

            int tableCount = input.readInt();
            for (int i = 0; i < tableCount; i++) {
                String tableName = input.readStr();
                int tableSize = input.readInt();
                if (match == null || tableName.startsWith(match)) {
                    printTableInfo(tableName, tableSize, input);
                    println("");
                } else {
                    input.skipBytes(tableSize);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void printTableInfo(String tableName, int tableSize, ConfigInput input) {
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
            }
            case SchemaEnum schemaEnum -> {
                initDepSchemas();
                printSchemaEnum(tableName, schemaEnum);
                printDepSchemas();

                String schemaName = tableName + "_Detail";
                Schema realSchema = rootSchema.implementations.get(schemaName);
                if (realSchema instanceof SchemaBean) {
                    println("%s data(size=%d):", tableName, tableSize);
                    printTableData(input, (SchemaBean) realSchema);
                }
            }
            default -> {
            }
        }

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
        println(name + " {");
        indent++;

        if (printedSchemas.contains(schemaBean)) {
            return;
        }

        printedSchemas.add(schemaBean);

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
                String type = input.readStr();
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
                        sb.append(input.readStr());
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

    private void println(String fmt, Object... args) {
        tmp.setLength(0);
        if (args.length > 0) {
            prefix(fmt);
            System.out.printf(tmp.toString(), args);
        } else {
            prefix(fmt);
            System.out.print(tmp);
        }
    }

    private void prefix(String fmt) {
        tmp.append("    ".repeat(Math.max(0, indent)));
        tmp.append(fmt);
        tmp.append('\n');
    }

}
