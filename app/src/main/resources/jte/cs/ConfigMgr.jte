@import configgen.gencs.ProcessorModel
@import configgen.schema.*
@param ProcessorModel model
@if(!model.implicitUsings)
    using System;
    using System.Collections.Generic;
@endif

namespace ${model.topPkg}
{
    public sealed class ConfigMgr
    {
        private static ConfigMgr? _default;
        private bool _ready;
        @for(TableSchema table : model.tableSchemas)
            !{var sm = model.tableModel(table);}
            !{var keys = new java.util.ArrayList<KeySchema>(); keys.add(table.primaryKey()); keys.addAll(table.uniqueKeys());}
            internal IReadOnlyList<${model.fullName(table)}> ${sm.tableField("Rows")} = null!;
            @for(KeySchema key : keys)
                @if(sm.isSeqKey(key))
                    internal ${model.fullName(table)}[] ${sm.indexField(key)} = null!;
                @else
                    internal IReadOnlyDictionary<${sm.qualifiedKeyType(key)}, ${model.fullName(table)}> ${sm.indexField(key)} = null!;
                @endif
            @endfor
            internal readonly Dictionary<${sm.qualifiedKeyType(table.primaryKey())}, List<string>> ${sm.tableField("Origins")} = new Dictionary<${sm.qualifiedKeyType(table.primaryKey())}, List<string>>();
            @if(table.entry() instanceof EntryType.EEnum)
                internal readonly ${model.fullName(table)}?[] ${sm.tableField("Symbols")} = new ${model.fullName(table)}?[${sm._vTable.enumNames().size()}];
            @elseif(table.entry() instanceof EntryType.EEntry)
                @for(String symbol : sm._vTable.enumNames())
                    internal ${model.fullName(table)}? ${sm.tableField("Symbol_" + sm.upper1(symbol))};
                @endfor
            @endif
        @endfor

        public IReadOnlyList<string> Explain<K>(string table, K key)
        @if(model.tableSchemas.isEmpty())
                => Array.Empty<string>();
        @elseif(model.tableSchemas.size() == 1)
            !{var table = model.tableSchemas.getFirst(); var sm = model.tableModel(table);}
                => table == "${table.name()}" ? Explain(${sm.tableField("Origins")}, key) : Array.Empty<string>();
        @else
                    => table switch {
            @for(TableSchema table : model.tableSchemas)
                !{var sm = model.tableModel(table);}
                            "${table.name()}" => Explain(${sm.tableField("Origins")}, key),
            @endfor
                        _ => Array.Empty<string>()
                    };
        @endif


        @if(!model.tableSchemas.isEmpty())
            private static IReadOnlyList<string> Explain<K, Q>(Dictionary<K, List<string>> origins, Q key) where K : notnull
                => key is K typedKey && origins.TryGetValue(typedKey, out var chain) ? chain.AsReadOnly() : Array.Empty<string>();
        @endif


        public static ConfigMgr GetMgr() => System.Threading.Volatile.Read(ref _default)
            ?? throw new InvalidOperationException("Default configuration has not been set");

        public static void SetMgr(ConfigMgr? config)
        {
            if (config == null || !config._ready) throw new InvalidOperationException("Configuration is not ready");
            System.Threading.Volatile.Write(ref _default, config);
        }

        internal void MarkReady() { _ready = true; }
    }
}
