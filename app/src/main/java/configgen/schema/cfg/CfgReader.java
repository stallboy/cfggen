package configgen.schema.cfg;


import configgen.schema.*;
import configgen.schema.FieldType.FList;
import configgen.schema.FieldType.FMap;
import configgen.schema.FieldType.Primitive;
import configgen.schema.FieldType.StructRef;
import configgen.schema.Metadata.MetaFloat;
import configgen.schema.Metadata.MetaInt;
import configgen.schema.Metadata.MetaStr;
import configgen.schema.Metadata.MetaValue;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.units.qual.A;

import static configgen.schema.Metadata.MetaTag.TAG;
import static configgen.schema.cfg.CfgParser.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public enum CfgReader {
    INSTANCE;

    public void read(Path path) {
        CharStream input;
        try {
            input = CharStreams.fromPath(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CfgLexer lexer = new CfgLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CfgParser parser = new CfgParser(tokens);

        CfgSchema cfg = CfgSchema.of();
        SchemaContext schema = parser.schema();
        for (Schema_eleContext ele : schema.schema_ele()) {
            ParseTree child = ele.getChild(0);
            switch (child) {
                case Struct_declContext ctx -> {
                    StructSchema struct = read_struct(ctx);
                    System.out.println(struct);

                }
                case Interface_declContext ctx -> {

                }
                case Table_declContext ctx -> {

                }

                default -> throw new IllegalStateException("Unexpected value: " + child);
            }

        }
    }

    public StructSchema read_struct(Struct_declContext ctx) {
        String name = read_ns_ident(ctx.ns_ident());
        Metadata meta = read_metadata(ctx.metadata(), ctx.COMMENT());
        FieldFormat fmt = Metas.removeFmt(meta);
        FieldsAndForeigns ff = read_fields_foreigns(ctx.field_decl(), ctx.foreign_decl());
        return new StructSchema(name, fmt, meta, ff.fieldSchemas(), ff.foreignKeySchemas());
    }

    public String read_ns_ident(Ns_identContext ctx) {
        List<String> ids = new ArrayList<>();
        for (IdentifierContext ic : ctx.identifier()) {
            ids.add(ic.IDENT().getText());
        }
        return String.join(".", ids);
    }

    private Metadata read_metadata(MetadataContext metadata, TerminalNode comment) {
        Metadata meta = Metadata.of();
        for (Ident_with_opt_single_valueContext m : metadata.ident_with_opt_single_value()) {
            String k = m.identifier().IDENT().getText();
            Single_valueContext val = m.single_value();
            if (val == null) {
                meta.data().put(k, TAG);
            } else {
                TerminalNode tn = (TerminalNode) val.getChild(0);
                int type = tn.getSymbol().getType();
                String text = tn.getSymbol().getText();
                MetaValue mv = switch (type) {
                    case INTEGER_CONSTANT -> new MetaInt(Integer.parseInt(text));
                    case HEX_INTEGER_CONSTANT -> new MetaInt(Integer.decode(text));
                    case FLOAT_CONSTANT -> new MetaFloat(Float.parseFloat(text));
                    case STRING_CONSTANT -> new MetaStr(text);
                    default -> throw new IllegalStateException("Unexpected value: " + type);
                };
                meta.data().put(k, mv);
            }
        }

        if (comment != null) {
            String c = comment.getText();
            c = c.substring(2).trim();
            if (!c.isEmpty()) {
                Metas.putComment(meta, c);
            }
        }
        return meta;
    }

    private record FieldsAndForeigns(List<FieldSchema> fieldSchemas,
                                     List<ForeignKeySchema> foreignKeySchemas) {
    }

    private FieldsAndForeigns read_fields_foreigns(List<Field_declContext> fieldDeclContexts,
                                                   List<Foreign_declContext> foreignDeclContexts) {
        List<FieldSchema> fieldSchemas = new ArrayList<>(fieldDeclContexts.size());
        List<ForeignKeySchema> foreignKeySchemas = new ArrayList<>(foreignDeclContexts.size());
        for (Field_declContext ctx : fieldDeclContexts) {
            String name = ctx.identifier().IDENT().getText();
            FieldType type = read_type(ctx.type_());
            Metadata meta = read_metadata(ctx.metadata(), ctx.COMMENT());
            FieldFormat fmt = Metas.removeFmt(meta);
            FieldSchema fieldSchema = new FieldSchema(name, type, fmt, meta);
            fieldSchemas.add(fieldSchema);

            RefContext ref = ctx.ref();
            if (ref != null) {
                KeySchema localKey = new KeySchema(List.of(name));
                ForeignKeySchema foreignKeySchema = read_ref(ref, name, localKey, meta);
                foreignKeySchemas.add(foreignKeySchema);
            }
        }

        for (Foreign_declContext ctx : foreignDeclContexts) {
            String name = ctx.identifier().IDENT().getText();
            KeySchema localKey = read_key(ctx.key());
            Metadata meta = read_metadata(ctx.metadata(), ctx.COMMENT());
            ForeignKeySchema foreignKeySchema = read_ref(ctx.ref(), name, localKey, meta);
            foreignKeySchemas.add(foreignKeySchema);
        }

        return new FieldsAndForeigns(fieldSchemas, foreignKeySchemas);
    }

    private FieldType read_type(Type_Context ctx) {
        if (ctx.TLIST() != null) {
            return new FList(read_type_ele(ctx.type_ele(0)));
        } else if (ctx.TMAP() != null) {
            return new FMap(read_type_ele(ctx.type_ele(0)), read_type_ele(ctx.type_ele(1)));
        } else {
            return read_type_ele(ctx.type_ele(0));
        }
    }

    private FieldType read_type_ele(Type_eleContext ctx) {
        TerminalNode tbase = ctx.TBASE();
        if (tbase != null) {
            String text = tbase.getText();
            return Primitive.valueOf(text.toUpperCase());
        } else {
            return new StructRef(read_ns_ident(ctx.ns_ident()));
        }
    }

    private ForeignKeySchema read_ref(RefContext ctx, String name, KeySchema localKey, Metadata meta) {
        String refTable = read_ns_ident(ctx.ns_ident());
        RefKey refKey;
        KeySchema remoteKey = null;
        KeyContext keyCtx = ctx.key();
        if (keyCtx != null) {
            remoteKey = read_key(keyCtx);
        }
        boolean nullable = Metas.removeNullable(meta);
        if (remoteKey == null) {
            refKey = new RefKey.RefPrimary(nullable);
        } else if (ctx.REF() != null) {
            refKey = new RefKey.RefUniq(remoteKey, nullable);
        } else {
            refKey = new RefKey.RefList(remoteKey);
        }
        return new ForeignKeySchema(name, localKey, refTable, refKey, meta);
    }

    private KeySchema read_key(KeyContext keyCtx) {
        List<IdentifierContext> identifiers = keyCtx.identifier();
        List<String> rk = new ArrayList<>(identifiers.size());
        for (IdentifierContext ic : identifiers) {
            rk.add(ic.IDENT().getText());
        }
        return new KeySchema(rk);
    }


    public static void main(String[] args) {
        CfgReader.INSTANCE.read(Path.of("config.cfg"));
    }


}
