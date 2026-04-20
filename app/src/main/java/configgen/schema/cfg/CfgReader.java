package configgen.schema.cfg;

import configgen.schema.*;
import configgen.schema.FieldType.*;
import configgen.schema.Metadata.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import configgen.schema.Metadata.MetaEnumValues;
import static configgen.schema.Metadata.MetaTag.TAG;
import static configgen.schema.cfg.CfgParser.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public enum CfgReader {
    INSTANCE;

    public static CfgSchema parse(String cfgStr) {
        CfgSchema cfg = CfgSchema.of();
        INSTANCE.readCfgSchema(cfg, CharStreams.fromString(cfgStr), "", "<>");
        return cfg;
    }

    public void read(CfgSchema destination, Path source, String pkgNameDot) {
        CharStream input;
        try {
            input = CharStreams.fromPath(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        readCfgSchema(destination, input, pkgNameDot, source.normalize().toString());
    }

    private void readCfgSchema(CfgSchema destination, CharStream input, String pkgNameDot, String fromCfgFilePath) {
        CfgLexer lexer = new CfgLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CfgParser parser = new CfgParser(tokens);

        // 添加自定义错误监听器
        parser.removeErrorListeners();
        parser.addErrorListener(ThrowingErrorListener.INSTANCE);

        SchemaContext schema = parser.schema();
        for (Schema_eleContext ele : schema.schema_ele()) {
            ParseTree child = ele.getChild(0);
            switch (child) {
                case Struct_declContext ctx -> {
                    StructSchema struct = readStruct(ctx, pkgNameDot);
                    destination.add(struct);
                }
                case Interface_declContext ctx -> {
                    InterfaceSchema sInterface = readInterface(ctx, pkgNameDot);
                    destination.add(sInterface);
                }
                case Table_declContext ctx -> {
                    TableSchema table = readTable(ctx, pkgNameDot, fromCfgFilePath);
                    destination.add(table);
                }
                case Enum_declContext ctx -> {
                    TableSchema enumTable = readEnum(ctx, pkgNameDot);
                    destination.add(enumTable);
                }

                default -> throw new IllegalStateException("Unexpected value: " + child);
            }
        }

        // 读取并存储文件末尾注释
        String fileEndComment = CommentUtils.readSuffixComment(schema.suffix_comment());
        if (!fileEndComment.isEmpty()) {
            destination.setFileEndComment(pkgNameDot, fileEndComment);
        }
    }

    private TableSchema readTable(Table_declContext ctx, String pkgNameDot, String fromCfgFilePath) {
        String name = readNamespaceIdentifier(ctx.ns_ident());
        KeySchema primaryKey = readKey(ctx.key());

        String fullComment = CommentUtils.readFull3(
                ctx.leading_comment(),
                ctx.LC_COMMENT(),
                ctx.suffix_comment());

        Metadata meta = readMetadataValues(ctx.metadata());
        meta.putFromCfgFilepath(fromCfgFilePath);
        if (!fullComment.isEmpty()) {
            meta.putComment(fullComment);
        }

        EntryType entry = meta.removeEntry();
        boolean isColumnMode = meta.removeColumnMode();
        StructSpec ff = readStructSpec(ctx.field_decl(), ctx.foreign_decl());

        List<Key_declContext> kds = ctx.key_decl();
        List<KeySchema> uniqueKeys = new ArrayList<>(kds.size());
        for (Key_declContext kd : kds) {
            KeySchema uniqKey = readKey(kd.key());
            uniqueKeys.add(uniqKey);
        }

        return new TableSchema(pkgNameDot + name, primaryKey, entry, isColumnMode, meta,
                ff.fieldSchemas(), ff.foreignKeySchemas(), uniqueKeys);
    }

    private TableSchema readEnum(Enum_declContext ctx, String pkgNameDot) {
        String name = readNamespaceIdentifier(ctx.ns_ident());

        String fullComment = CommentUtils.readFull3(
                ctx.leading_comment(),
                ctx.LC_COMMENT(),
                ctx.suffix_comment());

        Metadata meta = readMetadataValues(ctx.metadata());
        if (!fullComment.isEmpty()) {
            meta.putComment(fullComment);
        }

        // 解析 enum 值：语法保证要么全是 empty，要么全是 assigned
        List<Enum_value_assignedContext> assignedCtxs = ctx.enum_value_assigned();
        if (!assignedCtxs.isEmpty()) {
            // 有赋值的 enum
            List<Metadata.EnumValueAssigned> enumValues = new ArrayList<>();
            for (Enum_value_assignedContext evc : assignedCtxs) {
                String valueName = evc.identifier().getText();
                String valueComment = CommentUtils.readFull2(
                        evc.leading_comment(),
                        evc.SEMI_COMMENT());
                int number = parseEnumNumber(evc.enum_number());
                enumValues.add(new Metadata.EnumValueAssigned(valueName, valueComment, number));
            }
            meta.putEnumValues(new MetaEnumValues.OfAssigned(enumValues));

            return new TableSchema(
                    pkgNameDot + name,
                    new KeySchema(List.of("name")),
                    new EntryType.EEnum("name"),
                    false,
                    meta,
                    List.of(
                            new FieldSchema("name", Primitive.STRING, FieldFormat.AutoOrPack.AUTO, Metadata.of()),
                            new FieldSchema("id", Primitive.INT, FieldFormat.AutoOrPack.AUTO, Metadata.of()),
                            new FieldSchema("comment", Primitive.TEXT, FieldFormat.AutoOrPack.AUTO, Metadata.of())
                    ),
                    List.of(),
                    List.of(new KeySchema(List.of("id")))
            );
        } else {
            // 无赋值的 enum
            List<Metadata.EnumValueEmpty> enumValues = new ArrayList<>();
            for (Enum_value_emptyContext evc : ctx.enum_value_empty()) {
                String valueName = evc.identifier().getText();
                String valueComment = CommentUtils.readFull2(
                        evc.leading_comment(),
                        evc.SEMI_COMMENT());
                enumValues.add(new Metadata.EnumValueEmpty(valueName, valueComment));
            }
            meta.putEnumValues(new MetaEnumValues.OfEmpty(enumValues));

            return new TableSchema(
                    pkgNameDot + name,
                    new KeySchema(List.of("name")),
                    new EntryType.EEnum("name"),
                    false,
                    meta,
                    List.of(
                            new FieldSchema("name", Primitive.STRING, FieldFormat.AutoOrPack.AUTO, Metadata.of()),
                            new FieldSchema("comment", Primitive.TEXT, FieldFormat.AutoOrPack.AUTO, Metadata.of())
                    ),
                    List.of(),
                    List.of()
            );
        }
    }

    private int parseEnumNumber(Enum_numberContext ctx) {
        String text = ctx.getText();
        if (text.startsWith("0x") || text.startsWith("0X") ||
            text.startsWith("-0x") || text.startsWith("-0X") ||
            text.startsWith("+0x") || text.startsWith("+0X")) {
            return Integer.decode(text);
        }
        return Integer.parseInt(text);
    }

    private InterfaceSchema readInterface(Interface_declContext ctx, String pkgNameDot) {
        String name = readNamespaceIdentifier(ctx.ns_ident());

        String fullComment = CommentUtils.readFull3(
                ctx.leading_comment(),
                ctx.LC_COMMENT(),
                ctx.suffix_comment());

        Metadata meta = readMetadataValues(ctx.metadata());
        if (!fullComment.isEmpty()) {
            meta.putComment(fullComment);
        }

        String enumRef = meta.removeEnumRef();
        String defaultImpl = meta.removeDefaultImpl();
        FieldFormat fmt = meta.removeFmt();

        List<Struct_declContext> struct_decls = ctx.struct_decl();
        List<StructSchema> structSchemas = new ArrayList<>(struct_decls.size());
        for (Struct_declContext sc : ctx.struct_decl()) {
            StructSchema struct = readStruct(sc, "");
            structSchemas.add(struct);
        }
        return new InterfaceSchema(pkgNameDot + name, enumRef, defaultImpl, fmt, meta, structSchemas);
    }

    private StructSchema readStruct(Struct_declContext ctx, String pkgNameDot) {
        String name = readNamespaceIdentifier(ctx.ns_ident());

        String fullComment = CommentUtils.readFull3(
                ctx.leading_comment(),
                ctx.LC_COMMENT(),
                ctx.suffix_comment());

        Metadata meta = readMetadataValues(ctx.metadata());
        if (!fullComment.isEmpty()) {
            meta.putComment(fullComment);
        }

        FieldFormat fmt = meta.removeFmt();
        StructSpec ff = readStructSpec(ctx.field_decl(), ctx.foreign_decl());
        return new StructSchema(pkgNameDot + name, fmt, meta, ff.fieldSchemas(), ff.foreignKeySchemas());
    }


    private String readNamespaceIdentifier(Ns_identContext ctx) {
        List<String> ids = new ArrayList<>();
        for (IdentifierContext ic : ctx.identifier()) {
            ids.add(ic.getText());
        }
        return String.join(".", ids);
    }

    private Metadata readMetadataValues(MetadataContext metadata) {
        Metadata meta = Metadata.of();
        for (Ident_with_opt_single_valueContext m : metadata.ident_with_opt_single_value()) {
            Minus_identContext minusIdentContext = m.minus_ident();
            if (minusIdentContext == null) {
                String k = m.identifier().getText();
                Single_valueContext val = m.single_value();
                if (val == null) {
                    meta.data().put(k, TAG);
                } else {
                    TerminalNode tn = (TerminalNode) val.getChild(0);
                    meta.data().put(k, readMetaValue(tn));
                }
            } else {
                meta.data().put("-" + minusIdentContext.identifier().getText(), TAG);
            }
        }
        return meta;
    }

    private static MetaValue readMetaValue(TerminalNode tn) {
        int type = tn.getSymbol().getType();
        String text = tn.getSymbol().getText();
        return switch (type) {
            case INTEGER_CONSTANT -> new MetaInt(Integer.parseInt(text));
            case HEX_INTEGER_CONSTANT -> new MetaInt(Integer.decode(text));
            case FLOAT_CONSTANT -> new MetaFloat(Float.parseFloat(text));
            case STRING_CONSTANT -> new MetaStr(text.trim().substring(1, text.trim().length() - 1));
            case BOOL_CONSTANT -> new MetaStr(text);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private record StructSpec(List<FieldSchema> fieldSchemas,
                              List<ForeignKeySchema> foreignKeySchemas) {
    }

    private StructSpec readStructSpec(List<Field_declContext> fieldDeclContexts,
                                      List<Foreign_declContext> foreignDeclContexts) {
        List<FieldSchema> fieldSchemas = new ArrayList<>(fieldDeclContexts.size());
        List<ForeignKeySchema> foreignKeySchemas = new ArrayList<>(foreignDeclContexts.size());
        for (Field_declContext ctx : fieldDeclContexts) {
            String name = ctx.identifier().getText();
            FieldType type = readType(ctx.type_());
            String comment = CommentUtils.readFull2(
                    ctx.leading_comment(),
                    ctx.SEMI_COMMENT());
            Metadata meta = readMetadataValues(ctx.metadata());
            if (!comment.isEmpty()) {
                meta.putComment(comment);
            }

            FieldFormat fmt = meta.removeFmt();
            FieldSchema fieldSchema = new FieldSchema(name, type, fmt, meta);
            fieldSchemas.add(fieldSchema);

            RefContext ref = ctx.ref();
            if (ref != null) {
                KeySchema localKey = new KeySchema(List.of(name));
                boolean nullable = meta.removeNullable();
                Metadata refMeta = meta.copy();
                refMeta.removeComment();
                ForeignKeySchema foreignKeySchema = readRef(ref, name, localKey, refMeta, nullable);
                foreignKeySchemas.add(foreignKeySchema);
            }
        }

        for (Foreign_declContext ctx : foreignDeclContexts) {
            String name = ctx.identifier().getText();
            KeySchema localKey = readKey(ctx.key());
            String comment = CommentUtils.readFull2(
                    ctx.leading_comment(),
                    ctx.SEMI_COMMENT());
            Metadata meta = readMetadataValues(ctx.metadata());
            if (!comment.isEmpty()) {
                meta.putComment(comment);
            }

            boolean nullable = meta.removeNullable();
            ForeignKeySchema foreignKeySchema = readRef(ctx.ref(), name, localKey, meta, nullable);
            foreignKeySchemas.add(foreignKeySchema);
        }

        return new StructSpec(fieldSchemas, foreignKeySchemas);
    }

    private FieldType readType(Type_Context ctx) {
        if (ctx instanceof TypeListContext listCtx) {
            return new FList(readTypeEle(listCtx.type_ele()));
        } else if (ctx instanceof TypeMapContext mapCtx) {
            return new FMap(readTypeEle(mapCtx.type_ele(0)), readTypeEle(mapCtx.type_ele(1)));
        } else { // TypeBasicContext
            TypeBasicContext basicCtx = (TypeBasicContext) ctx;
            return readTypeEle(basicCtx.type_ele());
        }
    }

    private SimpleType readTypeEle(Type_eleContext ctx) {
        TerminalNode tBase = ctx.TBASE();
        if (tBase != null) {
            String text = tBase.getText().toUpperCase();
            if (text.equals("STR"))
                return Primitive.STRING;
            return Primitive.valueOf(text);
        } else {
            return new StructRef(readNamespaceIdentifier(ctx.ns_ident()));
        }
    }

    private ForeignKeySchema readRef(RefContext ctx, String name, KeySchema localKey,
                                     Metadata meta, boolean nullable) {
        String refTable = readNamespaceIdentifier(ctx.ns_ident());
        RefKey refKey;
        KeySchema remoteKey = null;
        KeyContext keyCtx = ctx.key();
        if (keyCtx != null) {
            remoteKey = readKey(keyCtx);
        }
        if (remoteKey == null) {
            refKey = new RefKey.RefPrimary(nullable);
        } else if (ctx.REF() != null) {
            refKey = new RefKey.RefUniq(remoteKey, nullable);
        } else {
            refKey = new RefKey.RefList(remoteKey);
        }
        return new ForeignKeySchema(name, localKey, refTable, refKey, meta);
    }

    private KeySchema readKey(KeyContext keyCtx) {
        List<IdentifierContext> identifiers = keyCtx.identifier();
        List<String> rk = new ArrayList<>(identifiers.size());
        for (IdentifierContext ic : identifiers) {
            rk.add(ic.getText());
        }
        return new KeySchema(rk);
    }
}
