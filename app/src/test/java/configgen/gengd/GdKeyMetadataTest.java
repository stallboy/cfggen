package configgen.gengd;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GdKeyMetadataTest {
    @Test
    void emitsValueKeyMetadataOnlyForActualStructKeys(@TempDir Path root) throws Exception {
        Context context = TestCtx.newContext(root, """
                struct PrimaryKey { value:int; }
                struct UniqueKey { value:int; }
                struct MapKey { value:int; }
                struct IndexKey { value:int; }
                struct Plain { value:int; }
                struct Item { key:IndexKey; }
                table t[primary] {
                    [unique];
                    primary:PrimaryKey (pack); unique:UniqueKey (pack);
                    mapping:map<MapKey,int> (pack); items:list<Item> (pack,index='key'); plain:Plain (pack);
                }
                """, java.util.Map.of("t", "primary,unique,mapping,items,plain\nprimary,unique,mapping,items,plain\n1,2,,,6\n"));
        Path generated = root.resolve("config");
        new GdCodeGenerator(new ParameterParser("gd,dir:" + generated)).generate(context);

        for (String type : List.of("PrimaryKey", "UniqueKey", "MapKey", "IndexKey")) {
            String source = Files.readString(generated.resolve("Data" + type + ".gd"));
            assertTrue(source.contains("_cfggen_key_fields"), type);
            assertTrue(source.contains("_cfggen_key_type"), type);
        }
        for (String type : List.of("Plain", "Item")) {
            String source = Files.readString(generated.resolve("Data" + type + ".gd"));
            assertFalse(source.contains("_cfggen_key_fields"), type);
            assertFalse(source.contains("_cfggen_key_type"), type);
        }
    }
}
