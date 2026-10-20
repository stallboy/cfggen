@import configgen.gencs.ModuleModel
@import configgen.gencs.ModuleModel.NamespaceGroup
@import configgen.gencs.StructModel
@import configgen.gencs.InterfaceModel
@import configgen.schema.*
@import configgen.schema.FieldType.*
@import java.util.List
@param ModuleModel model
@if(!model.implicitUsings)
    @if(model.usesArray())
        using System;
    @endif
    @if(model.usesCollections())
        using System.Collections.Generic;
    @endif
@endif

@for(NamespaceGroup group : model.groups())

namespace ${group.ns}
{
    @for(StructModel sm : group.structs)
        !{String className = sm.name.className + (sm.isEnum() ? "Info" : "");}
        !{InterfaceSchema nullableInterface = sm.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
        !{boolean isImpl = nullableInterface != null;}

        public partial class ${className}
        {
            @if(sm._vTable != null)
                !{TableSchema table = sm._vTable.schema();}
                !{boolean primarySeq = sm.isSeqKey(table.primaryKey());}
                !{boolean hasSymbols = table.entry() instanceof EntryType.EntryBase;}
                !{String symbolField = hasSymbols ? ((EntryType.EntryBase)table.entry()).field() : "";}
                !{boolean fixedId = sm.isEnum() && table.meta().getEnumValues() instanceof Metadata.MetaEnumValues.OfAssigned && !table.primaryKey().fields().getFirst().equals("id");}
                @if(hasSymbols && !sm._vTable.enumNames().isEmpty() && !table.primaryKey().fields().getFirst().equals(symbolField))
                    private static readonly ${sm.keyClassName(table.primaryKey())}[] _symbolKeys = { ${sm._vTable.enumNames().stream().map(sm::symbolKey).collect(java.util.stream.Collectors.joining(", "))} };
                @endif
                @if(fixedId && !sm._vTable.enumNames().isEmpty())
                    private static readonly int[] _symbolIds = { ${sm._vTable.enumNames().stream().map(symbol -> table.meta().assignedEnumId(symbol).toString()).collect(java.util.stream.Collectors.joining(", "))} };
                @endif
                internal static ${sm.keyClassName(table.primaryKey())} PrimaryKey(${className} self) => ${sm.actualParamsKeySelf(table.primaryKey())};
                internal static void InitializeAll(List<${className}> list, LoadErrors errors, ConfigMgr config)
                {
                    int count = list.Count;
                    @if(primarySeq)
                        var s_all = new ${sm.dictValueType()}[count];
                    @else
                        var s_all = new ${sm.dictTypeWhenInit(table.primaryKey())}(count);
                    @endif
                    @for(KeySchema uk : table.uniqueKeys())
                        @if(sm.isSeqKey(uk))
                            var s${sm.uniqueKeyMapName(uk)} = new ${sm.dictValueType()}[count];
                        @else
                            var s${sm.uniqueKeyMapName(uk)} = new ${sm.dictTypeWhenInit(uk)}(count);
                        @endif
                    @endfor
                    @for(FieldSchema field : table.fields())
                        @if(field.isSeq())
                            var seq_${field.name()} = new bool[count];
                        @endif
                    @endfor
                    foreach (var self in list) {
                        var key = PrimaryKey(self);
                        @for(FieldSchema field : table.fields())
                            @if(field.isSeq())
                                {
                                    int seq = self.${sm.upper1(field.name())};
                                    if (seq < 0 || seq >= count || seq_${field.name()}[seq]) {
                                        errors.InvalidSeq("${table.name()}", "${field.name()}", key, seq, count, config.Explain("${table.name()}", key));
                                    }
                                    else {
                                        seq_${field.name()}[seq] = true;
                                    }
                                }
                            @endif
                        @endfor
                        @if(primarySeq)
                            if (key >= 0 && key < count) {
                                s_all[key] = self;
                            }
                        @else
                            s_all.Add(key, self);
                        @endif
                        @for(KeySchema uk : table.uniqueKeys())
                            @if(sm.isSeqKey(uk))
                                {
                                    int index = ${sm.actualParamsKeySelf(uk)};
                                    if (index >= 0 && index < count) {
                                        s${sm.uniqueKeyMapName(uk)}[index] = self;
                                    }
                                }
                            @else
                                {
                                    var uniqueKey = ${sm.actualParamsKeySelf(uk)};
                                    if (!s${sm.uniqueKeyMapName(uk)}.TryAdd(uniqueKey, self)) {
                                        errors.DuplicateUniqueKey("${table.name()}", "${String.join(",", uk.fields())}", key, uniqueKey, config.Explain("${table.name()}", key));
                                    }
                                }
                            @endif
                        @endfor
                        @if(hasSymbols)
                            string name = self.${sm.upper1(symbolField)};
                            @if(sm._vTable.enumNames().isEmpty())
                                @if(sm.isEnum())
                                    errors.UnknownSymbol("${table.name()}", name, key, config.Explain("${table.name()}", key));
                                @else
                                    if (name.Length != 0) {
                                        errors.UnknownSymbol("${table.name()}", name, key, config.Explain("${table.name()}", key));
                                    }
                                @endif
                            @else
                                @if(sm.isEnum())
                                    int symbol = (int)self.EEnum;
                                @else
                                    if (name.Length == 0) {
                                        continue;
                                    }
                                    @if(sm._vTable.enumNames().size() == 1)
                                        int symbol = name == "${sm._vTable.enumNames().getFirst()}" ? 0 : -1;
                                    @else
                                        int symbol = name switch {
                                            !{int index = 0;}
                                            @for(String enumName : sm._vTable.enumNames())
                                                "${enumName}" => ${index++},
                                            @endfor
                                            _ => -1
                                        };
                                    @endif
                                @endif
                                if (symbol < 0) {
                                    errors.UnknownSymbol("${table.name()}", name, key, config.Explain("${table.name()}", key));
                                }
                                @if(!table.primaryKey().fields().getFirst().equals(symbolField) || fixedId)
                                else {
                                    @if(!table.primaryKey().fields().getFirst().equals(symbolField))
                                        if (!key.Equals(_symbolKeys[symbol])) {
                                            errors.ChangedSymbolId("${table.name()}", name, _symbolKeys[symbol], key, config.Explain("${table.name()}", key));
                                        }
                                    @endif
                                    @if(fixedId)
                                        if (self.Id != _symbolIds[symbol]) {
                                            errors.ChangedSymbolId("${table.name()}", name, _symbolIds[symbol], self.Id, config.Explain("${table.name()}", key));
                                        }
                                    @endif
                                }
                                @endif
                            @endif
                        @endif
                    }
                    config.${sm.indexField(table.primaryKey())} = ${primarySeq ? "s_all" : "new System.Collections.ObjectModel.ReadOnlyDictionary<" + sm.keyClassName(table.primaryKey()) + ", " + className + ">(s_all)"};
                    @for(KeySchema uk : table.uniqueKeys())
                        !{String index = "s" + sm.uniqueKeyMapName(uk);}
                        config.${sm.indexField(uk)} = ${sm.isSeqKey(uk) ? index : "new System.Collections.ObjectModel.ReadOnlyDictionary<" + sm.keyClassName(uk) + ", " + className + ">(" + index + ")"};
                    @endfor
                    @if(hasSymbols)
                        @for(String enumName : sm._vTable.enumNames())
                            !{String slot = sm.isEnum() ? sm.tableField("Symbols") + "[(int)" + sm.name.className + "." + sm.upper1(enumName) + "]" : sm.tableField("Symbol_" + sm.upper1(enumName));}
                            config.${slot} = Get(config, ${sm.symbolKey(enumName)});
                            if (config.${slot} == null) {
                                errors.MissingSymbol("${table.name()}", "${enumName}", ${sm.symbolKey(enumName)});
                            }
                        @endfor
                    @endif
                    config.${sm.tableField("Rows")} = ${primarySeq ? "Array.AsReadOnly(s_all)" : "list.AsReadOnly()"};
                }

            @endif

            internal static ${className} _create(ConfigReader reader)
            {
                @for(FieldSchema field : sm.structural.fields())
                    !{String n = "value" + sm.upper1(field.name());}
                    !{FieldType t = field.type();}
                    @if(t instanceof FList(SimpleType item))
                        int Count_${n} = reader.ReadCount(0);
                        ${sm.type(t)} ${n};
                        if (Count_${n} == 0) {
                            ${n} = Array.Empty<${sm.type(item)}>();
                        }
                        else {
                            var values_${n} = new List<${sm.type(item)}>(Count_${n});
                            for (int i = 0; i < Count_${n}; i++) {
                                values_${n}.Add(${sm.create(item)});
                            }
                            ${n} = values_${n}.AsReadOnly();
                        }
                    @elseif(t instanceof FMap(SimpleType key, SimpleType value))
                        int Count_${n} = reader.ReadCount(0);
                        var ${n} = Count_${n} == 0 ? ${sm.mutableType(t)}.Empty : new ${sm.mutableType(t)}(Count_${n});
                        for (int i = 0; i < Count_${n}; i++) {
                            var key = ${sm.create(key)};
                            var value = ${sm.create(value)};
                            if (!${n}.TryAdd(key, value)) {
                                reader.Errors.DuplicateKey("${sm.structural.name()}.${field.name()}", key);
                            }
                        }
                    @else
                        var ${n} = ${sm.create(t)};
                    @endif
                @endfor
                @if(sm.structural.fields().isEmpty())
                    return new ${className}();
                @else
                    return new ${className} {
                        @for(FieldSchema field : sm.structural.fields())
                            ${sm.upper1(field.name())} = ${"value" + sm.upper1(field.name())},
                        @endfor
                        @if(sm.isEnum())
                            @if(sm._vTable.enumNames().isEmpty())
                                EEnum = (${sm.name.className})(-1)
                            @elseif(sm._vTable.enumNames().size() == 1)
                                    EEnum = ${"value" + sm.upper1(((EntryType.EEnum)(sm._vTable.schema().entry())).field())} == "${sm._vTable.enumNames().getFirst()}" ? ${sm.name.className}.${sm.upper1(sm._vTable.enumNames().getFirst())} : (${sm.name.className})(-1)
                                @else
                                EEnum = ${"value" + sm.upper1(((EntryType.EEnum)(sm._vTable.schema().entry())).field())} switch {
                                    @for(String enumName : sm._vTable.enumNames())
                                        "${enumName}" => ${sm.name.className}.${sm.upper1(enumName)},
                                    @endfor
                                    _ => (${sm.name.className})(-1)
                                }
                            @endif
                        @endif
                    };
                @endif
            }

            !{boolean hasFields = !sm.structural.fields().isEmpty();}

            <%--hash, equal, toString--%>
            @if(!hasFields)
                public override int GetHashCode()
                {
                    return this.GetType().GetHashCode();
                }

                public override bool Equals(object? obj)
                {
                    return obj is ${className};
                }

            @else
                !{List<FieldSchema> keys = sm._vTable != null ? sm._vTable.schema().primaryKey().fieldSchemas() : sm.structural.fields();}
                public override int GetHashCode()
                {
                    return ${sm.hashCodes(keys)};
                }

                public override bool Equals(object? obj)
                {
                    return obj is ${className} o && ${sm.equals(keys)};
                }

                public override string ToString()
                {
                    return "(" + ${sm.toStrings(sm.structural.fields())} + ")";
                }

            @endif

            @if(HasRef.hasRef(sm.structural))
                ${isImpl ? "public" : "internal"} void _resolve(LoadErrors errors, ConfigMgr config)
                {
                    @for(FieldSchema field : sm.structural.fields())
                        !{FieldType type = field.type();}
                        @if(type instanceof StructRef && HasRef.hasRef(type))
                            ${sm.upper1(field.name())}._resolve(errors, config);
                        @elseif(type instanceof FList list && HasRef.hasRef(list.item()))
                            foreach(var e in ${sm.upper1(field.name())}) {
                                e._resolve(errors, config);
                            }
                        @elseif(type instanceof FMap map)
                            @if(HasRef.hasRef(map.key()))
                                foreach(var e in ${sm.upper1(field.name())}.Keys) {
                                    e._resolve(errors, config);
                                }
                            @endif
                            @if(HasRef.hasRef(map.value()))
                                foreach(var e in ${sm.upper1(field.name())}.Values) {
                                    e._resolve(errors, config);
                                }
                            @endif
                        @endif
                    @endfor

                    @for(ForeignKeySchema fk : sm.structural.foreignKeys())
                        @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                            !{var firstField = fk.key().fieldSchemas().getFirst();}
                            !{var refName = sm.refName(fk);}
                            !{var fkStr = fk.name();}

                            @if(firstField.type() instanceof SimpleType smpl)
                                @if (refSimple.nullable())
                                    ${refName} = ${sm.tableGet(fk.refTableSchema(), refSimple, sm.actualParams(fk.key()))}${StructModel.isEnum(fk.refTableSchema())? "?.EEnum" : ""};
                                @else
                                    var r${refName} = ${sm.tableGet(fk.refTableSchema(), refSimple, sm.actualParams(fk.key()))};
                                    if (r${refName} == null) {
                                        errors.MissingReference("${sm.structural.name()}", "${fkStr}", "${fk.refTableSchema().name()}", ${sm.referenceKey(fk.key())});
                                    }
                                    else {
                                        ${refName} = r${refName}${StructModel.isEnum(fk.refTableSchema())? ".EEnum" : ""};
                                    }
                                @endif
                            @elseif(firstField.type() instanceof FList flist)
                                var resolved${refName} = ${sm.refAssignExpr(fk)};
                                foreach(var e in ${sm.upper1(firstField.name())}) {
                                    var r = ${sm.tableGet(fk.refTableSchema(), refSimple, "e")};
                                    if (r == null) {
                                        errors.MissingReference("${sm.structural.name()}", "${fkStr}", "${fk.refTableSchema().name()}", e);
                                    }
                                    else {
                                        resolved${refName}.Add(r${StructModel.isEnum(fk.refTableSchema())? ".EEnum" : ""});
                                    }
                                }
                                ${refName} = ${sm.freezeRef(fk, "resolved" + refName)};
                            @elseif(firstField.type() instanceof FMap fmap)
                                var resolved${refName} = ${sm.refAssignExpr(fk)};
                                foreach(var kv in ${sm.upper1(firstField.name())}) {
                                    var k = kv.Key;
                                    var v = ${sm.tableGet(fk.refTableSchema(), refSimple, "kv.Value")};
                                    if (v == null) {
                                        errors.MissingReference("${sm.structural.name()}", "${fkStr}", "${fk.refTableSchema().name()}", kv.Value);
                                    }
                                    else {
                                        resolved${refName}.TryAdd(k, v${StructModel.isEnum(fk.refTableSchema())? ".EEnum" : ""});
                                    }
                                }
                                ${refName} = ${sm.freezeRef(fk, "resolved" + refName)};
                            @endif
                        @endif
                    @endfor

                    @for(ForeignKeySchema fk : sm.structural.foreignKeys())
                        @if(fk.refKey() instanceof RefKey.RefList refList)
                            !{var refName = sm.refName(fk);}
                            var resolved${refName} = ${sm.refAssignExpr(fk)};
                            foreach (var v in ${sm.tableDataName(fk.refTableSchema())}.All(config)) {
                                !{var eqs = new java.util.ArrayList<String>();}
                                @for(int i = 0; i < fk.key().fields().size(); i++)
                                    !{eqs.add("v." + sm.upper1(refList.keyNames().get(i)) + ".Equals(" + sm.upper1(fk.key().fields().get(i)) + ")");}
                                @endfor
                                if (${String.join(" && ", eqs)}) {
                                    resolved${refName}.Add(v${StructModel.isEnum(fk.refTableSchema()) ? ".EEnum" : ""});
                                }
                            }
                            ${refName} = ${sm.freezeRef(fk, "resolved" + refName)};
                        @endif
                    @endfor
                }
            @endif


        }

    @endfor

    @for(InterfaceModel im : group.interfaces)
        !{String className = im.name.className;}

        public partial interface ${className}
        {
            @if(HasRef.hasRef(im.sInterface))
                void _resolve(LoadErrors errors, ConfigMgr config) { }
            @endif

            internal static ${className} _create(ConfigReader reader)
            {
                @if(im.sInterface.impls().isEmpty())
                    throw reader.Errors.UnknownVariant("${im.sInterface.name()}", reader.ReadStringInPool());
                @elseif(im.sInterface.impls().size() == 1)
                    !{var impl = im.sInterface.impls().getFirst();}
                    var impl = reader.ReadStringInPool();
                    return impl == "${impl.name()}" ? ${im.fullName(impl)}._create(reader) : throw reader.Errors.UnknownVariant("${im.sInterface.name()}", impl);
                @else
                    return reader.ReadStringInPool() switch {
                        @for(StructSchema implSchema : im.sInterface.impls())
                            "${implSchema.name()}" => ${im.fullName(implSchema)}._create(reader),
                        @endfor
                        var impl => throw reader.Errors.UnknownVariant("${im.sInterface.name()}", impl)
                    };
                @endif
            }
        }

    @endfor
}

@endfor
