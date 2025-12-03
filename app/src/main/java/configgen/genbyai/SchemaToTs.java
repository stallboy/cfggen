package configgen.genbyai;

import configgen.schema.*;
import configgen.value.CfgValue;
import configgen.value.ValueUtil;

import java.util.*;

public class SchemaToTs {
    private final CfgValue cfgValue;
    private final TableSchema tableSchema;
    private final boolean isGenerate$type;
    private final StringBuilder sb = new StringBuilder(2048);

    private final Map<String, Map<String, Struct>> namespaces = new LinkedHashMap<>();
    private final IdentityHashMap<FieldSchema, RefName> refNames = new IdentityHashMap<>();
    private final List<String> extraRefTables;

    private record Struct(Nameable nameable,
                          String tableKey /* 如果为null，表示结构，否则表示引用表的数据 */) {
    }

    private record RefName(String namespace,
                           String lastName) {
    }


    public SchemaToTs(CfgValue cfgValue, TableSchema tableSchema, List<String> extraRefTables, boolean isGenerate$type) {
        this.cfgValue = cfgValue;
        this.tableSchema = tableSchema;
        this.extraRefTables = extraRefTables;
        this.isGenerate$type = isGenerate$type;
    }

    public String generate() {
        Map<String, Nameable> allIncludedStructs = IncludedStructs.findAllIncludedStructs(tableSchema);
        for (Nameable struct : allIncludedStructs.values()) {
            addStruct(struct.lastName(), struct, null);

            if (struct instanceof Structural structural) {
                for (ForeignKeySchema fk : structural.foreignKeys()) {
                    // 简单一点: 1.只处理单字段，2.只考虑枚举table或在extraRefTables中的table
                    if (fk.key().fields().size() == 1
                            && (fk.refTableSchema().entry() instanceof EntryType.EEnum
                            || extraRefTables.contains(fk.refTableNormalized()))) {
                        TableSchema refTable = fk.refTableSchema();
                        KeySchema refKey = null;
                        switch (fk.refKey()) {
                            case RefKey.RefPrimary ignored -> {
                                refKey = refTable.primaryKey();
                            }
                            case RefKey.RefUniq refUniq -> {
                                refKey = refUniq.key();
                            }
                            case RefKey.RefList ignored -> {
                            }
                        }

                        if (refKey != null && refKey.fieldSchemas().size() == 1) {
                            FieldSchema refKeyField = refKey.fieldSchemas().getFirst();
                            if (refKeyField.type() instanceof FieldType.Primitive) {
                                FieldSchema field = fk.key().fieldSchemas().getFirst();
                                String lastName = refTable.lastName() + "_" + refKeyField.name();
                                refNames.put(field, new RefName(refTable.namespace(), lastName));
                                addStruct(lastName, refTable, refKeyField.name());
                            }
                        }
                    }
                }
            }
        }

        int nsCount = namespaces.size();
        int nsIdx = 0;
        for (Map.Entry<String, Map<String, Struct>> ns : namespaces.entrySet()) {
            String nsName = ns.getKey();
            println("namespace %s {", nsName);
            Map<String, Struct> structMap = ns.getValue();
            for (Map.Entry<String, Struct> e : structMap.entrySet()) {
                String structName = e.getKey();
                Struct struct = e.getValue();
                if (struct.tableKey == null) {
                    generateTypeDeclaration(structName, struct.nameable);
                } else {
                    generateUnionTypeByValues(structName, struct.nameable, struct.tableKey);
                }
            }

            nsIdx++;
            if (nsIdx < nsCount) {
                println("}");
            } else {
                sb.append("}");
            }
        }

        return sb.toString();
    }

    private void addStruct(String name, Nameable struct, String tableKey) {
        String ns = struct.namespace();
        if (struct instanceof StructSchema sc) {
            if (sc.nullableInterface() != null) {
                ns = sc.nullableInterface().fullName();
            }
        }

        Map<String, Struct> structMap = namespaces.computeIfAbsent(ns, k -> new LinkedHashMap<>());
        structMap.put(name, new Struct(struct, tableKey));
    }

    private void println(String fmt, Object... args) {
        if (args.length == 0) {
            sb.append(fmt);
        } else {
            sb.append(String.format(fmt, args));
        }

        sb.append(System.lineSeparator());
    }

