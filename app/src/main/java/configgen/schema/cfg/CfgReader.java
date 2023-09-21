package configgen.schema.cfg;


import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import configgen.schema.CfgSchema;

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
                    read(ctx);

                }
                case Interface_declContext ctx -> {

                }
                case Table_declContext ctx -> {

                }

                default -> throw new IllegalStateException("Unexpected value: " + child);
            }
            System.out.println(ele);

        }
    }

    public void read(Struct_declContext ctx) {
        ctx.metadata();
        ctx.COMMENT();
    }

    public String read_ns_ident(Ns_identContext ctx) {
        List<String> ids = new ArrayList<>();
        for (IdentifierContext ic : ctx.identifier()) {
            ids.add(ic.IDENT().getText());
        }
        return String.join(".", ids);
    }

    public static void main(String[] args) {
        CfgReader.INSTANCE.read(Path.of("config.cfg"));
    }


}
