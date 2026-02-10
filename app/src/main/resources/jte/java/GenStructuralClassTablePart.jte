@import configgen.genjava.code.Name
@import configgen.genjava.code.StructuralClassModel
@import configgen.genjava.code.MethodStr
@import configgen.schema.*
@import configgen.util.StringUtil
@import java.util.Arrays
@import java.util.stream.Collectors
@param StructuralClassModel model
    !{
        TableSchema table = (TableSchema) model.structural;
    }
    @if(table.primaryKey().fields().size() > 1)
@template.java.GenKeyClass(keySchema = table.primaryKey())
    @endif@template.java.GenMapGetBy(keySchema = table.primaryKey(), name = model.name, isPrimaryKey = true, codeTopPkg = model.codeTopPkg, mapsInMgr = model.mapsInMgr)
    @for(KeySchema keySchema : table.uniqueKeys())
        @if(keySchema.fields().size() > 1)@template.java.GenKeyClass(keySchema = keySchema)
        @endif@template.java.GenMapGetBy(keySchema = keySchema, name = model.name, isPrimaryKey = false, codeTopPkg = model.codeTopPkg, mapsInMgr = model.mapsInMgr)
    @endfor
    !{
        String primaryMapName = model.name.containerPrefix  + "All";
        String functionAllName = "all" + Arrays.stream(model.structural.name().split("\\.")).map(StringUtil::upper1).collect(Collectors.joining());
        model.mapsInMgr.add(String.format("public java.util.Collection<%s> %s() { return %s.values(); }",
                model.name.fullName, functionAllName, primaryMapName));
    }
    public static java.util.Collection<${model.className}> all() {
        ${model.codeTopPkg}.ConfigMgr mgr = ${model.codeTopPkg}.ConfigMgr.getMgr();
        return mgr.${functionAllName}();
    }

    public static class _ConfigLoader implements ${model.codeTopPkg}.ConfigLoader {

        @Override
        public void createAll(${model.codeTopPkg}.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                ${model.className} self = ${model.className}._create(input);
                mgr.${primaryMapName}.put(${MethodStr.actualParamsKey(table.primaryKey(), "self.", null)}, self);
                @for(KeySchema uk : table.uniqueKeys())
                    mgr.${model.name.containerPrefix + Name.uniqueKeyMapName(uk)}.put(${MethodStr.actualParamsKey(uk, "self.", null)}, self);
                @endfor
            }
        }

        @Override
        public void resolveAll(${model.codeTopPkg}.ConfigMgr mgr) {
            @if(model.hasRef && !model.isTableAndNeedBuilder)
                for (${model.className} e : mgr.${primaryMapName}.values()) {
                    e._resolve(mgr);
                }
            @else
                // no resolve
            @endif
        }

    }
