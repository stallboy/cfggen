package configgen.genjava.code;

import configgen.schema.HasRef;
import configgen.schema.InterfaceSchema;
import configgen.schema.StructSchema;
import configgen.util.CachedIndentPrinter;

import java.util.stream.Collectors;

import static configgen.gen.Generator.upper1;

class GenInterface {

    static void generate(InterfaceSchema sInterface, NameableName name, CachedIndentPrinter ps) {
        ps.println("package %s;", name.pkg);
        ps.println();
        if (NameableName.isSealedInterface) {
            String implClassNameList = sInterface.impls().stream().map(s -> upper1(s.name())).collect(Collectors.joining(", "));
            ps.println("public sealed interface %s permits %s {", name.className, implClassNameList);
        } else {
            ps.println("public interface %s {", name.className);
        }
        ps.inc();
        ps.println("%s type();", Name.refType(sInterface.enumRefTable()));
        ps.println();

        if (HasRef.hasRef(sInterface)) {
            ps.println("default void _resolve(%s.ConfigMgr mgr) {", Name.codeTopPkg);
            ps.println("}");
            ps.println();
        }

        ps.println("static %s _create(configgen.genjava.ConfigInput input) {", name.className);
        ps.inc();
        ps.println("switch(input.readStr()) {");
        for (StructSchema impl : sInterface.impls()) {
            if (impl == sInterface.nullableDefaultImplStruct()) {
                ps.println1("case \"\":");
            }
            ps.println1("case \"%s\":", impl.name());
            ps.println2("return %s._create(input);", Name.fullName(impl));
        }

        ps.println("}");
        ps.println("throw new IllegalArgumentException();");
        ps.dec();
        ps.println("}");
        ps.dec();
        ps.println("}");
    }
}
