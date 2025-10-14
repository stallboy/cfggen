package configgen.genjava.code;

import configgen.schema.*;

import java.util.List;

import static configgen.gen.Generator.lower1;

public class StructuralClassModel {
    public final Structural structural;
    public final NameableName name;
    public final List<String> mapsInMgr;

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

    public record FieldInfo(String name,
                            String type,
                            String comment) {
    }

    public record ForeignKeyInfo(String type,
                                 String name) {
    }

    public StructuralClassModel(Structural structural, NameableName name, boolean isTableAndNeedBuilder, List<String> mapsInMgr) {
        this.structural = structural;
        this.name = name;
        this.mapsInMgr = mapsInMgr;
        this.pkg = name.pkg;
        this.className = name.className;
        this.isSealedInterface = NameableName.isSealedInterface;
        this.isTable = structural instanceof TableSchema;
        this.isTableAndNeedBuilder = isTableAndNeedBuilder;
        this.isStructAndHasNoField = !isTable && structural.fields().isEmpty();
        this.codeTopPkg = Name.codeTopPkg;

        // Interface information
        nullableInterface = structural instanceof StructSchema struct ? struct.nullableInterface() : null;
        this.isImpl = nullableInterface != null;
        this.nullableInterfaceFullName = isImpl ? Name.fullName(nullableInterface) : null;
        this.enumRefTable = isImpl ? nullableInterface.nullableEnumRefTable() : null;

        // Fields
        this.fields = structural.fields().stream()
                .map(f -> new FieldInfo(lower1(f.name()), TypeStr.type(f.type()), f.comment()))
                .toList();

        // Foreign keys
        this.foreignKeys = structural.foreignKeys().stream()
                .map(fk -> new ForeignKeyInfo(Name.refType(fk), Name.refName(fk)))
                .toList();

        this.hasRef = HasRef.hasRef(structural);
    }


    public String formalParams() {
        return MethodStr.formalParams(structural.fields());
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
}