package configgen.genjava.code;

import configgen.schema.InterfaceSchema;
import configgen.type.TBean;
import configgen.util.CachedIndentPrinter;

class GenInterface {

    static void generate(InterfaceSchema sInterface, NameableName name, CachedIndentPrinter ps) {
        ps.println("package %s;", name.pkg);
        ps.println();
        ps.println("public interface %s {", name.className);
        ps.inc();
        ps.println("%s type();", Name.refType(sInterface.getChildDynamicBeanEnumRefTable()));
        ps.println();

        if (sInterface.hasRef()) {
            ps.println("default void _resolve(%s.ConfigMgr mgr) {", Name.codeTopPkg);
            ps.println("}");
            ps.println();
        }

        ps.println("static %s _create(configgen.genjava.ConfigInput input) {", name.className);
        ps.inc();
        ps.println("switch(input.readStr()) {");
        for (TBean actionBean : sInterface.getChildDynamicBeans()) {
            if (actionBean.name.equals(sInterface.getChildDynamicDefaultBeanName())) {
                ps.println1("case \"\":");
            }
            ps.println1("case \"%s\":", actionBean.name);
            ps.println2("return %s._create(input);", Name.fullName(actionBean));
        }

        ps.println("}");
        ps.println("throw new IllegalArgumentException();");
        ps.dec();
        ps.println("}");
        ps.dec();
        ps.println("}");
    }
}
