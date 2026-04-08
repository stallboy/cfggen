@import configgen.gencs.ModuleModel
@import configgen.gencs.ModuleModel.NamespaceGroup
@import configgen.gencs.StructModel
@import configgen.gencs.InterfaceModel
@import configgen.schema.*
@import configgen.schema.FieldType.*
@import java.util.List
@param ModuleModel model
!{String topPkg = model.topPkg;}
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
                internal static void Initialize(ConfigReader reader)
                {
                    _all = [];
                    @for(KeySchema uk : table.uniqueKeys())
                        ${sm.uniqueKeyMapName(uk)} = [];
                    @endfor

                    @if(table.entry() instanceof EntryType.EEntry eEntry)
                        @for(String enumName : sm._vTable.enumNames())
                            ${className}? e${sm.upper1(enumName)} = null;
                        @endfor
                    @endif

                    for (var c = reader.ReadInt32(); c > 0; c--)
                    {
                        var self = _create(reader);
                        _all.Add(${sm.actualParamsKeySelf(table.primaryKey())}, self);
                        @for(KeySchema uk : table.uniqueKeys())
                            ${sm.uniqueKeyMapName(uk)}.Add(${sm.actualParamsKeySelf(uk)}, self);
                        @endfor
                        @if(sm.isEnum())
                            ${sm.name.className}Extensions._infos[(int)self.eEnum] = self;
                        @endif

                        @if(table.entry() instanceof EntryType.EEntry eEntry)
                            !{String ef = sm.upper1(eEntry.field());}
                            if (self.${ef}.Length == 0)
                                continue;
                            switch(self.${ef})
                            {
                                @for(String enumName : sm._vTable.enumNames())
                                    case "${enumName}":
                                        if (e${sm.upper1(enumName)} != null)
                                            reader.EnumDuplicateInData("${enumName}");
                                        e${sm.upper1(enumName)} = self;
                                        break;
                                @endfor
                                default:
                                    reader.EnumNotInCode(self.${ef});
                                    break;
                            }
                        @endif
                    }

                    @if(table.entry() instanceof EntryType.EEntry)
                        @for(String enumName : sm._vTable.enumNames())
                            if (e${sm.upper1(enumName)} == null) reader.EnumNotInData("${enumName}");
                            else ${sm.upper1(enumName)} = e${sm.upper1(enumName)};
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

            internal static ${className} _create(ConfigReader reader)
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
                @if(sm.structural.fields().isEmpty())
                    return new ${className}();
                @else
                    return new ${className} {
                        @for(FieldSchema field : sm.structural.fields())
                            ${sm.upper1(field.name())} = ${StructModel._lower1(field.name())},
                        @endfor
                        @if(sm.isEnum())
                            eEnum = Enum.Parse<${sm.name.className}>(StringUtil.UpperFirstChar(${StructModel._lower1(((EntryType.EEnum)(sm._vTable.schema().entry())).field())}))
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
                    if (obj == null) return false;
                    if (obj == this) return true;
                    var o = obj as ${className};
                    return o != null;
                }

            @else
                !{List<FieldSchema> keys = sm._vTable != null ? sm._vTable.schema().primaryKey().fieldSchemas() : sm.structural.fields();}
                public override int GetHashCode()
                {
                    return ${sm.hashCodes(keys)};
                }

                public override bool Equals(object? obj)
                {
                    if (obj == null) return false;
                    if (obj == this) return true;
                    var o = obj as ${className};
                    return o != null && ${sm.equals(keys)};
                }

                public override string ToString()
                {
                    return "(" + ${sm.toStrings(sm.structural.fields())} + ")";
                }

            @endif

            @if(HasRef.hasRef(sm.structural))
                ${isImpl ? "public" : "internal"} void _resolve(ConfigReader reader)
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

                            @if(firstField.type() instanceof SimpleType smpl)
                                @if (refSimple.nullable())
                                    ${refName} = ${sm.tableGet(fk.refTableSchema(), refSimple, sm.actualParams(fk.key()))};
                                @else
                                    var r${refName} = ${sm.tableGet(fk.refTableSchema(), refSimple, sm.actualParams(fk.key()))};
                                    if (r${refName} == null) reader.RefNotFound("${sm.structural.name()}", "${fkStr}", ${model.upper1(firstField.name())}${sm.toStringOrNot(smpl)});
                                    else ${refName} = r${refName}${StructModel.isEnum(fk.refTableSchema())? ".eEnum" : ""};
                                @endif
                            @elseif(firstField.type() instanceof FList flist)
                                ${refName} = [];
                                foreach(var e in ${sm.upper1(firstField.name())})
                                {
                                    var r = ${sm.tableGet(fk.refTableSchema(), refSimple, "e")};
                                    if (r == null) reader.RefNotFound("${sm.structural.name()}", "${fkStr}", e${sm.toStringOrNot(flist.item())});
                                    else ${refName}.Add(r${StructModel.isEnum(fk.refTableSchema())? ".eEnum" : ""});
                                }
                            @elseif(firstField.type() instanceof FMap fmap)
                                ${refName} = [];
                                foreach(var kv in ${sm.upper1(firstField.name())})
                                {
                                    var k = kv.Key;
                                    var v = ${sm.tableGet(fk.refTableSchema(), refSimple, "kv.Value")};
                                    if (v == null) reader.RefNotFound("${sm.structural.name()}", "${fkStr}", kv.Value${sm.toStringOrNot(fmap.value())});
                                    else ${refName}.Add(k, v${StructModel.isEnum(fk.refTableSchema())? ".eEnum" : ""});
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
                void _resolve(ConfigReader reader)
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
