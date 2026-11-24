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
        var seqIndexes = new java.util.LinkedHashMap<String, java.util.List<String>>();
        if (primarySeq) {
            seqIndexes.computeIfAbsent(table.primaryKey().fieldSchemas().getFirst().name(), ignored -> new java.util.ArrayList<>())
                    .add(primaryMapName);
        }
        for (KeySchema uk : table.uniqueKeys()) {
            if (uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq()) {
                seqIndexes.computeIfAbsent(uk.fieldSchemas().getFirst().name(), ignored -> new java.util.ArrayList<>())
                        .add(model.name.containerPrefix + Name.uniqueKeyMapName(uk));
            }
        }
    }
    public static java.util.${primarySeq ? "List" : "Collection"}<${model.className}> all() {
        ${model.codeTopPkg}.ConfigMgr mgr = ${model.codeTopPkg}.ConfigMgr.getMgr();
        return mgr.${functionAllName}();
    }

    @if(GenJavaUtil.needsIndex(table))
    public static void _index(${model.codeTopPkg}.ConfigMgr mgr, java.util.Map<${Name.keyClassName(model.cfg, table.primaryKey(), model.name)}, ${model.className}> rows, ${errorsClass} errors) {
        int c = rows.size();
        @if(!seqIndexes.isEmpty())
            boolean validSeq = true;
            @for(String index : seqIndexes.values().stream().flatMap(java.util.List::stream).toList())
                var ${index} = new ${model.className}[c];
            @endfor
        @endif
        @for(KeySchema uk : table.uniqueKeys())
            !{ boolean ukSeqInit = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq(); }
            @if(!ukSeqInit)
                var ${model.name.containerPrefix + Name.uniqueKeyMapName(uk)} = new java.util.LinkedHashMap<${Name.keyClassName(model.cfg, uk, model.name)}, ${model.className}>(c);
            @endif
        @endfor
        @for(FieldSchema field : table.fields())
            @if(field.isSeq() && !seqIndexes.containsKey(field.name()))
                var seq_${field.name()} = new boolean[c];
            @endif
        @endfor
        for (var self : rows.values()) {
            @for(FieldSchema field : table.fields())
                @if(field.isSeq())
                    !{
                        var indexes = seqIndexes.getOrDefault(field.name(), java.util.List.of());
                        String duplicate = indexes.isEmpty() ? "seq_" + field.name() + "[seq]" : indexes.getFirst() + "[seq] != null";
                    }
                    {
                        int seq = self.get${configgen.util.StringUtil.upper1(field.name())}();
                        @if(model.cfg.jspecify() && !indexes.isEmpty())
                            //noinspection ConstantValue
                        @endif
                        if (seq < 0 || seq >= c || ${duplicate}) {
                            var primaryKey = ${model.actualParamsKey(table.primaryKey(), "self.")};
                            errors.invalidSeq("${table.name()}", "${field.name()}", primaryKey, seq, c, mgr.explain("${table.name()}", primaryKey));
                            @if(!seqIndexes.isEmpty())
                                validSeq = false;
                            @endif
                        } else {
                            @if(indexes.isEmpty())
                                seq_${field.name()}[seq] = true;
                            @else
                                @for(String index : indexes)
                                    ${index}[seq] = self;
                                @endfor
                            @endif
                        }
                    }
                @endif
            @endfor
            @for(KeySchema uk : table.uniqueKeys())
                !{ boolean ukSeq = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq(); }
                @if(!ukSeq)
                    if (${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}.putIfAbsent(${model.actualParamsKey(uk, "self.")}, self) != null) {
                        var primaryKey = ${model.actualParamsKey(table.primaryKey(), "self.")};
                        errors.duplicateUniqueKey("${table.name()}", "${String.join(",", uk.fields())}", primaryKey, ${model.actualParamsKey(uk, "self.")}, mgr.explain("${table.name()}", primaryKey));
                    }
                @endif
            @endfor
        }
        @if(!seqIndexes.isEmpty())
            if (!validSeq) {
                return;
            }
        @endif
        mgr._set_${primaryMapName}(${primarySeq ? "java.util.Arrays.asList(" + primaryMapName + ")" : "rows"});
        @for(KeySchema uk : table.uniqueKeys())
            !{
                String ukMapName = model.name.containerPrefix + Name.uniqueKeyMapName(uk);
                boolean ukSeq = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq();
            }
            mgr._set_${ukMapName}(${ukSeq ? "java.util.Arrays.asList(" + ukMapName + ")" : ukMapName});
        @endfor
    }
@endif
