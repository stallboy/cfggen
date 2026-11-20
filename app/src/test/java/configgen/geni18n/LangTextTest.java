package configgen.geni18n;

import configgen.TestCtx;
import configgen.i18n.TextByIdFinder;
import configgen.i18n.TranslationCatalog;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LangTextTest {
    @Test
    void savesExtractedWorkbooksIntoNewLanguageDirectory(@TempDir Path root) throws Exception {
        var context = TestCtx.newContext(Files.createDirectory(root.resolve("data")), "table item[id] { id:int; label:text; }",
                Map.of("item", "id,label\nid,label\n1,Original\n"));
        Path output = root.resolve("translation/en");
        LangText.extract(context.makeValue()).save(output, new LangStat());

        assertTrue(Files.size(output.resolve("_top.xlsx")) > 0);
    @Test
    void extractsEachLanguageWithoutChangingSharedValues(@TempDir Path root) throws Exception {
        Path languages = Files.createDirectory(root.resolve("languages"));
        Files.writeString(languages.resolve("en.csv"), "item,Original,Translated\n");
        Files.writeString(languages.resolve("fr.csv"), "other,Other,Autre\n");
        var context = TestCtx.newLangSwitchContext(Files.createDirectory(root.resolve("data")),
                "table item[id] (lang='label') { id:int; label:text; }",
                Map.of("item", "id,label\nid,label\n1,Original\n"), languages, "zh_cn");
        var value = context.makeValue();
        var catalogs = context.nullableTranslationBundle().langMap();
        var english = extractRecord(value, catalogs.get("en"));
        assertEquals("Translated", english.texts().getFirst().translated());
        assertEquals("Translated", english.description());

        var french = extractRecord(value, catalogs.get("fr"));
        assertEquals("", french.texts().getFirst().translated());
        assertEquals("Original", french.description());
        var text = (VText) value.vTableMap().get("item").primaryKeyMap().values().iterator().next().values().get(1);
        assertEquals("Original", text.value());
        assertEquals("", text.translated());
    }

    private static TextByIdFinder.OneRecord extractRecord(CfgValue value, TranslationCatalog catalog) {
        return LangText.extract(value, catalog).get("_top").get("item").getPkToTexts().get("1");
    }
}
