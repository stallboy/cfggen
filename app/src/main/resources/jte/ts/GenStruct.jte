@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gents.StructModel
@param StructModel model
using System;
using System.Collections.Generic;


!{String className = model.className(model.structural);}
!{InterfaceSchema nullableInterface = model.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
!{boolean isImpl = nullableInterface != null;}
@if(isImpl)
    class ${className} extends ${model.className(nullableInterface)} {
        !{var enumRefTable = nullableInterface.nullableEnumRefTable();}
        @if(enumRefTable != null)
            type() : ${model.className(enumRefTable)} {
                return ${model.className(enumRefTable)}.${model.structural.name()};
            }

        @endif
@else
    class ${className} {
@endif

    <%--static enum--%>
    @if(model._vTable != null && model._vTable.schema().entry() instanceof EntryType.EntryBase)
        @for(String enumName : model._vTable.enumNames())
            private static _${enumName} : ${className};
            static get ${model.upper1(enumName)} :${className} { return this._${enumName}; }

        @endfor
    @endif

    <%--field property--%>
    @for(FieldSchema field : model.structural.fields())
        !{String t = model.type(field.type());}
        !{String n = field.name();}
        private _${n} : ${t} | undefined;
        @if(!field.comment().isEmpty())
            /* ${field.comment()} */
        @endif
        get ${model.upper1(n)} : ${t} { return this._${n} as ${t}; }
    @endfor


    <%--ref property--%>
    @for(ForeignKeySchema fk : model.structural.foreignKeys())
        !{String t = model.refType(fk);}
        !{String n = model.refName(fk);}
        private _${n} : ${t} | undefined;
        get ${n} : ${t} { return this._${n} as ${t}; }
    @endfor


    <%--table static Get,All,Initialize,Resolve--%>
    @if(model._vTable != null)
        !{TableSchema table = model._vTable.schema();}
        @template.cs.GenMapGetBy(model = model, keySchema = table.primaryKey(), isPrimaryKey = true)
        @for(KeySchema uk : table.uniqueKeys())
            @template.cs.GenMapGetBy(model = model, keySchema = uk, isPrimaryKey = false)
        @endfor

        static All() : Map<${model.mapKeyType(table.primaryKey())}, ${className}> {
            return all;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Map<${model.mapKeyType(table.primaryKey())}, ${className}>();
            @for(KeySchema uk : table.uniqueKeys())
                ${model.uniqueKeyMapName(uk)} = new Config.KeyedList<${model.mapKeyType(uk)}, ${className}>();
            @endfor

            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(${model.actualParamsKeySelf(table.primaryKey())}, self);
                @for(KeySchema uk : table.uniqueKeys())
                    ${model.uniqueKeyMapName(uk)}.Add(${model.actualParamsKeySelf(uk)}, self);
                @endfor

                @if(table.entry() instanceof EntryType.EntryBase entryBase)
                    !{String ef = model.upper1(entryBase.field());}
                    if (self.${ef}.Trim().Length == 0)
                        continue;
                    switch(self.${ef}.Trim())
                    {
                        @for(String enumName : model._vTable.enumNames())
                            case "${enumName}":
                                if (${model.upper1(enumName)} != null)
                                    errors.EnumDup("${model.structural.name()}", self.ToString());
                                ${model.upper1(enumName)} = self;
                                break;
                        @endfor
                        default:
                            errors.EnumDataAdd("${model.structural.name()}", self.ToString());
                            break;
                    }
                @endif
            }

            @if(table.entry() instanceof EntryType.EntryBase)
                @for(String enumName : model._vTable.enumNames())
                    if (${model.upper1(enumName)} == null)
                        errors.EnumNull("${model.structural.name()}", "${enumName}");
                @endfor
            @endif
        }

        @if(HasRef.hasRef(model.structural))
            internal static void Resolve(Config.LoadErrors errors)
            {
                foreach (var v in All())
                    v._resolve(errors);
            }
        @endif

    @endif

    <%--static create--%>
    internal ${isImpl ? "new " : ""}static ${className} _create(Config.Stream os)
    {
        var self = new ${className}();
        @for(FieldSchema field : model.structural.fields())
            !{String n = field.name();}
            !{FieldType t = field.type();}
            @if(t instanceof FList(SimpleType item))
                self.${model.upper1(n)} = new ${model.type(t)}();
                for (var c = os.ReadInt32(); c > 0; c--)
                    self.${model.upper1(n)}.Add(${model.create(item)});
            @elseif(field.type() instanceof FMap(SimpleType key, SimpleType value))
                self.${model.upper1(n)} = new ${model.type(t)}();
                for (var c = os.ReadInt32(); c > 0; c--)
                {
                    self.${model.upper1(n)}.Add(${model.create(key)}, ${model.create(value)});
                }
            @else
                self.${model.upper1(n)} = ${model.create(t)};
            @endif
        @endfor
        return self;
    }

    <%--resolve--%>
    @if(HasRef.hasRef(model.structural))
        internal ${isImpl ? "override " : ""}void _resolve(Config.LoadErrors errors)
        {
            @for(FieldSchema field : model.structural.fields())
                !{FieldType type = field.type();}
                @if(HasRef.hasRef(type))
                    @if(type instanceof StructRef)
                        ${model.upper1(field.name())}._resolve(errors);
                    @elseif(type instanceof FList)
                        foreach(var e in ${model.upper1(field.name())})
                            e._resolve(errors);
                    @elseif(type instanceof FMap)
                        foreach(var e in ${model.upper1(field.name())}.Values)
                            e._resolve(errors);
                    @endif
                @endif
            @endfor

            @for(ForeignKeySchema fk : model.structural.foreignKeys())
                @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                    !{var firstField = fk.key().fieldSchemas().getFirst();}
                    !{var refName = model.refName(fk);}
                    !{var fkStr = "\"" + fk.name() + "\"";}

                    @if(firstField.type() instanceof SimpleType)
                        ${refName} = ${model.tableGet(fk.refTableSchema(), refSimple, model.actualParams(fk.key()))};
                        @if(!refSimple.nullable())
                            if (${refName} == null) errors.RefNull("${model.structural.name()}", ToString(), ${fkStr});
                        @endif
                    @elseif(firstField.type() instanceof FList)
                        ${refName} = new ${model.refType(fk)}();
                        foreach(var e in ${model.upper1(firstField.name())})
                        {
                            var r = ${model.tableGet(fk.refTableSchema(), refSimple, "e")};
                            if (r == null) errors.RefNull("${model.structural.name()}", ToString(), ${fkStr});
                            ${refName}.Add(r);
                        }
                    @elseif(firstField.type() instanceof FMap)
                        ${refName} = new ${model.refType(fk)}();
                        foreach(var kv in ${model.upper1(firstField.name())}.Map)
                        {
                            var k = kv.Key;
                            var v = ${model.tableGet(fk.refTableSchema(), refSimple, "kv.Value")};
                            if (v == null) errors.RefNull("${model.structural.name()}", ToString(), ${fkStr});
                            ${refName}.Add(k, v);
                        }
                    @endif
                @endif
            @endfor

            @for(ForeignKeySchema fk : model.structural.foreignKeys())
                @if(fk.refKey() instanceof RefKey.RefList refList)
                    !{var refName = model.refName(fk);}
                    ${refName} = new List<${model.className(fk.refTableSchema())}>();
                    foreach (var v in ${model.className(fk.refTableSchema())}.All())
                    {
                        !{var eqs = new java.util.ArrayList<String>();}
                        @for(int i = 0; i < fk.key().fields().size(); i++)
                            !{eqs.add("v." + model.upper1(refList.keyNames().get(i)) + ".Equals(" + model.upper1(fk.key().fields().get(i)) + ")");}
                        @endfor
                        if (${String.join(" && ", eqs)})
                            ${refName}.Add(v);
                    }
                @endif
            @endfor
        }
    @endif
}
