package configgen.genjava.code;

import configgen.schema.HasRef;
import configgen.schema.InterfaceSchema;
import configgen.schema.StructSchema;
import configgen.util.CachedIndentPrinter;

import java.util.stream.Collectors;

import static configgen.gen.Generator.upper1;

class GenInterface {

    static void generate(InterfaceSchema sInterface, NameableName name, CachedIndentPrinter ps) {
        ps.println(STR. "package \{ name.pkg };" );
        ps.println();
        if (NameableName.isSealedInterface) {
            String implClassNameList = sInterface.impls().stream().map(s -> upper1(s.name())).collect(Collectors.joining(", "));
            ps.println(STR. "public sealed interface \{ name.className } permits \{ implClassNameList } {" );
        } else {
            ps.println(STR. "public interface \{ name.className } {" );
        }
        ps.inc();
        ps.println(STR. "\{ Name.refType(sInterface.enumRefTable()) } type();" );
        ps.println();

        if (HasRef.hasRef(sInterface)) {
            ps.println(STR. "default void _resolve(\{ Name.codeTopPkg }.ConfigMgr mgr) {" );
            ps.println("}");
            ps.println();
        }

        ps.println(STR. "static \{ name.className } _create(configgen.genjava.ConfigInput input) {" );
        ps.inc();
        ps.println("switch(input.readStr()) {");
        for (StructSchema impl : sInterface.impls()) {
            if (impl == sInterface.nullableDefaultImplStruct()) {
                ps.println1("case \"\":");
            }
            ps.println1(STR. "case \"\{ impl.name() }\":" );
            ps.println2(STR. "return \{ Name.fullName(impl) }._create(input);" );
        }

        ps.println("}");
        ps.println("throw new IllegalArgumentException();");
        ps.dec();
        ps.println("}");
        ps.dec();
        ps.println("}");
    }
}