    private void generateTypeDeclaration(String structName, Nameable struct) {
        switch (struct) {
            case Structural structural -> {
                println("export interface %s {%s", structName, comment(structural.comment()));
                if (isGenerate$type) {
                    println("\t$type: \"%s\"", structural.fullName());
                }
                for (FieldSchema field : structural.fields()) {
                    println("\t%s: %s;%s", field.name(), fieldType(field, structural), comment(field.comment()));
                }
                println("}");
            }

            case InterfaceSchema interfaceSchema -> {
                println("export type %s = ", structName);
                int i = 0;
                int size_1 = interfaceSchema.impls().size() - 1;
                for (StructSchema impl : interfaceSchema.impls()) {
                    String or = i < size_1 ? " |" : ";";
                    println("\t%s%s", impl.fullName(), or);
                    i++;
                }
            }
        }
    }

    private String comment(String raw) {
        return raw.isEmpty() ? "" : " /* %s */".formatted(raw);
    }

    private void generateUnionTypeByValues(String structName, Nameable nameable, String tableKey) {
        CfgValue.VTable table = cfgValue.getTable(nameable.name());
        Objects.requireNonNull(table);


        Set<String> fieldNames = new LinkedHashSet<>(2);
        if (table.schema().entry() instanceof EntryType.EEnum eEnum) {
            fieldNames.add(eEnum.field());
        }
        String title = table.schema().meta().getStr("title", null);
        if (title != null) {
            fieldNames.add(title);
        }
        fieldNames.remove(tableKey);


        println("export type %s = ", structName);
        int idx = 0;
        List<CfgValue.VStruct> valueList = table.valueList();
        int size_1 = valueList.size() - 1;
        for (CfgValue.VStruct record : valueList) {
            CfgValue.Value fv = ValueUtil.extractFieldValue(record, tableKey);
            Objects.requireNonNull(fv);
            String v;
            switch (fv) {
                case CfgValue.StringValue stringValue -> {
                    v = "'%s'".formatted(stringValue.value());
                }
                case CfgValue.VFloat vFloat -> {
                    v = String.valueOf(vFloat.value());
                }
                case CfgValue.VInt vInt -> {
                    v = String.valueOf(vInt.value());
                }
                case CfgValue.VLong vLong -> {
                    v = String.valueOf(vLong);
                }

                default -> {
                    throw new RuntimeException(fv + " not supported");
                }
            }

            StringBuilder comment = new StringBuilder();
            int i = 0;
            for (String fn : fieldNames) {
                if (i > 0) {
                    comment.append(",");
                }
                CfgValue.Value d = ValueUtil.extractFieldValue(record, fn);
                if (d instanceof CfgValue.StringValue sv) {
                    comment.append(sv.value());
                }
                i++;
            }

            String or = idx < size_1 ? " |" : ";";
            println("\t%s%s%s", v, comment(comment.toString()), or);
            idx++;
        }
    }

    private String fieldType(FieldSchema field, Structural structural) {
        RefName rn = refNames.get(field);
        if (rn != null) {
            String refName;
            if (rn.namespace.equals(structural.namespace())) {
                refName = rn.lastName;
            } else {
                refName = rn.namespace + "." + rn.lastName;
            }

            if (field.type() instanceof FieldType.FList) {
                return refName + "[]";
            } else {
                return refName;
            }
        }
        return fieldType(field.type(), structural);
    }

    private String fieldType(FieldType fieldType, Structural structural) {
        switch (fieldType) {
            case FieldType.Primitive primitive -> {
                return switch (primitive) {
                    case INT, LONG, FLOAT -> "number";
                    case BOOL -> "boolean";
                    case STRING, TEXT -> "string";
                };
            }
            case FieldType.StructRef structRef -> {
                Fieldable obj = structRef.obj();
                if (obj.namespace().equals(structural.namespace())) {
                    return obj.lastName();
                }
                return obj.fullName();
            }
            case FieldType.FList fList -> {
                return fieldType(fList.item(), structural) + "[]";
            }
            case FieldType.FMap ignored -> {
                throw new IllegalArgumentException("map not supported");
            }
        }
    }

}
