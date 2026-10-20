@import configgen.genjava.GenJavaUtil
@import configgen.genjava.code.*
@import configgen.schema.*
@import configgen.util.StringUtil
@param configgen.genjava.code.GenCfg cfg
@param Structural structural
        @for(ForeignKeySchema fk : structural.foreignKeys())
            @if(fk.refKey() instanceof RefKey.RefSimple refSimple)
                !{
                    FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                    String refName = Name.refName(fk);
                    TableSchema refTable = fk.refTableSchema();
                }
                @if(firstField.type() instanceof FieldType.SimpleType)
                    this.${refName} = ${MethodStr.tableGet(cfg, refTable, refSimple, MethodStr.actualParamsKeyRaw(fk.key(), "this."))};
                    @if(!refSimple.nullable())
                        if (this.${refName} == null) {
                            errors.missingReference("${structural.name()}", "${fk.name()}", "${refTable.name()}", ${MethodStr.referenceKey(fk.key())});
                        }
                    @endif
                @elseif(firstField.type() instanceof FieldType.FList fList)
                    !{
                        String firstFieldName = StringUtil.lower1(firstField.name());
                    }
                    if (this.${firstFieldName}.isEmpty()) {
                        this.${refName} = java.util.Collections.emptyList();
                    } else {
                        this.${refName} = new java.util.ArrayList<>(this.${firstFieldName}.size());
                        for (${TypeStr.boxType(cfg, fList.item())} e : this.${firstFieldName}) {
                            var ref = ${MethodStr.tableGet(cfg, refTable, refSimple, "e")};
                            if (ref == null) {
                                errors.missingReference("${structural.name()}", "${fk.name()}", "${refTable.name()}", e);
                            }
                            this.${refName}.add(ref);
                        }
                    }
                @elseif(firstField.type() instanceof FieldType.FMap fMap)
                    !{
                        String firstFieldName = StringUtil.lower1(firstField.name());
                    }
                    if (this.${firstFieldName}.isEmpty()) {
                        this.${refName} = java.util.Collections.emptyMap();
                    } else {
                        this.${refName} = new java.util.LinkedHashMap<>(this.${firstFieldName}.size());
                        for (java.util.Map.Entry<${TypeStr.boxType(cfg, fMap.key())}, ${TypeStr.boxType(cfg, fMap.value())}> e : this.${firstFieldName}.entrySet()) {
                            var ref = ${MethodStr.tableGet(cfg, refTable, refSimple, "e.getValue()")};
                            if (ref == null) {
                                errors.missingReference("${structural.name()}", "${fk.name()}", "${refTable.name()}", e.getValue());
                            }
                            this.${refName}.put(e.getKey(), ref);
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
                this.${refName} = new java.util.ArrayList<>();
                @if(isEnumAndNoDetail)
                    for (${refN.fullName} v : ${refN.fullName}.values()) {
                @elseif(isEnum)
                    for (${refN.fullName} vv : ${refN.fullName}.values()) {
                        ${Name.tableDataFullName(cfg, refTable)} v = vv.ref(mgr);
                        if (v == null) {
                            continue;
                        }
                @else
                    @if(refTable.primaryKey().fieldSchemas().size() == 1 && refTable.primaryKey().fieldSchemas().getFirst().isSeq())
                        for (${Name.refType(cfg, refTable)} v : mgr._get_${refN.containerPrefix}All()) {
                    @else
                        for (${Name.refType(cfg, refTable)} v : mgr._get_${refN.containerPrefix}All().values()) {
                    @endif
                @endif
                    !{
                        java.util.List<String> eqs = new java.util.ArrayList<>();
                        for (int i = 0; i < fk.key().fields().size(); i++) {
                            FieldSchema k = fk.key().fieldSchemas().get(i);
                            String rk = refList.keyNames().get(i);
                            eqs.add(MethodStr.equal("v.get" + StringUtil.upper1(rk) + "()", "this." + StringUtil.lower1(k.name()), k.type()));
                        }
                    }
                    if (${String.join(" && ", eqs)}) {
                        @if(isEnumAndNoDetail)
                            this.${refName}.add(v);
                        @elseif(isEnum)
                            this.${refName}.add(vv);
                        @else
                            this.${refName}.add(v);
                        @endif
                    }
                }
                this.${refName} = java.util.Collections.unmodifiableList(this.${refName});
            @endif
            @if(fk.refKey() instanceof RefKey.RefSimple && fk.key().fieldSchemas().getFirst().type() instanceof FieldType.FList)
                this.${Name.refName(fk)} = java.util.Collections.unmodifiableList(this.${Name.refName(fk)});
            @elseif(fk.refKey() instanceof RefKey.RefSimple && fk.key().fieldSchemas().getFirst().type() instanceof FieldType.FMap)
                this.${Name.refName(fk)} = java.util.Collections.unmodifiableMap(this.${Name.refName(fk)});
            @endif
        @endfor
