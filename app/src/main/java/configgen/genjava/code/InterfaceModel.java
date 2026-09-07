package configgen.genjava.code;

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
                       String className,
                       String fullName) {
    }

    InterfaceModel(GenCfg cfg, InterfaceSchema sInterface, NameableName name) {
        this.pkg = name.pkg;
        this.codeTopPkg = cfg.codeTopPkg();
        this.className = name.className;
        this.isSealedInterface = cfg.isSealedInterface();
        this.nullableEnumRefTable = sInterface.nullableEnumRefTable() != null ?
                Name.refType(cfg, sInterface.nullableEnumRefTable()) : null;
        this.hasRef = HasRef.hasRef(sInterface);

        this.impls = sInterface.impls().stream().map(impl -> {
            // permits 用与实际生成 impl 类同一个 NameableName，保证 prefix/beautifulName 下名字一致
            NameableName implName = new NameableName(cfg, impl);
            return new Impl(impl.name(), implName.className, implName.fullName);
        }).toList();
    }
}
