@import configgen.gencs.ProcessorModel
@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gencs.StructModel
@import configgen.util.StringUtil
@param ProcessorModel model
@if(!model.implicitUsings)
    using System;
    using System.Collections.Generic;
@endif

${model.nsLine()}

public static class Processor
{
    @if(!model.languageLiterals.isEmpty())
        private static readonly string[] Languages = { ${model.languageLiterals} };
    @endif

    public static ConfigMgr LoadPackagesAndSetMgr(params ConfigPackage[] packages)
    {
        var config = LoadPackages(packages);
        ConfigMgr.SetMgr(config);
        return config;
    }

    public static ConfigMgr LoadPackages(params ConfigPackage[] packages)
    {
        ConfigPackage.ValidateOrder(packages);
        var errors = new LoadErrors();
        var config = new ConfigMgr();
        var loaders = NewLoaders(packages, config, errors);
        foreach (var pack in packages) {
            foreach (string block in pack.BlockNames) {
                if (block.StartsWith(".", StringComparison.Ordinal) && !loaders.ContainsKey(block.Substring(1))) {
                    errors.UnknownTableBlock(pack.Name, block);
                }
            }
        }
        errors.CheckErrors();
        foreach (var load in loaders.Values) {
            load();
        }
        errors.CheckErrors();
        @for(TableSchema table : model.tableSchemas)
            @if(HasRef.hasRef(table))
                foreach (var row in ${model.fullName(table)}.All(config)) {
                    row._resolve(errors, config);
                }
            @endif
        @endfor
        errors.CheckErrors();
        @if(!model.relations.isEmpty())
        ResolveRelations(config, errors);
        errors.CheckErrors();
        @endif
        @for(TableSchema table : model.tableSchemas)
            foreach (var row in ${model.fullName(table)}.All(config)) {
                Ready(row, config);
            }
        @endfor
        config.MarkReady();
        return config;
    }

    private static void Ready<T>(T value, ConfigMgr config)
    {
        if (value is IConfigExtension extension) {
            extension.OnConfigReady(config);
        }
    }

    private static Dictionary<string, Action> NewLoaders(ConfigPackage[] packages, ConfigMgr config, LoadErrors errors)
    {
        return new Dictionary<string, Action>(StringComparer.Ordinal) {
            @for(TableSchema table : model.tableSchemas)
                !{var sm = model.tableModel(table);}
                ["${table.name()}"] = () => ${model.fullName(table)}.InitializeAll(
                    ReadTable(packages, "${table.name()}", ${model.fullName(table)}._create,
                        ${model.fullName(table)}.PrimaryKey, config.${sm.tableField("Origins")}, errors), errors, config),
            @endfor
        };
    }

    @if(!model.relations.isEmpty())
@template.cs.Relations(model = model)
@template.cs.RelationResolver()
    @endif

    @for(Structural structural : model.readyStructs)
        private static void Ready(${model.fullName(structural)} value, ConfigMgr config)
        {
            @for(FieldSchema field : structural.fields())
                !{String fieldName = "value." + StringUtil.upper1(field.name());}
                @if(field.type() instanceof StructRef)
                    Ready(${fieldName}, config);
                @elseif(field.type() instanceof FList list && list.item() instanceof StructRef)
                    foreach (var item in ${fieldName}) {
                        Ready(item, config);
                    }
                @elseif(field.type() instanceof FMap map && (map.key() instanceof StructRef || map.value() instanceof StructRef))
                    foreach (var pair in ${fieldName}) {
                        @if(map.key() instanceof StructRef)
                            Ready(pair.Key, config);
                        @endif
                        @if(map.value() instanceof StructRef)
                            Ready(pair.Value, config);
                        @endif
                    }
                @endif
            @endfor
            Ready<${model.fullName(structural)}>(value, config);
        }

    @endfor
    @for(InterfaceSchema union : model.readyInterfaces)
        private static void Ready(${model.fullName(union)} value, ConfigMgr config)
        {
            !{var nested = union.impls().stream().filter(StructModel::hasNested).toList();}
            @if(nested.size() == 1)
                if (value is ${model.fullName(nested.getFirst())} item) {
                    Ready(item, config);
                }
                else {
                    Ready<${model.fullName(union)}>(value, config);
                }
            @else
                switch (value) {
                    @for(StructSchema impl : nested)
                        case ${model.fullName(impl)} item: Ready(item, config); return;
                    @endfor
                }
                Ready<${model.fullName(union)}>(value, config);
            @endif
        }

    @endfor
    private static List<T> ReadTable<K, T>(ConfigPackage[] packages, string table,
        Func<ConfigReader, T> readRow, Func<T, K> primaryKey, Dictionary<K, List<string>> origins,
        LoadErrors errors) where K : notnull
    {
        var rows = new List<T>();
        var slots = new Dictionary<K, int>();
        string block = "." + table;
        foreach (var pack in packages) {
            if (!pack.HasBlock(block)) {
                continue;
            }
            bool isBase = ReferenceEquals(pack, packages[0]);
            string packName = pack.Name;
            var input = pack.Block(block${model.languageLiterals.isEmpty() ? "" : ", Languages"}); input.SetErrors(errors);
            int count = input.ReadCount();
            for (int j = 0; j < count; j++) {
                var row = readRow(input);
                K key = primaryKey(row);
                bool exists = slots.TryGetValue(key, out var slot);
                bool duplicate = exists;
                if (!isBase) {
                    if (!origins.TryGetValue(key, out var chain)) {
                        chain = new List<string>();
                        origins.Add(key, chain);
                    }
                    duplicate = chain.Count != 0 && chain[chain.Count - 1] == packName;
                    if (!duplicate) {
                        chain.Add(packName);
                    }
                }
                if (duplicate) {
                    errors.DuplicateRecord(table, packName, key);
                    continue;
                }
                if (exists) {
                    rows[slot] = row;
                } else {
                    slots.Add(key, rows.Count);
                    rows.Add(row);
                }
            }
            input.RequireEnd();
        }
        return rows;
    }
}
@if(model.unity)
}
@endif
