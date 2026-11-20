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
    void searchesFullWidthIntegersAndWritesAsciiNumbers() throws Exception {
        Context ctx = context();
        Path output = tempDir.resolve("result.txt");
        Locale saved = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-EG"));
            new ValueInspectTool(new ParameterParser("search,to=" + output
                    + ",q=\u3000INT\u20031 0x100000001 -9223372036854775808 9223372036854775807 010\u3000"))
                    .generate(ctx);
            assertEquals("""
                    t[7], n, 4294967297
                    t[8], id, 8
                    t[8], n, -9223372036854775808
                    t[9], n, 9223372036854775807
                    """.strip().lines().toList(),
                    Files.readString(output).substring(1).lines().toList());
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, saved);
        }
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

    @Test
    void closesOutputAfterAnUnexpectedInteractiveFailure() throws Exception {
        Context ctx = context();
        Path output = tempDir.resolve("failed.txt");
        InputStream saved = System.in;
        byte[] command = "int 7\n".getBytes(StandardCharsets.UTF_8);
        InputStream input = new InputStream() {
            private boolean sent;

            @Override
            public int read(byte[] bytes, int offset, int length) {
                if (sent) {
                    throw new IllegalStateException("input failed");
                }
                System.arraycopy(command, 0, bytes, offset, command.length);
                sent = true;
                return command.length;
            }

            @Override
            public int read() {
                throw new AssertionError("Expected a buffered read");
            }
        };
        try {
            System.setIn(input);
            assertThrows(IllegalStateException.class,
                    () -> new ValueInspectTool(new ParameterParser("search,to=" + output)).generate(ctx));
            assertEquals("\uFEFFt[7], id, 7" + System.lineSeparator(), Files.readString(output));
        } finally {
            System.setIn(saved);
        }
    }
}
