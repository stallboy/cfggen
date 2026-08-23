@import configgen.genjava.GenJavaUtil
@import configgen.genjava.code.*
@import configgen.schema.*
@import configgen.util.StringUtil
@param configgen.genjava.code.GenCfg cfg
@param Structural structural
public void _resolveDirect(${cfg.codeTopPkg()}.ConfigMgr mgr) {
        @for(ForeignKeySchema fk : structural.foreignKeys())
            @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                !{
                    FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                    String refName = Name.refName(fk);
                    TableSchema refTable = fk.refTableSchema();
                }
                @if(firstField.type() instanceof FieldType.SimpleType)
                    ${refName} = ${MethodStr.tableGet(cfg, refTable, refSimple, MethodStr.actualParams(fk.key().fields()))};
                    @if(!refSimple.nullable())
                        configgen.genjava.LoadValueErrs.requireNonNull(${refName}, "${structural.name()}.${fk.name()} -> ${refTable.name()}", ${MethodStr.keyDisplayExpr(fk.key())});
                    @endif
                @elseif(firstField.type() instanceof FieldType.FList fList)
                    !{
                        String firstFieldName = StringUtil.lower1(firstField.name());
                    }
                    if (${firstFieldName}.isEmpty()) {
                        ${refName} = java.util.Collections.emptyList();
                    } else {
                        ${refName} = new java.util.ArrayList<>(${firstFieldName}.size());
                        for (${TypeStr.boxType(cfg, fList.item())} e : ${firstFieldName}) {
                            ${Name.refType(cfg, refTable)} r = ${MethodStr.tableGet(cfg, refTable, refSimple, "e")};
                            configgen.genjava.LoadValueErrs.requireNonNull(r, "${structural.name()}.${fk.name()} -> ${refTable.name()}", e);
                            ${refName}.add(r);
                        }
                    }
                @elseif(firstField.type() instanceof FieldType.FMap fMap)
                    !{
                        String firstFieldName = StringUtil.lower1(firstField.name());
                    }
                    if (${firstFieldName}.isEmpty()) {
                        ${refName} = java.util.Collections.emptyMap();
                    } else {
                        ${refName} = new java.util.LinkedHashMap<>(${firstFieldName}.size());
                        for (java.util.Map.Entry<${TypeStr.boxType(cfg, fMap.key())}, ${TypeStr.boxType(cfg, fMap.value())}> e : ${firstFieldName}.entrySet()) {
                            ${Name.refType(cfg, refTable)} rv = ${MethodStr.tableGet(cfg, refTable, refSimple, "e.getValue()")};
                            configgen.genjava.LoadValueErrs.requireNonNull(rv, "${structural.name()}.${fk.name()} -> ${refTable.name()}", e.getValue());
                            ${refName}.put(e.getKey(), rv);
                        }
                    }
                @endif
            @elseif(fk.refKey() instanceof RefKey.RefList refList)
                !{
                    String refName = Name.refName(fk);
                    TableSchema refTable = fk.refTableSchema();
                    NameableName refN = new NameableName(cfg, refTable);
                    boolean isEnumAndNoDetail = GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(refTable);
                    boolean isEnum = refTable.entry() instanceof EntryType.EEnum && !isEnumAndNoDetail;
                }
                ${refName} = new java.util.ArrayList<>();
                @if(isEnumAndNoDetail)
                    for (${refN.fullName} v : ${refN.fullName}.values()) {
                @elseif(isEnum)
                    for (${refN.fullName} vv : ${refN.fullName}.values()) {
                        @if(refTable.primaryKey().fieldSchemas().size() == 1 && refTable.primaryKey().fieldSchemas().getFirst().isSeq())
                            ${Name.tableDataFullName(cfg, refTable)} v = mgr.${refN.containerPrefix}All[vv.get${StringUtil.upper1(refTable.primaryKey().fields().getFirst())}()];
                        @else
                            ${Name.tableDataFullName(cfg, refTable)} v = mgr.${refN.containerPrefix}All.get(vv.get${StringUtil.upper1(refTable.primaryKey().fields().getFirst())}());
                        @endif
                @else
                    @if(refTable.primaryKey().fieldSchemas().size() == 1 && refTable.primaryKey().fieldSchemas().getFirst().isSeq())
                        for (${Name.refType(cfg, refTable)} v : java.util.Arrays.asList(mgr.${refN.containerPrefix}All)) {
                    @else
                        for (${Name.refType(cfg, refTable)} v : mgr.${refN.containerPrefix}All.values()) {
                    @endif
                @endif
                    !{
                        java.util.List<String> eqs = new java.util.ArrayList<>();
                        for (int i = 0; i < fk.key().fields().size(); i++) {
                            FieldSchema k = fk.key().fieldSchemas().get(i);
                            String rk = refList.keyNames().get(i);
                            eqs.add(MethodStr.equal("v.get" + StringUtil.upper1(rk) + "()", StringUtil.lower1(k.name()), k.type()));
                        }
                    }
                    if (${String.join(" && ", eqs)})
                        @if(isEnumAndNoDetail)
                            ${refName}.add(v);
                        @elseif(isEnum)
                            ${refName}.add(vv);
                        @else
                            ${refName}.add(v);
                        @endif
                }
                ${refName} = ${refName}.isEmpty() ? java.util.Collections.emptyList() : new java.util.ArrayList<>(${refName});
            @endif
        @endfor
    }
