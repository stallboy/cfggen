package configgen.genbytes;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genjava.ConfigPackage;
import configgen.value.CfgValueErrs;
import configgen.value.CfgValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
                + baseOut.resolve("base.bytes") + ",dir:" + output;
        new BytesGenerator(new ParameterParser(arguments)).generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema()));
        ConfigPackage pack = ConfigPackage.read(output.resolve("map.bytes"));
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
    void mergedNonPrimarySeqFieldsAllowIndependentPermutations() throws Exception {
        Path baseDir = Files.createDirectories(root.resolve("base-data"));
        Context base = TestCtx.newContext(baseDir, """
                table item[id] { id:int; rank:int (seq); display:int (seq); [rank]; [display]; }
                """, Map.of("item", "id,rank,display\nid,rank,display\n1,0,0\n2,1,1\n3,2,2\n"));
        Path baseOut = root.resolve("base-pack");
        new BytesGenerator(new ParameterParser("bytes,dir:" + baseOut)).generate(base);

        Path modDir = Files.createDirectories(root.resolve("mod-data"));
        Files.writeString(modDir.resolve("item.csv"), "id,rank,display\nid,rank,display\n1,1,2\n2,0,1\n3,2,0\n");
        Path output = root.resolve("mod-pack");
        new BytesGenerator(new ParameterParser("bytes,pack:map,requires:base,deps:"
                + baseOut.resolve("base.bytes") + ",dir:" + output))
                .generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema()));

        var input = ConfigPackage.read(output.resolve("map.bytes")).block(".item");
        assertEquals(3, input.readInt());
        for (int value : new int[]{1, 1, 2, 2, 0, 1, 3, 2, 0}) {
            assertEquals(value, input.readInt(), "Neither seq field may reorder the owned records");
        }
        input.requireEnd();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, -1})
    void mergedSeqFieldsRejectDuplicateAndOutOfRangeValues(int rank) throws Exception {
        Path baseDir = Files.createDirectories(root.resolve("base-data"));
        Context base = TestCtx.newContext(baseDir, "table item[id] { id:int; rank:int (seq); }",
                Map.of("item", "id,rank\nid,rank\n1,0\n2,1\n"));
        Path baseOut = root.resolve("base-pack");
        new BytesGenerator(new ParameterParser("bytes,dir:" + baseOut)).generate(base);

        Path modDir = Files.createDirectories(root.resolve("mod-data"));
        Files.writeString(modDir.resolve("item.csv"), "id,rank\nid,rank\n1," + rank + "\n");
        var failure = assertThrows(CfgValueException.class,
                () -> new BytesGenerator(new ParameterParser("bytes,pack:map,requires:base,deps:"
                        + baseOut.resolve("base.bytes") + ",dir:" + root.resolve("mod-pack")))
                        .generate(new Context(Context.ContextCfg.of(modDir), base.cfgSchema())));
        assertEquals(1, failure.getErrs().errs().size());
        var error = assertInstanceOf(CfgValueErrs.SeqValueNotContinuous.class, failure.getErrs().errs().getFirst());
        assertEquals("rank", error.field());
        assertEquals(rank, error.expectedValue());
    }

    @Test
    void sourceSeqFieldsStillRequireSourceFileOrder() throws Exception {
        Path data = Files.createDirectories(root.resolve("source-data"));
        Context source = TestCtx.newContext(data, "table item[id] { id:int; rank:int (seq); }",
                Map.of("item", "id,rank\nid,rank\n1,1\n2,0\n"));

        var failure = assertThrows(CfgValueException.class, source::makeValue);
        assertEquals(2, failure.getErrs().errs().size());
        assertTrue(failure.getErrs().errs().stream().allMatch(CfgValueErrs.SeqValueNotContinuous.class::isInstance));
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
