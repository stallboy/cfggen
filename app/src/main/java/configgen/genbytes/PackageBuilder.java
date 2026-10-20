package configgen.genbytes;

import configgen.ctx.Context;
import configgen.data.Source;
import configgen.genjava.*;
import configgen.schema.*;
import configgen.value.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static configgen.value.CfgValue.*;

/** Parse local records first; dependencies and this layer are validated as one effective data set. */
final class PackageBuilder {
    record Result(CfgValue own, CfgValue effective) {}

    static Result build(Context ctx, String tag, List<Path> paths, String name, String requires) throws IOException {
        CfgSchema schema = ctx.cfgSchema();
        if (tag != null) {
            var errors = CfgSchemaErrs.of();
            schema = new CfgSchemaFilterByTag(schema, tag, errors).filter();
            new CfgSchemaResolver(schema, errors).resolve();
            errors.checkErrors("package schema");
        }
        var packs = new ArrayList<ConfigPackage>();
        for (Path path : paths) {
            try (var input = new ConfigInput(path)) { packs.add(ConfigPackage.read(input)); }
        }
        if (!packs.isEmpty()) ConfigPackage.validateOrder(packs);
        var available = new HashMap<String, ConfigPackage>();
        for (var pack : packs) available.put(pack.name(), pack);
        var declared = new LinkedHashSet<String>();
        if (!requires.isEmpty()) {
            for (String item : requires.split(",", -1)) {
                String dependency = item.strip();
                if (dependency.isEmpty() || dependency.equals(name) || !declared.add(dependency) || !available.containsKey(dependency)) {
                    throw new ConfigErr("Missing or invalid build dependency for " + name + ": " + item);
                }
            }
        }
        if (available.containsKey(name)) throw new ConfigErr("Pack depends on itself: " + name);
        var reachable = new HashSet<String>();
        var pending = new ArrayDeque<>(declared);
        while (!pending.isEmpty()) {
            String dependency = pending.removeFirst();
            if (reachable.add(dependency)) pending.addAll(available.get(dependency).dependencies());
        }
        if (!reachable.containsAll(available.keySet())) throw new ConfigErr("Build inputs include undeclared dependencies");

        var errors = CfgValueErrs.of();
        Map<String, List<VStruct>> local = new LinkedHashMap<>();
        for (TableSchema table : schema.tableMap().values()) {
            var source = ctx.cfgData().getDTable(table.name());
            if (source != null) {
                local.put(table.name(), new VTableParser(table, source, ctx.cfgSchema().findTable(table.name()), ctx.contextCfg().headRow(), errors).parseRecords());
            } else if (!ctx.sourceStructure().jsonFilesOf(table.name()).isEmpty()) {
                local.put(table.name(), new VTableJsonParser(table, schema.isPartial(), ctx.sourceStructure(),
                        ctx.cfgSchema().findTable(table.name()), errors, new CfgValueStat()).parseRecords());
            } else if (packs.isEmpty() && table.meta().getEnumValues() != null) {
                local.put(table.name(), new VTableCreator(table, errors).create(List.of()).valueList());
            }
        }
        errors.checkErrors("package fields", false);
        Map<String, LinkedHashMap<Value, VStruct>> merged = new LinkedHashMap<>();
        for (String table : schema.tableMap().keySet()) merged.put(table, new LinkedHashMap<>());
        String[] languages = ctx.nullableLangSwitch() == null ? new String[0] : new String[]{ctx.nullableLangSwitch().defaultLang()};
        for (var pack : packs) {
            for (String block : pack.blockNames()) {
                if (!block.startsWith(".")) continue;
                String tableName = block.substring(1);
                TableSchema table = schema.findTable(tableName);
                if (table == null) throw new ConfigErr("Unknown table in dependency " + pack.name() + ": " + tableName);
                var input = pack.block(block, languages);
                int count = input.readCount(1);
                var rows = new ArrayList<VStruct>(count);
                for (int i = 0; i < count; i++) rows.add(readStruct(table, input, Source.DFile.of(pack.name(), tableName).child(Integer.toString(i))));
                input.requireEnd();
                merge(table, merged.get(tableName), rows);
            }
        }
        for (var entry : local.entrySet()) merge(schema.findTable(entry.getKey()), merged.get(entry.getKey()), entry.getValue());
        CfgValue effective = CfgValue.of(schema);
        CfgValue own = CfgValue.of(schema);
        for (TableSchema table : schema.tableMap().values()) {
            var rows = new ArrayList<>(merged.get(table.name()).values());
            if (table.primaryKey().fieldSchemas().size() == 1 && table.primaryKey().fieldSchemas().getFirst().isSeq()) {
                rows.sort(Comparator.comparingInt(row -> ((VInt) ValueUtil.extractPrimaryKeyValue(row, table)).value()));
            }
            VTable all = new VTableCreator(table, errors).createRecords(rows);
            if (table.meta().hasEnumValues()) {
                VTable fixed = new VTableCreator(table, errors).create(List.of());
                if (!Objects.equals(fixed.enumNames(), all.enumNames())
                        || !Objects.equals(fixed.enumNameToIntegerValueMap(), all.enumNameToIntegerValueMap())) {
                    throw new ConfigErr("Fixed schema enum identity changed: " + table.name());
                }
                if (table.meta().getEnumValues() instanceof Metadata.MetaEnumValues.OfAssigned) {
                    int idIndex = FindFieldIndex.findFieldIndex(table, table.findField("id"));
                    for (var fixedRow : fixed.primaryKeyMap().entrySet()) {
                        if (!fixedRow.getValue().values().get(idIndex).equals(all.primaryKeyMap().get(fixedRow.getKey()).values().get(idIndex))) throw new ConfigErr("Fixed schema enum ID changed: " + table.name() + "." + fixedRow.getKey());
                    }
                }
            }
            effective.vTableMap().put(table.name(), all);
            if (local.containsKey(table.name())) {
                var ownKeys = new LinkedHashMap<Value, VStruct>();
                for (VStruct row : local.get(table.name())) ownKeys.put(ValueUtil.extractPrimaryKeyValue(row, table), row);
                own.vTableMap().put(table.name(), new VTable(table, local.get(table.name()), ownKeys,
                        new LinkedHashMap<>(), all.enumNames(), all.enumNameToIntegerValueMap()));
            }
        }
        new RefValidator(effective, errors).validate();
        errors.checkErrors("effective package " + name, false);
        var expected = SchemaParser.parsePackage(effective, ctx.nullableLangSwitch());
        for (var pack : packs) {
            var input = pack.block("schema");
            Schema actual = SchemaDeserializer.deserialize(input); input.requireEnd();
            if (!expected.compatible(actual) || !actual.compatible(expected)) throw new ConfigErr("Schema or fixed symbols differ in dependency " + pack.name());
        }
        return new Result(own, effective);
    }

