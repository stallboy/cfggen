package configgen.genjava.code;


import configgen.genjava.GenJavaUtil;
import configgen.schema.*;
import configgen.util.CachedIndentPrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static configgen.gen.Generator.*;
import static configgen.schema.EntryType.*;
import static configgen.schema.FieldType.*;
import static configgen.value.CfgValue.*;

class GenStructuralClass {


    static void generate(Structural structural, VTable vtable,
                         NameableName name, CachedIndentPrinter ps, boolean isTableAndNeedBuilder) {
        boolean isTable = vtable != null;
        boolean isStruct = vtable == null;
        InterfaceSchema nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        boolean isImpl = nullableInterface != null;
        boolean isStructAndHasNoField = isStruct && structural.fields().isEmpty();

        ps.println("package %s;", name.pkg);
        ps.println();
        if (isImpl) {
            String classStr = NameableName.isSealedInterface ? "final class" : "class";
            ps.println("public %s %s implements %s {", classStr, name.className, Name.fullName(nullableInterface));

            TableSchema enumRefTable = nullableInterface.nullableEnumRefTable();
            if (enumRefTable != null) {
                ps.println1("@Override");
                ps.println1("public %s type() {", Name.refType(enumRefTable));
                ps.println2("return %s.%s;", Name.refType(enumRefTable), structural.name().toUpperCase());
                ps.println1("}");
                ps.println();
            }
        } else {
            ps.println("public class %s {", name.className);
        }

        // field
        for (FieldSchema field : structural.fields()) {
            ps.println1("private %s %s;", TypeStr.type(field.type()), lower1(field.name()));
        }

        if (!isTableAndNeedBuilder) {
            // fk
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                ps.println1("private %s %s;", Name.refType(fk), Name.refName(fk));
            }
            ps.println();
        }


        // constructor
        //noinspection StatementWithEmptyBody
        if (isStructAndHasNoField) {
            // 如果是没有field的struct
            // 后面会生成空参数的public构造函数
            // 这里忽略
        } else {
            ps.println1("private %s() {", name.className);
            ps.println1("}");
            ps.println();
        }

        if (isStruct) {
            // struct有public构造器
            ps.println1("public %s(%s) {", name.className, MethodStr.formalParams(structural.fields()));
            for (FieldSchema field : structural.fields()) {
                String ln = lower1(field.name());
                ps.println2("this.%s = %s;", ln, ln);
            }
            ps.println1("}");
            ps.println();
        } else if (isTableAndNeedBuilder) {
            GenStructuralClassTablePart.generateTableBuild(structural, name, ps);
        }

        // static create from ConfigInput
        ps.println1("public static %s _create(configgen.genjava.ConfigInput input) {", name.className);
        ps.println2("%s self = new %s();", name.className, name.className);
        for (FieldSchema field : structural.fields()) {
            String ln = lower1(field.name());
            switch (field.type()) {
                case SimpleType simpleType -> {
                    ps.println2("self.%s = %s;", ln, TypeStr.readValue(simpleType));
                }
                case FList fList -> {
                    ps.println2("{");
                    ps.println3("int c = input.readInt();");
                    ps.println3("if (c == 0) {");
                    ps.println4("self.%s = java.util.Collections.emptyList();", ln);
                    ps.println3("} else {");
                    ps.println4("self.%s = new java.util.ArrayList<>(c);", ln);
                    ps.println4("for (; c > 0; c--) {");
                    ps.println5("self.%s.add(%s);", ln, TypeStr.readValue((fList.item())));
                    ps.println4("}");
                    ps.println3("}");
                    ps.println2("}");
                }
                case FMap fMap -> {
                    ps.println2("{");
                    ps.println3("int c = input.readInt();");
                    ps.println3("if (c == 0) {");
                    ps.println4("self.%s = java.util.Collections.emptyMap();", ln);
                    ps.println3("} else {");
                    ps.println4("self.%s = new java.util.LinkedHashMap<>(c);", ln);
                    ps.println4("for (; c > 0; c--) {");
                    ps.println5("self.%s.put(%s, %s);", ln, TypeStr.readValue(fMap.key()),
                            TypeStr.readValue((fMap.value())));
                    ps.println4("}");
                    ps.println3("}");
                    ps.println2("}");
                }
            }
        }
        ps.println2("return self;");
        ps.println1("}");
        ps.println();


