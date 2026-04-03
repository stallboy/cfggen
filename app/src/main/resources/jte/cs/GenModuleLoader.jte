@import configgen.gencs.ModuleModel
@import configgen.gencs.ModuleModel.NamespaceGroup
@import configgen.gencs.StructModel
@import configgen.gencs.InterfaceModel
@import configgen.schema.*
@import configgen.schema.FieldType.*
@param ModuleModel model
!{String topPkg = model.topPkg;}
@for(NamespaceGroup group : model.groups())

namespace ${group.ns}
{
    @for(StructModel sm : group.structs)
        !{String className = sm.name.className;}
        !{InterfaceSchema nullableInterface = sm.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
        !{boolean isImpl = nullableInterface != null;}

        public partial class ${className}
        {
            @if(sm._vTable != null)
                !{TableSchema table = sm._vTable.schema();}
                internal static void Initialize(ConfigReader reader)
                {
                    _all = [];
                    @for(KeySchema uk : table.uniqueKeys())
                        ${sm.uniqueKeyMapName(uk)} = [];
                    @endfor

                    for (var c = reader.ReadInt32(); c > 0; c--)
                    {
                        var self = _create(reader);
                        _all.Add(${sm.actualParamsKeySelf(table.primaryKey())}, self);
                        @for(KeySchema uk : table.uniqueKeys())
                            ${sm.uniqueKeyMapName(uk)}.Add(${sm.actualParamsKeySelf(uk)}, self);
                        @endfor

                        @if(table.entry() instanceof EntryType.EntryBase entryBase)
                            !{String ef = sm.upper1(entryBase.field());}
                            if (self.${ef}.Trim().Length == 0)
                                continue;
                            switch(self.${ef}.Trim())
                            {
                                @for(String enumName : sm._vTable.enumNames())
                                    case "${enumName}":
                                        if (${sm.upper1(enumName)} != null)
                                            reader.EnumDuplicateInData("${enumName}");
                                        ${sm.upper1(enumName)} = self;
                                        break;
                                @endfor
                                default:
                                    reader.EnumNotInCode(self.${ef}.Trim());
                                    break;
                            }
                        @endif
                    }

                    @if(table.entry() instanceof EntryType.EntryBase)
                        @for(String enumName : sm._vTable.enumNames())
                            if (${sm.upper1(enumName)} == null)
                                reader.EnumNotInData("${enumName}");
                        @endfor
                    @endif
                }

                @if(HasRef.hasRef(sm.structural))
                    internal static void Resolve(ConfigReader reader)
                    {
                        foreach (var v in All())
                            v._resolve(reader);
                    }
                @endif

            @endif

            internal ${isImpl ? "new " : ""}static ${className} _create(ConfigReader reader)
            {
                @for(FieldSchema field : sm.structural.fields())
                    !{String n = StructModel._lower1(field.name());}
                    !{FieldType t = field.type();}
                    @if(t instanceof FList(SimpleType item))
                        ${sm.type(t)} ${n} = [];
                        for (var c = reader.ReadInt32(); c > 0; c--)
                            ${n}.Add(${sm.create(item)});
                    @elseif(field.type() instanceof FMap(SimpleType key, SimpleType value))
                        ${sm.type(t)} ${n} = [];
                        for (var c = reader.ReadInt32(); c > 0; c--)
                        {
                            ${n}.Add(${sm.create(key)}, ${sm.create(value)});
                        }
                    @else
                        var ${n} = ${sm.create(t)};
                    @endif
                @endfor
                return new ${className} {
                    @for(FieldSchema field : sm.structural.fields())
                        ${sm.upper1(field.name())} = ${StructModel._lower1(field.name())},
                    @endfor
                };
            }

        @if(HasRef.hasRef(sm.structural))
            internal void _resolve(ConfigReader reader)
            {
                @for(FieldSchema field : sm.structural.fields())
                    !{FieldType type = field.type();}
                    @if(HasRef.hasRef(type))
                        @if(type instanceof StructRef)
                            ${sm.upper1(field.name())}._resolve(reader);
                        @elseif(type instanceof FList)
                            foreach(var e in ${sm.upper1(field.name())})
                                e._resolve(reader);
                        @elseif(type instanceof FMap)
                            foreach(var e in ${sm.upper1(field.name())}.OrderedValues)
                                e._resolve(reader);
                        @endif
                    @endif
                @endfor

                @for(ForeignKeySchema fk : sm.structural.foreignKeys())
                    @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                        !{var firstField = fk.key().fieldSchemas().getFirst();}
                        !{var refName = sm.refName(fk);}
                        !{var fkStr = fk.name();}

                        @if(firstField.type() instanceof SimpleType)
                            @if (refSimple.nullable())
                                ${refName} = ${sm.tableGet(fk.refTableSchema(), refSimple, sm.actualParams(fk.key()))};
                            @else
                                ${refName} = ${sm.tableGet(fk.refTableSchema(), refSimple, sm.actualParams(fk.key()))}!;
                                if (${refName} == null) reader.RefNotFound("${sm.structural.name()}", "${fkStr}", ${model.upper1(fk.key().fields().getFirst())}.ToString());
                            @endif
                        @elseif(firstField.type() instanceof FList)
                            ${refName} = [];
                            foreach(var e in ${sm.upper1(firstField.name())})
                            {
                                var r = ${sm.tableGet(fk.refTableSchema(), refSimple, "e")};
                                if (r == null) reader.RefNotFound("${sm.structural.name()}", "${fkStr}", e.ToString());
                                else ${refName}.Add(r);
                            }
                        @elseif(firstField.type() instanceof FMap)
                            ${refName} = [];
                            foreach(var kv in ${sm.upper1(firstField.name())})
                            {
                                var k = kv.Key;
                                var v = ${sm.tableGet(fk.refTableSchema(), refSimple, "kv.Value")};
                                if (v == null) reader.RefNotFound("${sm.structural.name()}", "${fkStr}", kv.Value.ToString());
                                else ${refName}.Add(k, v);
                            }
                        @endif
                    @endif
                @endfor

                @for(ForeignKeySchema fk : sm.structural.foreignKeys())
                    @if(fk.refKey() instanceof RefKey.RefList refList)
                        !{var refName = sm.refName(fk);}
                        ${refName} = [];
                        foreach (var v in ${sm.fullName(fk.refTableSchema())}.All())
                        {
                            !{var eqs = new java.util.ArrayList<String>();}
                            @for(int i = 0; i < fk.key().fields().size(); i++)
                                !{eqs.add("v." + sm.upper1(refList.keyNames().get(i)) + ".Equals(" + sm.upper1(fk.key().fields().get(i)) + ")");}
                            @endfor
                            if (${String.join(" && ", eqs)})
                                ${refName}.Add(v);
                        }
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
            internal void _resolve(ConfigReader reader)
            {
            }
        @endif

        internal static ${className} _create(ConfigReader reader)
        {
            var impl = reader.ReadStringInPool();
            switch(impl)
            {
                @for(StructSchema implSchema : im.sInterface.impls())
                    case "${implSchema.name()}":
                        return ${im.fullName(implSchema)}._create(reader);
                @endfor
            }
            throw reader.NotFoundImpl(impl, "${im.sInterface.name()}");
        }
    }

@endfor
}

@endfor
