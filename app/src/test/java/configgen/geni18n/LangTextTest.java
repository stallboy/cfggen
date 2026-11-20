package configgen.geni18n;

import configgen.TestCtx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LangTextTest {
    @Test
    void savesExtractedWorkbooksIntoNewLanguageDirectory(@TempDir Path root) throws Exception {
        var context = TestCtx.newContext(Files.createDirectory(root.resolve("data")), "table item[id] { id:int; label:text; }",
                Map.of("item", "id,label\nid,label\n1,Original\n"));
        Path output = root.resolve("translation/en");
        LangText.extract(context.makeValue()).save(output, new LangStat());

        assertTrue(Files.size(output.resolve("_top.xlsx")) > 0);
    }
}