        // getter
        for (FieldSchema field : structural.fields()) {
            String n = field.name();
            String comment = field.comment();
            if (!comment.isEmpty()) {
                ps.println1("/**");
                ps.println1(" * " + comment);
                ps.println1(" */");
            }

            ps.println1("public " + TypeStr.type(field.type()) + " get" + upper1(n) + "() {");
            ps.println2("return " + lower1(n) + ";");
            ps.println1("}");
            ps.println();
        }

        if (!isTableAndNeedBuilder) {
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                ps.println1("public %s %s() {", Name.refType(fk), lower1(Name.refName(fk)));
                ps.println2("return %s;", Name.refName(fk));
                ps.println1("}");
                ps.println();
            }
        }


        if (isStructAndHasNoField) {
            ps.println1("@Override");
            ps.println1("public int hashCode() {");
            ps.println2("return " + name.className + ".class.hashCode();");
            ps.println1("}");
            ps.println();

            ps.println1("@Override");
            ps.println1("public boolean equals(Object other) {");
            ps.println2("return other instanceof " + name.className + ";");
            ps.println1("}");
            ps.println();


        } else if (isStruct) {
            ps.println1("@Override");
            ps.println1("public int hashCode() {");
            ps.println2("return " + MethodStr.hashCodes(structural.fields()) + ";");
            ps.println1("}");
            ps.println();

            ps.println1("@Override");
            ps.println1("public boolean equals(Object other) {");
            ps.println2("if (!(other instanceof " + name.className + "))");
            ps.println3("return false;");
            ps.println2(name.className + " o = (" + name.className + ") other;");
            ps.println2("return " + MethodStr.equals(structural.fields()) + ";");
            ps.println1("}");
            ps.println();
        }

        // toString
        String beanName = "";
        if (isImpl)
            beanName = name.className;
        ps.println1("@Override");
        ps.println1("public String toString() {");
        if (isStructAndHasNoField) {
            ps.println2("return \"%s\";", beanName);
        } else {
            String params = structural.fields().stream().map(f -> lower1(f.name())).collect(
                    Collectors.joining(" + \",\" + "));
            ps.println2("return \"%s(\" + %s + \")\";", beanName, params);
        }
        ps.println1("}");
        ps.println();


        // _resolve
        if (HasRef.hasRef(structural) && !isTableAndNeedBuilder) {
            generateResolve(structural, nullableInterface, ps);
        }

        if (isTable) {
            GenStructuralClassTablePart.generate(structural, vtable, isTableAndNeedBuilder, name, ps);
        }
        ps.println("}");
    }


    private static void generateResolve(Structural structural, InterfaceSchema nullableInterface, CachedIndentPrinter ps) {
        boolean hasDirectRef = !structural.foreignKeys().isEmpty();
        if (hasDirectRef) {
            ps.println1("public void _resolveDirect(%s.ConfigMgr mgr) {", Name.codeTopPkg);

            // 2,处理本struct里的refSimple，
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                if (!(fk.refKey() instanceof RefKey.RefSimple refSimple)) {
                    continue;
                }
                FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                String refName = Name.refName(fk);
                TableSchema refTable = fk.refTableSchema();
                switch (firstField.type()) {
                    case SimpleType ignored -> {
                        ps.println2(refName + " = " + MethodStr.tableGet(refTable, refSimple,
                                MethodStr.actualParams(fk.key().fields())));
                        if (!refSimple.nullable())
                            ps.println2("java.util.Objects.requireNonNull(" + refName + ");");
                    }
                    case FList fList -> {
                        String firstFieldName = lower1(firstField.name());
                        ps.println2("if (%s.isEmpty()) {", firstFieldName);
                        ps.println3("%s = java.util.Collections.emptyList();", refName);
                        ps.println2("} else {");
                        ps.println3("%s = new java.util.ArrayList<>(%s.size());", refName, firstFieldName);
                        ps.println3("for (%s e : %s) {", TypeStr.boxType(fList.item()), firstFieldName);
                        ps.println4(Name.refType(refTable) + " r = " + MethodStr.tableGet(refTable, refSimple, "e"));
                        ps.println4("java.util.Objects.requireNonNull(r);");
                        ps.println4(refName + ".add(r);");
                        ps.println3("}");
                        ps.println2("}");
                    }
                    case FMap fMap -> {
                        String firstFieldName = lower1(firstField.name());
                        ps.println2("if (%s.isEmpty()) {", firstFieldName);
                        ps.println3("%s = java.util.Collections.emptyMap();", refName);
                        ps.println2("} else {");
                        ps.println3("%s = new java.util.LinkedHashMap<>(%s.size());", refName, firstFieldName);
                        ps.println3("for (java.util.Map.Entry<%s, %s> e : %s.entrySet()) {", TypeStr.boxType(fMap.key()), TypeStr.boxType(fMap.value()), firstFieldName);
                        ps.println4(Name.refType(refTable) + " rv = " + MethodStr.tableGet(refTable, refSimple, "e.getValue()"));
                        ps.println4("java.util.Objects.requireNonNull(rv);");
                        ps.println4(refName + ".put(e.getKey(), rv);");
                        ps.println3("}");
                        ps.println2("}");
                    }
                }
            }

            // 3,处理本struct里的refList
            for (ForeignKeySchema fk : structural.foreignKeys()) {
                if (!(fk.refKey() instanceof RefKey.RefList refList)) {
                    continue;
                }
                String refName = Name.refName(fk);
                TableSchema refTable = fk.refTableSchema();

                ps.println2("%s = new java.util.ArrayList<>();", refName);

                NameableName refN = new NameableName(refTable);
                boolean isEnumAndNoDetail = GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(refTable);
                boolean isEnum = refTable.entry() instanceof EEnum && !isEnumAndNoDetail;
                if (isEnumAndNoDetail) {
                    ps.println2("for (%s v : %s.values()) {", refN.fullName, refN.fullName);
                } else if (isEnum) {
                    ps.println2("for (%s vv : %s.values()) {", refN.fullName, refN.fullName);
                    String primK = refTable.primaryKey().fields().getFirst();
                    ps.println3("%s v = mgr.%sAll.get(vv.get%s());", refN.fullName + "_Detail", refN.containerPrefix,
                            upper1(primK));
                } else {
                    ps.println2("for (%s v : mgr.%sAll.values()) {", Name.refType(refTable), refN.containerPrefix); // 为了跟之前兼容
                }

                List<String> eqs = new ArrayList<>();
                for (int i = 0; i < fk.key().fields().size(); i++) {
                    FieldSchema k = fk.key().fieldSchemas().get(i);
                    String rk = refList.keyNames().get(i); // refKey不可能是refTable的primary key，所以可以直接调用keyNames
                    eqs.add(MethodStr.equal("v.get" + upper1(rk) + "()", lower1(k.name()), k.type()));
                }
                ps.println3("if (" + String.join(" && ", eqs) + ")");

                if (isEnumAndNoDetail) {
                    ps.println4(refName + ".add(v);");
                    ps.println2("}");
                } else if (isEnum) {
                    ps.println4(refName + ".add(vv);");
                    ps.println2("}");
                } else {
                    ps.println4(refName + ".add(v);");
                    ps.println2("}");
                }
                ps.println2("%s = %s.isEmpty() ? java.util.Collections.emptyList() : new java.util.ArrayList<>(%s);", refName, refName, refName);
            }

            ps.println1("}");
            ps.println();

        }

        boolean isImpl = nullableInterface != null;
        if (isImpl) {
            ps.println1("@Override");
        }
        ps.println1("public void _resolve(%s.ConfigMgr mgr) {", Name.codeTopPkg);


        // 1,先调用子_resolve
        for (FieldSchema field : structural.fields()) {
            FieldType type = field.type();
            if (!HasRef.hasRef(type)) {
                continue;
            }
            String ln = lower1(field.name());
            switch (type) {
                case StructRef ignored -> {
                    ps.println2("%s._resolve(mgr);", ln);
                }
                case FList fList -> {
                    ps.println2("for (%s e : %s) {", TypeStr.boxType(fList.item()), ln);
                    ps.println3("e._resolve(mgr);");
                    ps.println2("}");

                }
                case FMap fMap -> {
                    ps.println2("for (%s v : %s.values()) {", TypeStr.boxType(fMap.value()), ln);
                    ps.println3("v._resolve(mgr);");
                    ps.println2("}");
                }
                case Primitive ignored -> {
                }
            }
        }
        if (hasDirectRef) {
            ps.println2("_resolveDirect(mgr);");
        }

        ps.println1("}");
        ps.println();
    }

}
