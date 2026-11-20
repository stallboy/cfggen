package configgen.editorserver;

import configgen.JsonRecordFixture;
import configgen.ctx.Context;
import configgen.schema.TableSchemaRefGraph;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordRefIdsServiceTest {
    @Test
    void expandsOutgoingReferencesThroughAnIncomingResult(@TempDir Path data) throws IOException {
        var schema = CfgReader.parse("""
                table a[id] (json) { id:int; b:int ->b; }
                table b[id] (json) { id:int; a:int ->a; c:int ->c; }
                table c[id] (json) { id:int; }
                """);
        schema.resolve().checkErrors();
        JsonRecordFixture.write(data, "a", "{\"id\":1,\"b\":1}");
        JsonRecordFixture.write(data, "b", "{\"id\":1,\"a\":1,\"c\":1}");
        JsonRecordFixture.write(data, "c", "{\"id\":1}");

        var value = new Context(Context.ContextCfg.of(data), schema).makeValue();
        var graph = new TableSchemaRefGraph(value.schema());
        var refs = new RecordRefIdsService(value, graph, "a", "1", 1, 2, 3).retrieve().recordRefIds();
        assertEquals(List.of("a:0", "b:-1", "c:2"),
                refs.stream().map(ref -> ref.table() + ":" + ref.depth()).toList());
        assertEquals(2, new RecordRefIdsService(value, graph, "a", "1", 1, 2, 2)
                .retrieve().recordRefIds().size());
    }
}
