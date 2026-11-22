package configgen.geni18n;

import configgen.TestCtx;
import configgen.i18n.TextByIdFinder;
import configgen.i18n.TodoFile;
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
        var context = TestCtx.newContext(Files.createDirectory(root.resolve("data")), """
                table item[id] { id:int; label:text; detail:text; }
                """, Map.of("item", "id,label,detail\nid,label,detail\n1,Original,\n2,Other,Detail\n"));
        Path output = root.resolve("translation/en");
        LangText extracted = LangText.extract(context.makeValue());
        extracted.save(output, new LangStat());

        assertTrue(Files.size(output.resolve("_top.xlsx")) > 0);
        var reloaded = LangText.ofFinder(TextByIdFinder.loadOneLang(output));
        assertEquals(extracted, reloaded);
        assertEquals(extracted.hashCode(), reloaded.hashCode());

        var todo = List.of(TodoFile.header(),
                new TodoFile.Line("item", "1", "label", "Original", "Translated"),
                new TodoFile.Line("item", "2", "label", "Other", ""));
        new TodoFile(todo, List.of(TodoFile.header())).save(output.resolveSibling("_todo_en.xlsx"));
        var finder = TextByIdFinder.loadOneLang(output).getTextFinder("item");
        assertNotNull(finder);
        assertEquals("Translated", finder.findText("1", List.of("label"), "Original"));
        assertNull(finder.findText("2", List.of("label"), "Other"));
    }

    @Test
    void extractsEachLanguageWithoutChangingSharedValues(@TempDir Path root) throws Exception {
        Path languages = Files.createDirectory(root.resolve("languages"));
        Files.writeString(languages.resolve("en.csv"), "item,Original,Translated\n");
        Files.writeString(languages.resolve("fr.csv"), "other,Other,Autre\n");
        var context = TestCtx.newLangSwitchContext(Files.createDirectory(root.resolve("data")),
                "table item[id] (lang='label') { id:int; label:text; }",
                Map.of("item", "id,label\nid,label\n1,Original\n"), languages, "zh_cn");
        var value = context.makeValue();
        var catalogs = context.translationBundle().langMap();
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