    private static void merge(TableSchema table, Map<Value, VStruct> target, List<VStruct> rows) {
        var seen = new HashSet<Value>();
        for (VStruct row : rows) {
            Value key = ValueUtil.extractPrimaryKeyValue(row, table);
            if (!seen.add(key)) throw new ConfigErr("Duplicate primary key in package table " + table.name() + ": " + key);
            target.put(key, row);
        }
    }

    private static VStruct readStruct(Structural schema, ConfigInput input, Source source) {
        var fields = new ArrayList<Value>();
        for (FieldSchema field : schema.fields()) fields.add(read(field.type(), input, source));
        return new VStruct(schema, fields, source);
    }

    private static Value read(FieldType type, ConfigInput input, Source source) {
        return switch (type) {
            case FieldType.Primitive primitive -> switch (primitive) {
                case BOOL -> new VBool(input.readBool(), source);
                case INT -> new VInt(input.readInt(), source);
                case LONG -> new VLong(input.readLong(), source);
                case FLOAT -> new VFloat(input.readFloat(), source);
                case STRING -> new VString(input.readStringInPool(), source);
                case TEXT -> new VText(input.readTextInPool(), source);
            };
            case FieldType.StructRef ref -> {
                if (ref.obj() instanceof InterfaceSchema iface) {
                    String name = input.readStringInPool();
                    StructSchema impl = iface.impls().stream().filter(child -> child.name().equals(name)).findFirst()
                            .orElseThrow(() -> new ConfigErr("Unknown union implementation: " + name));
                    yield new VInterface(iface, readStruct(impl, input, source), source);
                }
                yield readStruct((Structural) ref.obj(), input, source);
            }
            case FieldType.FList list -> {
                int count = input.readInt();
                if (count < 0) throw new ConfigErr("Negative list count");
                var values = new ArrayList<SimpleValue>();
                for (int i = 0; i < count; i++) values.add((SimpleValue) read(list.item(), input, source));
                yield new VList(values, source);
            }
            case FieldType.FMap map -> {
                int count = input.readInt();
                if (count < 0) throw new ConfigErr("Negative map count");
                var values = new LinkedHashMap<SimpleValue, SimpleValue>();
                for (int i = 0; i < count; i++) {
                    SimpleValue key = (SimpleValue) read(map.key(), input, source);
                    SimpleValue value = (SimpleValue) read(map.value(), input, source);
                    if (values.putIfAbsent(key, value) != null) throw new ConfigErr("Duplicate map key");
                }
                yield new VMap(values, source);
            }
        };
    }
}
