@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gents.StructModel
@param StructModel model
!{String className = model.className(model.structural);}
!{InterfaceSchema nullableInterface = model.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
!{boolean isImpl = nullableInterface != null;}
@if(isImpl)
    export class ${className} extends ${model.className(nullableInterface)} {
        !{var enumRefTable = nullableInterface.nullableEnumRefTable();}
        @if(enumRefTable != null)
            type() : ${model.className(enumRefTable)} {
                return ${model.className(enumRefTable)}.${model.structural.name()};
            }

        @endif
@else
    export class ${className} {
@endif

    <%--static enum--%>
    @if(model._vTable != null && model._vTable.schema().entry() instanceof EntryType.EntryBase)
        @for(String enumName : model._vTable.enumNames())
            private static _${enumName} : ${className};
            static get ${model.upper1(enumName)}() :${className} { return this._${enumName}; }

        @endfor
    @endif

    <%--field property--%>
    @for(FieldSchema field : model.structural.fields())
        !{String t = model.type(field.type());}
        !{String n = field.name();}
        private _${n}: ${t} | undefined;
        @if(!field.comment().isEmpty())
            /* ${field.comment()} */
        @endif
        get ${model.upper1(n)}(): ${t} { return this._${n} as ${t}; }
    @endfor


    <%--ref property--%>
    @for(ForeignKeySchema fk : model.structural.foreignKeys())
        !{String t = model.refType(fk);}
        !{String n = model.refName(fk);}
        private _${n}: ${t} | undefined;
        get ${n}(): ${t} { return this._${n} as ${t}; }
    @endfor

    ToString() : string {
        return "(" + ${model.toStrings(model.structural.fields())} + ")";
    }

    <%--table static Get,All,Initialize,Resolve--%>
    @if(model._vTable != null)
        !{TableSchema table = model._vTable.schema();}
        @template.ts.GenMapGetBy(model = model, keySchema = table.primaryKey(), isPrimaryKey = true)
        @for(KeySchema uk : table.uniqueKeys())
            @template.ts.GenMapGetBy(model = model, keySchema = uk, isPrimaryKey = false)
        @endfor

        static All() : Map<${model.mapKeyType(table.primaryKey())}, ${className}> {
            return this.all;
        }

        static Initialize(os: Stream, errors: LoadErrors) {
            this.all = new Map<${model.mapKeyType(table.primaryKey())}, ${className}>();
            @for(KeySchema uk : table.uniqueKeys())
                this.${model.uniqueKeyMapName(uk)} = new Map<${model.mapKeyType(uk)}, ${className}>();
            @endfor

            for (let c = os.ReadInt32(); c > 0; c--)
            {
                let self = this._create(os);
                this.all.set(${model.actualParamsKeySelf(table.primaryKey())}, self);
                @for(KeySchema uk : table.uniqueKeys())
                    this.${model.uniqueKeyMapName(uk)}.set(${model.actualParamsKeySelf(uk)}, self);
                @endfor

                @if(table.entry() instanceof EntryType.EntryBase entryBase)
                    !{String ef = entryBase.field();}
                    if (self._${ef}.trim().length === 0) {
                        continue;
                    }
                    switch(self._${ef}.trim()) {
                        @for(String enumName : model._vTable.enumNames())
                            case "${enumName}":
                                if (this._${enumName} != null)
                                    errors.EnumDup("${model.structural.name()}", "${enumName}");
                                this._${enumName} = self;
                                break;
                        @endfor
                        default:
                            errors.EnumDataAdd("${model.structural.name()}", self._${ef});
                            break;
                    }
                @endif
            }

            @if(table.entry() instanceof EntryType.EntryBase)
                @for(String enumName : model._vTable.enumNames())
                    if (this._${enumName} == null) {
                        errors.EnumNull("${model.structural.name()}", "${enumName}");
                    }
                @endfor
            @endif
        }

        @if(HasRef.hasRef(model.structural))
            static Resolve(errors: LoadErrors) {
                for (const v of this.all.values()) {
                    v._resolve(errors);
                }
            }
        @endif

    @endif

    <%--static create--%>
    static _create(os: Stream) : ${className} {
        const self = new ${className}();
        @for(FieldSchema field : model.structural.fields())
            !{String n = field.name();}
            !{FieldType t = field.type();}
            @if(t instanceof FList(SimpleType item))
                self._${(n)} = [];
                for (let c = os.ReadInt32(); c > 0; c--)
                    self._${n}.push(${model.create(item)});
            @elseif(field.type() instanceof FMap(SimpleType key, SimpleType value))
                self._${(n)}  = new ${model.type(t)}();
                for (let c = os.ReadInt32(); c > 0; c--) {
                    self._${n}.set(${model.create(key)}, ${model.create(value)});
                }
            @else
                self._${n} = ${model.create(t)};
            @endif
        @endfor
        return self;
    }

    <%--resolve--%>
    @if(HasRef.hasRef(model.structural))
        _resolve(errors: LoadErrors) {
            @for(FieldSchema field : model.structural.fields())
                !{String n = field.name();}
                !{FieldType t = field.type();}
                @if(HasRef.hasRef(t))
                    @if(t instanceof StructRef)
                        this._${n}._resolve(errors);
                    @elseif(t instanceof FList)
                        for (const e of this._${n}) {
                            e._resolve(errors);
                        }
                    @elseif(t instanceof FMap)
                        for (const e of this._${n}.values()) {
                            e._resolve(errors);
                        }
                    @endif
                @endif
            @endfor

            @for(ForeignKeySchema fk : model.structural.foreignKeys())
                @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                    !{var firstField = fk.key().fieldSchemas().getFirst();}
                    !{var refName = model.refName(fk);}

                    @if(firstField.type() instanceof SimpleType)
                        this._${refName} = ${model.tableGet(fk.refTableSchema(), refSimple, model.actualParamsKeyThis(fk.key()))};
                        @if(!refSimple.nullable())
                            if (this._${refName} === undefined) {
                                errors.RefNull("${model.structural.name()}", this.ToString(), "${fk.name()}");
                            }
                        @endif
                    @elseif(firstField.type() instanceof FList)
                        this._${refName} = [];
                        for (const e of this._${firstField.name()}) {
                            const r = ${model.tableGet(fk.refTableSchema(), refSimple, "e")};
                            if (r === undefined) {
                                errors.RefNull("${model.structural.name()}", this.ToString(), "${fk.name()}");
                            }
                            this._${refName}.push(r);
                        }
                    @elseif(firstField.type() instanceof FMap)
                        this._${refName} = new ${model.refType(fk)}();
                        for (const e of this._${firstField.name()}.entries()) {
                            const v = ${model.tableGet(fk.refTableSchema(), refSimple, "e[1]")};
                            if (v === undefined) {
                                errors.RefNull("${model.structural.name()}", this.ToString(), "${fk.name()}");
                            }
                            this._${refName}.set(e[0], v);
                        }
                    @endif
                @endif
            @endfor

            @for(ForeignKeySchema fk : model.structural.foreignKeys())
                @if(fk.refKey() instanceof RefKey.RefList refList)
                    !{var refName = model.refName(fk);}
                    this._${refName} = [];
                    for (const v of ${model.className(fk.refTableSchema())}.All().values())
                    {
                        !{var eqs = new java.util.ArrayList<String>();}
                        @for(int i = 0; i < fk.key().fields().size(); i++)
                            !{eqs.add("v." + model.upper1(refList.keyNames().get(i)) + " === this._" + (fk.key().fields().get(i)));}
                        @endfor
                        if (${String.join(" && ", eqs)})
                            this._${refName}.push(v);
                    }
                @endif
            @endfor
        }
    @endif
}
