package configgen.genjava.code;

import configgen.schema.*;

import java.util.List;

public class ResolveModel {
    public final String className;
    public final boolean hasDirectRef;
    public final boolean isImpl;
    public final String codeTopPkg;
    public final List<ForeignKeySchema> foreignKeys;
    public final List<FieldSchema> fields;
    public final Structural structural;
    public final InterfaceSchema nullableInterface;

    public ResolveModel(Structural structural, InterfaceSchema nullableInterface) {
        this.structural = structural;
        this.className = new NameableName(structural).className;
        this.hasDirectRef = !structural.foreignKeys().isEmpty();
        this.isImpl = nullableInterface != null;
        this.codeTopPkg = Name.codeTopPkg;
        this.foreignKeys = structural.foreignKeys();
        this.fields = structural.fields();
        this.nullableInterface = nullableInterface;
    }
}