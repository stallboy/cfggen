package configgen.genjava.code;

import configgen.gen.Generator;
import configgen.schema.HasRef;
import configgen.schema.InterfaceSchema;

import java.util.List;

public class InterfaceModel {
    public final String pkg;
    public final String codeTopPkg;
    public final String className;
    public final boolean isSealedInterface;
    public final String nullableEnumRefTable;
    public final boolean hasRef;
    public final List<Impl> impls;

    public record Impl(String name,
                       String upper1Name,
                       String fullName) {
    }

    InterfaceModel(InterfaceSchema sInterface, NameableName name) {
        this.pkg = name.pkg;
        this.codeTopPkg = Name.codeTopPkg;
        this.className = name.className;
        this.isSealedInterface = NameableName.isSealedInterface;
        this.nullableEnumRefTable = sInterface.nullableEnumRefTable() != null ?
                Name.refType(sInterface.nullableEnumRefTable()) : null;
        this.hasRef = HasRef.hasRef(sInterface);

        this.impls = sInterface.impls().stream().map(impl ->
                        new Impl(impl.name(),
                                Generator.upper1(impl.name()),
                                Name.fullName(impl)))
                .toList();
    }
}