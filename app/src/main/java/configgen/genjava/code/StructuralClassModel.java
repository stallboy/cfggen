package configgen.genjava.code;

import configgen.schema.*;

import java.util.List;

import static configgen.util.StringUtil.lower1;

public class StructuralClassModel {
    public final Structural structural;
    public final NameableName name;

    public final GenCfg cfg;
    public final String pkg;
    public final String className;
    public final boolean isSealedInterface;
    public final boolean isImpl;
    public final boolean isTable;
    public final boolean isTableAndNeedBuilder;
    public final boolean isStructAndHasNoField;
    public final InterfaceSchema nullableInterface;
    public final String nullableInterfaceFullName;
    public final TableSchema enumRefTable;
    public final List<FieldInfo> fields;
    public final List<ForeignKeyInfo> foreignKeys;
    public final boolean hasRef;
    public final String codeTopPkg;
    public final String sourceComment; // 类文件顶部来源注释（trailing + 数据文件路径）；无则空串

    public record FieldInfo(String name,
                            String type,
                            String comment) {
    }

    public record ForeignKeyInfo(String type,
                                 String name) {
    }

    public StructuralClassModel(GenCfg cfg, Structural structural, NameableName name, boolean isTableAndNeedBuilder,
                                String sourceComment) {
        this.cfg = cfg;
        this.structural = structural;
        this.name = name;
        this.sourceComment = sourceComment;
        this.pkg = name.pkg;
        this.className = name.className;
        this.isSealedInterface = cfg.isSealedInterface();
        this.isTable = structural instanceof TableSchema;
        this.isTableAndNeedBuilder = isTableAndNeedBuilder;
        this.isStructAndHasNoField = !isTable && structural.fields().isEmpty();
        this.codeTopPkg = cfg.codeTopPkg();

        // Interface information
        nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        this.isImpl = nullableInterface != null;
        this.nullableInterfaceFullName = isImpl ? Name.fullName(cfg, nullableInterface) : null;
        this.enumRefTable = isImpl ? nullableInterface.nullableEnumRefTable() : null;

        // Fields
        this.fields = structural.fields().stream()
                .map(f -> new FieldInfo(lower1(f.name()), TypeStr.type(cfg, f.type()), f.comment()))
                .toList();

        // Foreign keys
        this.foreignKeys = structural.foreignKeys().stream()
                .map(fk -> new ForeignKeyInfo(Name.refType(cfg, fk), Name.refName(fk)))
                .toList();

        this.hasRef = HasRef.hasRef(structural);
    }


    public String formalParams() {
        return MethodStr.formalParams(cfg, structural.fields());
    }

    public String hashCodes() {
        return MethodStr.hashCodes(structural.fields());
    }

    public String equals() {
        return MethodStr.equals(structural.fields());
    }

    public String toStringParams() {
        return fields.stream()
                .map(FieldInfo::name)
                .reduce((a, b) -> a + " + \",\" + " + b)
                .orElse("");
    }

    // 以下为模板渲染的便捷入口：让 .jte 无须再传 cfg 调 Name/TypeStr 的静态方法
    public String refType(TableSchema table) {
        return Name.refType(cfg, table);
    }

    public String enumFieldName(String enumName) {
        return Name.enumFieldName(cfg, enumName);
    }

    public String readValue(FieldType t) {
        return TypeStr.readValue(cfg, t);
    }

    public String actualParamsKey(KeySchema keySchema, String pre) {
        return MethodStr.actualParamsKey(cfg, keySchema, pre, null);
    }
}
