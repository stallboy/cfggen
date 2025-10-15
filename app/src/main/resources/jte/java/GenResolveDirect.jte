@import configgen.gen.Generator
@import configgen.genjava.GenJavaUtil
@import configgen.genjava.code.*
@import configgen.schema.*
@param String codeTopPkg
@param Structural structural
public void _resolveDirect(${codeTopPkg}.ConfigMgr mgr) {
        @for(ForeignKeySchema fk : structural.foreignKeys())
            @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                !{
                    FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                    String refName = Name.refName(fk);
                    TableSchema refTable = fk.refTableSchema();
                }
                @if(firstField.type() instanceof FieldType.SimpleType)
                    ${refName} = ${MethodStr.tableGet(refTable, refSimple, MethodStr.actualParams(fk.key().fields()))};
                    @if(!refSimple.nullable())
                        java.util.Objects.requireNonNull(${refName});
                    @endif
                @elseif(firstField.type() instanceof FieldType.FList fList)
                    !{
                        String firstFieldName = Generator.lower1(firstField.name());
                    }
                    if (${firstFieldName}.isEmpty()) {
                        ${refName} = java.util.Collections.emptyList();
                    } else {
                        ${refName} = new java.util.ArrayList<>(${firstFieldName}.size());
                        for (${TypeStr.boxType(fList.item())} e : ${firstFieldName}) {
                            ${Name.refType(refTable)} r = ${MethodStr.tableGet(refTable, refSimple, "e")};
                            java.util.Objects.requireNonNull(r);
                            ${refName}.add(r);
                        }
                    }
                @elseif(firstField.type() instanceof FieldType.FMap fMap)
                    !{
                        String firstFieldName = Generator.lower1(firstField.name());
                    }
                    if (${firstFieldName}.isEmpty()) {
                        ${refName} = java.util.Collections.emptyMap();
                    } else {
                        ${refName} = new java.util.LinkedHashMap<>(${firstFieldName}.size());
                        for (java.util.Map.Entry<${TypeStr.boxType(fMap.key())}, ${TypeStr.boxType(fMap.value())}> e : ${firstFieldName}.entrySet()) {
                            ${Name.refType(refTable)} rv = ${MethodStr.tableGet(refTable, refSimple, "e.getValue()")};
                            java.util.Objects.requireNonNull(rv);
                            ${refName}.put(e.getKey(), rv);
                        }
                    }
                @endif
            @elseif(fk.refKey() instanceof RefKey.RefList refList)
                !{
                    String refName = Name.refName(fk);
                    TableSchema refTable = fk.refTableSchema();
                    NameableName refN = new NameableName(refTable);
                    boolean isEnumAndNoDetail = GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(refTable);
                    boolean isEnum = refTable.entry() instanceof EntryType.EEnum && !isEnumAndNoDetail;
                }
                ${refName} = new java.util.ArrayList<>();
                @if(isEnumAndNoDetail)
                    for (${refN.fullName} v : ${refN.fullName}.values()) {
                @elseif(isEnum)
                    for (${refN.fullName} vv : ${refN.fullName}.values()) {
                        ${refN.fullName}_Detail v = mgr.${refN.containerPrefix}All.get(vv.get${Generator.upper1(refTable.primaryKey().fields().getFirst())}());
                @else
                    for (${Name.refType(refTable)} v : mgr.${refN.containerPrefix}All.values()) {
                @endif
                    !{
                        java.util.List<String> eqs = new java.util.ArrayList<>();
                        for (int i = 0; i < fk.key().fields().size(); i++) {
                            FieldSchema k = fk.key().fieldSchemas().get(i);
                            String rk = refList.keyNames().get(i);
                            eqs.add(MethodStr.equal("v.get" + Generator.upper1(rk) + "()", Generator.lower1(k.name()), k.type()));
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
