using System;
using System.Collections.Generic;

@if(!pkg.equals("Config")) {
using Config;
}

namespace @{pkg}
{
    public partial class @{className}@if(isImpl) { : @{interfaceName}}
    {
        @if(isTableEnumOrEntry) {
            @for(enumName : vTable.enumNames()) {
            public static @{className} @{enumNameUpper} { get; private set; }
            }
        }

        // field property
        @for(fieldSchema : structural.fields()) {
            public @{type(fieldSchema.type())} @{fieldName} { get; private set; }@if(!fieldSchema.comment().isEmpty()) { /* @{fieldSchema.comment()}*/}
        }

        // ref property
        @for(fk : structural.foreignKeys()) {
            public @{refType(fk)} @{refName(fk)} { get; private set; }
        }

        //constructor
        @if(table == null) {
        public @{className}() {
        }

        @if(!hasNoFields) {
        public @{className}(@formalParams) {
            @for(f : structural.fields()) {
            this.@{upper1(f.name())} = @{lower1(f.name())};
            }
        }
        }
        }

        @if(hasNoFields) {
        //hash
        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        //equal
        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as @{className};
            return o != null;
        }
        } else {
        //hash
        public override int GetHashCode()
        {
            return @{hashCodes};
        }

        //equal
        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as @{className};
            return o != null && @{equals};
        }

        //toString
        public override string ToString()
        {
            return "(" + @{toStrings} + ")";
        }
        }

        @if(table != null) {
            //static all
            public static List<@{className}> All()
            {
                return all.OrderedValues;
            }

            // static filter
            public static List<@{className}> Filter(Predicate<@{className}> predicate)
            {
                var r = new List<@{className}>();
                foreach (var e in all.OrderedValues)
                {
                    if (predicate(e))
                        r.Add(e);
                }
                return r;
            }

            //static initialize
            internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
            {
                all = new Config.KeyedList<@{keyClassName}>
                @for(uk : table.uniqueKeys()) {
                @{uniqueKeyMapName(uk)} = new Config.KeyedList<@{keyClassName(uk)}>
                }

                for (var c = os.ReadInt32(); c > 0; c--)
                {
                    var self = _create(os);
                    @for(uk : table.uniqueKeys()) {
                    @{uniqueKeyMapName(uk)}.Add(@{actualParamsKeySelf}, self);
                    }

                    @if(table.entry() instanceof EntryType.EntryBase) {
                    String ef = upper1("@{entryField}");
                    if (self.@{ef}.Trim().Length == 0)
                        continue;
                    switch(self.@{ef}.Trim())
                    {
                        @for(enumName : vTable.enumNames()) {
                        case "@{enumName}":
                            if (@{upper1(enumName)} != null)
                                errors.EnumDup("@{structural.name()}", self.ToString());
                            @{upper1(enumName)} = self;
                            break;
                        }
                        default:
                            errors.EnumDataAdd("@{structural.name()}", self.ToString());
                            break;
                    }
                    }
                }

                @if(table.entry() instanceof EntryType.EntryBase) {
                    @for(enumName : vTable.enumNames()) {
                    if (@{upper1(enumName)} == null)
                        errors.EnumNull("@{structural.name()}", "@{enumName}");
                    }
                }
            }

            //static resolve
            @if(HasRef.hasRef(structural)) {
            internal static void Resolve(Config.LoadErrors errors)
            {
                foreach (var v in All())
                    v._resolve(errors);
            }
            }
        }

        //static create
        @{pre} @{className} _create(Config.Stream os)
        {
            var self = new @{className}();
            @for(fieldSchema : structural.fields()) {
                String n = fieldSchema.name();
                FieldType t = fieldSchema.type();
                if (t instanceof FList(SimpleType item)) {
                    self.@{upper1(n)} = new @{type(t)}();
                    for (var c = os.ReadInt32(); c > 0; c--)
                        self.@{upper1(n)}.Add(@{_create(item)});
                } else if (t instanceof FMap(SimpleType key, SimpleType value)) {
                    self.@{upper1(n)} = new @{type(t)}();
                    for (var c = os.ReadInt32(); c > 0; c--)
                        self.@{upper1(n)}.Add(@{_create(key)}, @{_create(value)});
                } else {
                    self.@{upper1(n)} = @{_create(t)};
                }
            }
            return self;
        }

        //resolve
        @if(HasRef.hasRef(structural)) {
        @{preResolve}void _resolve(Config.LoadErrors errors)
        {
            // 1,先调用子_resolve
            @for(field : structural.fields()) {
                FieldType type = field.type();
                if (!HasRef.hasRef(type)) {
                    continue;
                }
                switch (type) {
                    case StructRef ignored -> {
                        @{upper1(field.name())}._resolve(errors);
                    }
                    case FList ignored -> {
                        foreach (var e in @{upper1(field.name())})
                        {
                            e._resolve(errors);
                        }
                    }
                    case FMap ignored -> {
                        foreach (var kv in @{upper1(field.name())}.Map)
                        {
                            kv.Value._resolve(errors);
                        }
                    }
                    case Primitive ignored -> {}
                }
            }

            // 2,处理本struct里的refSimple
            @for(fk : structural.foreignKeys()) {
                if (!(fk.refKey() instanceof RefKey.RefSimple refSimple)) {
                    continue;
                }
                FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                String refName = refName(fk);
                String fkStr = "@{fk.name()}";

                switch (firstField.type()) {
                    case SimpleType ignored -> {
                        @{refName} = @{tableGet};
                        if (!refSimple.nullable()) {
                            if (@{refName} == null) errors.RefNull("@{structural.name()}", ToString(), fkStr);
                        }
                    }
                    case FList ignored -> {
                        @{refName} = new @{refType(fk)}();
                        foreach (var e in @{upper1(firstField.name())})
                        {
                            var r = @{tableGet};
                            if (r == null) errors.RefNull("@{structural.name()}", ToString() , fkStr);
                            @{refName}.Add(r);
                        }
                    }
                    case FMap ignored -> {
                        @{refName} = new @{refType(fk)}();
                        foreach (var kv in @{upper1(firstField.name())}.Map)
                        {
                            var k = kv.Key;
                            var v = @{tableGet};
                            if (v == null) errors.RefNull("@{structural.name()}", ToString(), fkStr);
                            @{refName}.Add(k, v);
                        }
                    }
                }
            }

            // 3,处理本struct里的refList
            @for(fk : structural.foreignKeys()) {
                if (!(fk.refKey() instanceof RefKey.RefList refList)) {
                    continue;
                }
                @{refName(fk)} = new List<@{fullName(fk.refTableSchema())}>();
                foreach (var v in @{fullName(fk.refTableSchema())}.All())
                {
                    List<String> eqs = new ArrayList<>();
                    @for(i : range(fk.key().fields().size())) {
                    String k = fk.key().fields().get(@{i});
                    String rk = refList.keyNames().get(@{i});
                    eqs.add("v.@{upper1(rk)}.Equals(@{upper1(k)})");
                    }
                    if (@String.join(" && ", eqs)}) {
                        @{refName(fk)}.Add(v);
                    }
                }
            }
        }
        }
    }
}