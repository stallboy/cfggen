package configgen.genbytes;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genjava.ConfigInput;
import configgen.genjava.ConfigPackage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PackageBuildTest {
    @TempDir Path root;

    @Test
    void sparseModUsesFrozenSchemaAndValidatesAgainstBase() throws Exception {
        Path baseDir = Files.createDirectories(root.resolve("base-data"));
        Context base = TestCtx.newContext(baseDir, """
                table department[id] { id:int (seq); name:str; }
                table unit[id] { id:int; name:str; department:int ->department; [name]; }
                """, Map.of(
                "department", "id,name\nid,name\n0,alpha\n1,beta\n",
                "unit", "id,name,department\nid,name,department\n1,one,0\n2,two,1\n"));
        Path baseOut = root.resolve("base-pack");
        new BytesGenerator(new ParameterParser("bytes,pack:base,dir:" + baseOut)).generate(base);
        String schemaBefore = Files.readString(baseDir.resolve("config.cfg"));
        Path modDir = Files.createDirectories(root.resolve("mod-data"));
        Files.writeString(modDir.resolve("unit.csv"), "id,name,department\nid,name,department\n1,changed,1\n3,new,0\n");
        Path output = root.resolve("mod-pack");
        String arguments = "bytes,pack:map,requires:base,deps:"
                + baseOut.resolve("config.bytes") + ",dir:" + output;
        new BytesGenerator(new ParameterParser(arguments)).generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema()));
        ConfigPackage pack = ConfigPackage.read(new ConfigInput(output.resolve("config.bytes")));
        assertTrue(pack.blockNames().contains(".unit"));
        assertFalse(pack.blockNames().contains(".department"), "Export only rows owned by this mod");
        assertEquals(2, pack.block(".unit").readInt());
        assertEquals(schemaBefore, Files.readString(baseDir.resolve("config.cfg")));
        assertFalse(Files.exists(modDir.resolve("config.cfg")), "Never synthesize or rewrite a schema in the mod directory");

        Files.writeString(modDir.resolve("unit.csv"), "id,name,department\nid,name,department\n1,two,0\n");
        assertThrows(RuntimeException.class, () -> new BytesGenerator(new ParameterParser(arguments))
                .generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema())), "Final unique-key collision must fail");
        Files.writeString(modDir.resolve("unit.csv"), "id,name,department\nid,name,department\n1,changed,99\n");
        assertThrows(RuntimeException.class, () -> new BytesGenerator(new ParameterParser(arguments))
                .generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema())), "Missing cross-pack references must fail");

        Files.delete(modDir.resolve("unit.csv"));
        Files.writeString(modDir.resolve("department.csv"), "id,name\nid,name\n1,overridden\n");
        assertDoesNotThrow(() -> new BytesGenerator(new ParameterParser(arguments))
                .generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema())), "A seq mod is sparse before merging");

        Files.writeString(modDir.resolve("monsetr.csv"), "id\nid\n1\n");
        var failure = assertThrows(IllegalArgumentException.class, () -> new Context(Context.ContextCfg.of(modDir), base.cfgSchema()));
        assertTrue(failure.getMessage().contains("monsetr"));
    }

    @Test
    void fixedSchemaEnumCannotBeRedefinedByFirstPackage() throws Exception {
        Path data = Files.createDirectories(root.resolve("enum"));
        var schema = configgen.schema.cfg.CfgReader.parse("enum color { Red=1; Blue=2; }");
        schema.resolve().checkErrors("test");
        Files.writeString(data.resolve("color.csv"), "name,id,comment\nname,id,comment\nRed,9,changed\nBlue,2,blue\n");
        var error = assertThrows(configgen.genjava.ConfigErr.class, () -> new BytesGenerator(new ParameterParser("bytes,pack:base,dir:" + root.resolve("bad")))
                .generate(new Context(Context.ContextCfg.of(data), schema)));
        assertTrue(error.getMessage().contains("enum ID changed"));
    }
}
