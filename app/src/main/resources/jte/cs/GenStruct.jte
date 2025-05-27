@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gencs.StructModel
@import java.util.List
@param StructModel model
using System;
using System.Collections.Generic;

@if(!model.topPkg.equals("Config"))
using Config;
@endif

namespace ${model.name.pkg}
{
    !{String className = model.name.className;}
    !{InterfaceSchema nullableInterface = model.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
    !{boolean isImpl = nullableInterface != null;}
    @if(isImpl)
        public partial class ${className} : ${model.fullName(nullableInterface)}
        {
            !{var enumRefTable = nullableInterface.nullableEnumRefTable();}
            @if(enumRefTable != null)
                public override ${model.fullName(enumRefTable)} type() {
                    return ${model.fullName(enumRefTable)}.${model.structural.name()};
                }

            @endif
    @else
        public partial class ${className}
        {
    @endif

        <%--static enum--%>
        @if(model._vTable != null && model._vTable.schema().entry() instanceof EntryType.EntryBase)
            @for(String enumName : model._vTable.enumNames())
                public static ${className} ${model.upper1(enumName)} { get; private set; }
            @endfor
        @endif

        <%--field property--%>
        @for(FieldSchema field : model.structural.fields())
            public ${model.type(field.type())} ${model.upper1(field.name())} { get; private set; }@if(!field.comment().isEmpty()) /* ${field.comment()} */@endif
        @endfor

        <%--ref property--%>
        @for(ForeignKeySchema fk : model.structural.foreignKeys())
            public ${model.refType(fk)} ${model.refName(fk)} { get; private set; }
        @endfor


        <%--constructor--%>
        !{boolean hasFields = !model.structural.fields().isEmpty();}
        @if(model._vTable == null)
            public ${className}() {
            }

            @if(hasFields)
                public ${className}(${model.formalParams(model.structural.fields())}) {
                    @for(FieldSchema field : model.structural.fields())
                        this.${model.upper1(field.name())} = ${model.lower1(field.name())};
                    @endfor
                }

            @endif
        @endif

        <%--hash, equal, toString--%>
        @if(!hasFields)
            public override int GetHashCode()
            {
                return this.GetType().GetHashCode();
            }

            public override bool Equals(object obj)
            {
                if (obj == null) return false;
                if (obj == this) return true;
                var o = obj as ${className};
                return o != null;
            }

        @else
            !{List<FieldSchema> keys = model._vTable != null ? model._vTable.schema().primaryKey().fieldSchemas() : model.structural.fields();}
            public override int GetHashCode()
            {
                return ${model.hashCodes(keys)};
            }

            public override bool Equals(object obj)
            {
                if (obj == null) return false;
                if (obj == this) return true;
                var o = obj as ${className};
                return o != null && ${model.equals(keys)};
            }

            public override string ToString()
            {
                return "(" + ${model.toStrings(model.structural.fields())} + ")";
            }

        @endif

        <%--table static Get,All,Initialize,Resolve--%>
        @if(model._vTable != null)
            !{TableSchema table = model._vTable.schema();}
            @template.cs.GenMapGetBy(model = model, keySchema = table.primaryKey(), isPrimaryKey = true)
            @for(KeySchema uk : table.uniqueKeys())
                @template.cs.GenMapGetBy(model = model, keySchema = uk, isPrimaryKey = false)
            @endfor

            public static List<${className}> All()
            {
                return all.OrderedValues;
            }

            public static List<${className}> Filter(Predicate<${className}> predicate)
            {
                var r = new List<${className}>();
                foreach (var e in all.OrderedValues)
                {
                    if (predicate(e))
                        r.Add(e);
                }
                return r;
            }

            internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
            {
                all = new Config.KeyedList<${model.keyClassName(table.primaryKey())}, ${className}>();
                @for(KeySchema uk : table.uniqueKeys())
                    ${model.uniqueKeyMapName(uk)} = new Config.KeyedList<${model.keyClassName(uk)}, ${className}>();
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
                        ${refName} = new List<${model.fullName(fk.refTableSchema())}>();
                        foreach (var v in ${model.fullName(fk.refTableSchema())}.All())
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
}