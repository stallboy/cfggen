@import configgen.genjava.code.Name
@import configgen.genjava.code.StructuralClassModel
@import configgen.schema.*
@import java.util.Arrays
@import java.util.stream.Collectors
@param StructuralClassModel model
    !{
        TableSchema table = (TableSchema) model.structural;
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
    @if(primarySeq)
    public static java.util.List<${model.className}> all() {
        ${model.codeTopPkg}.ConfigMgr mgr = ${model.codeTopPkg}.ConfigMgr.getMgr();
        return mgr.${functionAllName}();
    }
    @else
    public static java.util.Collection<${model.className}> all() {
        ${model.codeTopPkg}.ConfigMgr mgr = ${model.codeTopPkg}.ConfigMgr.getMgr();
        return mgr.${functionAllName}();
    }
    @endif

    public static class _ConfigLoader implements ${model.codeTopPkg}.ConfigLoader {

        @Override
        public void createAll(${model.codeTopPkg}.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            @if(primarySeq)
                mgr.${primaryMapName} = new ${model.className}[c];
            @else
                mgr.${primaryMapName} = new java.util.LinkedHashMap<>(c);
            @endif
            @for(KeySchema uk : table.uniqueKeys())
                !{ boolean ukSeqInit = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq(); }
                @if(ukSeqInit)
                    mgr.${model.name.containerPrefix + Name.uniqueKeyMapName(uk)} = new ${model.className}[c];
                @else
                    mgr.${model.name.containerPrefix + Name.uniqueKeyMapName(uk)} = new java.util.LinkedHashMap<>(c);
                @endif
            @endfor
            for (; c > 0; c--) {
                ${model.className} self = ${model.className}._create(input);
                @if(primarySeq)
                    mgr.${primaryMapName}[${model.actualParamsKey(table.primaryKey(), "self.")}] = self;
                @else
                    mgr.${primaryMapName}.put(${model.actualParamsKey(table.primaryKey(), "self.")}, self);
                @endif
                @for(KeySchema uk : table.uniqueKeys())
                    !{ boolean ukSeq2 = uk.fieldSchemas().size() == 1 && uk.fieldSchemas().getFirst().isSeq(); }
                    @if(ukSeq2)
                        mgr.${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}[${model.actualParamsKey(uk, "self.")}] = self;
                    @else
                        mgr.${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}.put(${model.actualParamsKey(uk, "self.")}, self);
                    @endif
                @endfor
            }
        }

        @Override
        public void resolveAll(${model.codeTopPkg}.ConfigMgr mgr) {
            @if(model.hasRef && !model.isTableAndNeedBuilder)
                for (${model.className} e : mgr.${primaryMapName}${primarySeq ? "" : ".values()"}) {
                    e._resolve(mgr);
                }
            @else
                // no resolve
            @endif
        }

    }
