package configgen.tool;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValueInspectToolTest {
    @TempDir
    Path tempDir;

    private Context context() {
        return TestCtx.newContext(tempDir, "table t[id] { id:int; n:long; }",
                Map.of("t", "id,n\nid,n\n7,4294967297\n8,-9223372036854775808\n9,9223372036854775807\n"));
    }

    @Test
    void blankQueryRunsInteractivelyWithoutClosingHostInput() throws Exception {
        Context ctx = context();
        Path output = tempDir.resolve("interactive.txt");
        InputStream saved = System.in;
        ByteArrayInputStream input = new ByteArrayInputStream(
                "\u3000\nref t[\n\u3000INT\u20034294967297\nexit\n".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() {
                fail("The inspector must leave standard input open");
            }
        };
        try {
            System.setIn(input);
            new ValueInspectTool(new ParameterParser("search,to=" + output + ",q=\u3000")).generate(ctx);
            assertEquals(List.of("\uFEFFUsage: ref <refTable[key]> [ignoredTable1]...",
                    "t[7], n, 4294967297"), Files.readString(output).lines().toList());
        } finally {
            System.setIn(saved);
        }
    }
}
