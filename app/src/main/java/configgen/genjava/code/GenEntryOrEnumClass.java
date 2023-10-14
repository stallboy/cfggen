package configgen.genjava.code;

import configgen.schema.EntryType;
import configgen.schema.FieldSchema;
import configgen.schema.ForeignKeySchema;
import configgen.schema.TableSchema;
import configgen.util.CachedIndentPrinter;

import java.util.Map;

import static configgen.gen.Generator.lower1;
import static configgen.gen.Generator.upper1;
import static configgen.value.CfgValue.VTable;

class GenEntryOrEnumClass {

    static void generate(VTable vTable, EntryType.EntryBase entryBase, NameableName name, CachedIndentPrinter ps,
                         boolean isNeedReadData, NameableName dataName) {
        TableSchema table = vTable.schema();
        ps.println("package " + name.pkg + ";");
        ps.println();

        boolean isEnum = entryBase instanceof EntryType.EEnum;
        ps.println((isEnum ? "public enum " : "public class ") + name.className + " {");
        boolean hasNoIntValue = vTable.enumNameToIntegerValueMap() == null;
        if (hasNoIntValue) {
            int len = vTable.enumNames().size();
            int c = 0;
            for (String enumName : vTable.enumNames()) {
                c++;
                String fix = c == len ? ";" : ",";
                if (isEnum) {
                    ps.println1("%s(\"%s\")%s", enumName.toUpperCase(), enumName, fix);
                } else {
                    ps.println1("public static final %s %s = new %s(\"%s\");", name.className, enumName.toUpperCase(), name.className, enumName);
                }
            }
            if (isEnum && 0 == c) {
                ps.println1(";");
            }

            ps.println();
            ps.println1("private final String value;");
            ps.println();

            ps.println1("%s(String value) {", name.className);
            ps.println2("this.value = value;");
            ps.println1("}");
            ps.println();

//            ps.println1("public String getValue() {");
//            ps.println2("return value;");
//            ps.println1("}");
//            ps.println();

        } else {
            int len = vTable.enumNameToIntegerValueMap().size();
            int c = 0;
            for (Map.Entry<String, Integer> entry : vTable.enumNameToIntegerValueMap().entrySet()) {
                String enumName = entry.getKey();
                int value = entry.getValue();
                c++;
                String fix = c == len ? ";" : ",";

                if (isEnum) {
                    ps.println1("%s(\"%s\", %d)%s", enumName.toUpperCase(), enumName, value, fix);
                } else {
                    ps.println1("public static final %s %s = new %s(\"%s\", %d);", name.className, enumName.toUpperCase(), name.className, enumName, value);
                }
            }
            if (isEnum && 0 == c) {
                ps.println1(";");
            }

            ps.println();
            ps.println1("private final String name;");
            ps.println1("private final int value;");

            ps.println();

            ps.println1("%s(String name, int value) {", name.className);
            ps.println2("this.name = name;");
            ps.println2("this.value = value;");
            ps.println1("}");
            ps.println();

//            ps.println1("public String getName() {");
//            ps.println2("return name;");
//            ps.println1("}");
//            ps.println();
//
//            ps.println1("public int getValue() {");
//            ps.println2("return value;");
//            ps.println1("}");
//            ps.println();
        }


        if (isEnum) {
            ps.println1("private static final java.util.Map<%s, %s> map = new java.util.HashMap<>();", hasNoIntValue ? "String" : "Integer", name.className);
            ps.println();

            ps.println1("static {");
            ps.println2("for(%s e : %s.values()) {", name.className, name.className);
            ps.println3("map.put(e.value, e);");
            ps.println2("}");
            ps.println1("}");
            ps.println();

            ps.println1("public static %s get(%s value) {", name.className, hasNoIntValue ? "String" : "int");
            ps.println2("return map.get(value);");
            ps.println1("}");
            ps.println();

            for (FieldSchema field : table.fields()) {
                String comment = field.comment();

                if (!comment.isEmpty()) {
                    ps.println1("/**");
                    ps.println1(" * " + comment);
                    ps.println1(" */");
                }
                ps.println1("public " + TypeStr.type(field.type()) + " get" + upper1(field.name()) + "() {");
                if (field == table.primaryKey().fieldSchemas().get(0)) {
                    ps.println2("return value;");
                } else if (field == entryBase.fieldSchema()) {
                    ps.println2("return name;");
                } else {
                    ps.println2("return ref().get" + upper1(field.name()) + "();");
                }
                ps.println1("}");
                ps.println();

            }

            for (ForeignKeySchema fk : table.foreignKeys()) {
                String refFuncName = lower1(Name.refName(fk));
                ps.println1("public " + Name.refType(fk) + " " + refFuncName + "() {");
                ps.println2("return ref()." + refFuncName + "();");
                ps.println1("}");
                ps.println();
            }
        }

        if (isNeedReadData) {
            ps.println1("public %s ref() {", dataName.fullName);
            ps.println2("return %s.get(value);", dataName.fullName);
            ps.println1("}");
            ps.println();
        }

        ps.println("}");
    }
}
