package configgen.schema.cfg;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SkillSchemaExamplesTest {
    @Test
    void internalReferenceGuidanceContainsACompleteResolvableExample() throws Exception {
        var schema = CfgReader.parse(example());
        assertTrue(schema.resolve().errs().isEmpty());
        assertNotNull(schema.findTable("cast"));
        assertNotNull(schema.findTable("ability"));
    }

    @Test
    void theExampleRequiresTheDeclaredLocalIndex() throws Exception {
        var schema = CfgReader.parse(example().replace(" (index='key')", ""));
        assertFalse(schema.resolve().errs().isEmpty());
    }

    private static String example() throws Exception {
        String text = Files.readString(Path.of("../cfgdev/skills/cfggen-architect/references/internal-references.md"));
        var match = Pattern.compile("```cfg\\R(.*?)```", Pattern.DOTALL).matcher(text);
        assertTrue(match.find(), "The skill must provide a standalone CFG example");
        return match.group(1);
    }
}
