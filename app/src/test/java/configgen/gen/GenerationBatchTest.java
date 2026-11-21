package configgen.gen;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.genbytes.BytesGenerator;
import configgen.gengo.GoCodeGenerator;
import configgen.genjson.JsonGenerator;
import configgen.util.CachedFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class GenerationBatchTest {
    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void generatesBytesAndGoInEitherOrderOnFirstRun(boolean bytesFirst) throws IOException {
        Context context = newContext();
        Path project = tempDir.resolve("project");
        Path output = project.resolve("config");
        String bytes = "bytes,dir:" + output;
        String go = "go,dir:" + project;

        Main.registerAllProviders();
        assertEquals(0, Main.runWithCatch(new String[]{"-datadir", context.rootDir().toString(),
                "-gen", bytesFirst ? bytes : go, "-gen", bytesFirst ? go : bytes}));
        assertTrue(Files.isRegularFile(output.resolve("base.bytes")));
        assertTrue(Files.isRegularFile(output.resolve("item.go")));
        assertTrue(Files.isRegularFile(output.resolve(".cfggen-output")));
    }

    @Test
    void refusesToClaimAnUnmarkedDirectoryWithExistingFiles() throws IOException {
        Context context = newContext();
        Path project = tempDir.resolve("project");
        Path output = Files.createDirectories(project.resolve("config"));
        Path manual = Files.writeString(output.resolve("notes.txt"), "manual");
        var bytes = new BytesGenerator(new ParameterParser("bytes,dir:" + output));
        var go = new GoCodeGenerator(new ParameterParser("go,dir:" + project));

        IOException error = assertThrows(IOException.class, () -> GenerationBatch.run(context, List.of(bytes, go)));

        assertTrue(error.getMessage().contains("has no regular output marker"));
        assertEquals("manual", Files.readString(manual));
        assertFalse(Files.exists(output.resolve(".cfggen-output")));
        assertFalse(Files.exists(output.resolve("item.go")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void waitsForTheFirstBatchToFinishBeforeWriting(boolean postRun) throws Exception {
        Context context = newContext();
        Path output = tempDir.resolve("json");
        var firstExport = new JsonGenerator(new ParameterParser("json,tables:item,dst:" + output));
        var firstHasWritten = new CountDownLatch(1);
        var finishFirst = new CountDownLatch(1);
        var secondSubmitted = new CountDownLatch(1);

        var pauseGenerator = new Generator(new ParameterParser("pause")) {
            @Override
            public void generate(Context ctx, CachedFiles outputs) throws IOException {
                try {
                    firstHasWritten.countDown();
                    assertTrue(finishFirst.await(10, SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException(e);
                }
            }
        };
        var secondExport = new JsonGenerator(new ParameterParser("json,tables:target,dst:" + output));
        var bytes = new BytesGenerator(new ParameterParser("bytes,dir:" + output));

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                if (postRun) {
                    synchronized (GenerationBatch.class) {
                        GenerationBatch.run(context, List.of(firstExport));
                        firstHasWritten.countDown();
                        assertTrue(finishFirst.await(10, SECONDS));
                    }
                } else {
                    GenerationBatch.run(context, List.of(firstExport, pauseGenerator));
                }
                return null;
            });
            try {
                assertTrue(firstHasWritten.await(10, SECONDS));
                var second = executor.submit(() -> {
                    secondSubmitted.countDown();
                    GenerationBatch.run(context, List.of(bytes, secondExport));
                    return null;
                });
                assertTrue(secondSubmitted.await(5, SECONDS));
                assertThrows(TimeoutException.class, () -> second.get(200, MILLISECONDS));
                assertTrue(Files.isRegularFile(output.resolve("_item/1.json")));
                assertFalse(Files.exists(output.resolve("base.bytes")));
                assertFalse(Files.exists(output.resolve("_target/2.json")));

                finishFirst.countDown();
                first.get(10, SECONDS);
                second.get(10, SECONDS);
            } finally {
                finishFirst.countDown();
                executor.shutdownNow();
            }
        }

        assertFalse(Files.exists(output.resolve("_item/1.json")));
        assertTrue(Files.isRegularFile(output.resolve("base.bytes")));
        assertTrue(Files.isRegularFile(output.resolve("_target/2.json")));
    }

    private Context newContext() throws IOException {
        return TestCtx.newContext(Files.createDirectory(tempDir.resolve("data")), """
                table item[id] { id:int; }
                table target[id] { id:int; }
                """, Map.of("item", "id\nid\n1\n", "target", "id\nid\n2\n"));
    }
}
