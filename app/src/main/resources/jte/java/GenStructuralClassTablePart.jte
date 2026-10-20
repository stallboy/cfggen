@import configgen.genjava.code.Name
@import configgen.genjava.code.StructuralClassModel
@import configgen.genjava.GenJavaUtil
@import configgen.schema.*
@import java.util.Arrays
@import java.util.stream.Collectors
@param StructuralClassModel model
    !{
        TableSchema table = (TableSchema) model.structural;
        String errorsClass = GenJavaUtil.loadValueErrsClass(model.className);
        boolean primarySeq = table.primaryKey().fieldSchemas().size() == 1 && table.primaryKey().fieldSchemas().getFirst().isSeq();
    }
    @if(table.primaryKey().fields().size() > 1)
@template.java.GenKeyClass(cfg = model.cfg, keySchema = table.primaryKey())
    @endif@template.java.GenMapGetBy(keySchema = table.primaryKey(), name = model.name, isPrimaryKey = true)
    @for(KeySchema keySchema : table.uniqueKeys())
        @if(keySchema.fields().size() > 1)@template.java.GenKeyClass(cfg = model.cfg, keySchema = keySchema)
        @endif@template.java.GenMapGetBy(keySchema = keySchema, name = model.name, isPrimaryKey = false)
    @endfor
    !{
        String primaryMapName = model.name.containerPrefix + "All";
        String functionAllName = "all" + Arrays.stream(model.structural.name().split("\\.")).map(s -> Name.pascalName(model.cfg, s)).collect(Collectors.joining());
    }
    public static java.util.${primarySeq ? "List" : "Collection"}<${model.className}> all() {
        ${model.codeTopPkg}.ConfigMgr mgr = ${model.codeTopPkg}.ConfigMgr.getMgr();
        return mgr.${functionAllName}();
    }

    @if(GenJavaUtil.needsIndex(table))
    public static void _index(${model.codeTopPkg}.ConfigMgr mgr, java.util.Map<${Name.keyClassName(model.cfg, table.primaryKey(), model.name)}, ${model.className}> rows, ${errorsClass} errors) {
        int c = rows.size();
        @if(primarySeq)
            var ${primaryMapName} = new java.util.ArrayList<${model.className}>(java.util.Collections.nCopies(c, null));
        @endif
        @for(KeySchema uk : table.uniqueKeys())
            !{ boolean ukSeqInit = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq(); }
            @if(ukSeqInit)
                var ${model.name.containerPrefix + Name.uniqueKeyMapName(uk)} = new java.util.ArrayList<${model.className}>(java.util.Collections.nCopies(c, null));
            @else
                var ${model.name.containerPrefix + Name.uniqueKeyMapName(uk)} = new java.util.LinkedHashMap<${Name.keyClassName(model.cfg, uk, model.name)}, ${model.className}>(c);
            @endif
        @endfor
        @for(FieldSchema field : table.fields())
            @if(field.isSeq())
                var seq_${field.name()} = new boolean[c];
            @endif
        @endfor
        for (var self : rows.values()) {
            @for(FieldSchema field : table.fields())
                @if(field.isSeq())
                    {
                        int seq = self.get${configgen.util.StringUtil.upper1(field.name())}();
                        if (seq < 0 || seq >= c || seq_${field.name()}[seq]) {
                            var primaryKey = ${model.actualParamsKey(table.primaryKey(), "self.")};
                            errors.invalidSeq("${table.name()}", "${field.name()}", primaryKey, seq, c, mgr.explain("${table.name()}", primaryKey));
                        } else {
                            seq_${field.name()}[seq] = true;
                        }
                    }
                @endif
            @endfor
            @if(primarySeq)
                int key = ${model.actualParamsKey(table.primaryKey(), "self.")};
                if (key >= 0 && key < c) {
                    ${primaryMapName}.set(key, self);
                }
            @endif
            @for(KeySchema uk : table.uniqueKeys())
                !{ boolean ukSeq = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq(); }
                @if(ukSeq)
                    {
                        int uniqueKey = ${model.actualParamsKey(uk, "self.")};
                        if (uniqueKey >= 0 && uniqueKey < c) {
                            ${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}.set(uniqueKey, self);
                        }
                    }
                @else
                    if (${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}.putIfAbsent(${model.actualParamsKey(uk, "self.")}, self) != null) {
                        var primaryKey = ${model.actualParamsKey(table.primaryKey(), "self.")};
                        errors.duplicateUniqueKey("${table.name()}", "${String.join(",", uk.fields())}", primaryKey, ${model.actualParamsKey(uk, "self.")}, mgr.explain("${table.name()}", primaryKey));
                    }
                @endif
            @endfor
        }
        mgr._set_${primaryMapName}(${primarySeq ? primaryMapName : "rows"});
        @for(KeySchema uk : table.uniqueKeys())
            mgr._set_${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}(${model.name.containerPrefix + Name.uniqueKeyMapName(uk)});
        @endfor
    }
@endif
