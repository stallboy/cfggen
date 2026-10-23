package configgen.genjava.code;

import configgen.InternalReferenceFixture;
import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import configgen.genbytes.PackageSerializer;
import configgen.genjava.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static configgen.genjava.SchemaPrimitive.*;
import static org.junit.jupiter.api.Assertions.*;

class SchemaReadConsistencyTest {
    @TempDir Path root;

    private record Change(String name, Consumer<SchemaInterface> apply) {}

    @ParameterizedTest
    @CsvSource({"false,-1", "false,2", "true,-1", "true,2"})
    void validatesTheLayoutUsedByGeneratedReaders(boolean multilingual, int schemaNumPerFile) throws Exception {
        Context context = fixture(multilingual);
        Path generated = root.resolve("generated");
        new JavaCodeGenerator(new ParameterParser("java,pkg:fixture,dir:" + generated
                + ",schemaNumPerFile:" + schemaNumPerFile)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("bytes"))).generate(context);
        Path classes = compile(generated);
        try (var loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> loading = loader.loadClass("fixture.ConfigMgrLoader");
            ConfigPackage base = ConfigPackage.read(root.resolve("bytes/base.bytes"));
            SchemaInterface expected = (SchemaInterface) loader.loadClass("fixture.ConfigCodeSchema")
                    .getMethod("getPackageSchema").invoke(null);
            assertTrue(base.validateSchema(expected, new LoadValueErrs()));
            Object current = load(loading, base);
            verify(current, multilingual);

            // Named definitions and fixed symbols are looked up by name, independent of their schema order.
            ConfigPackage reordered = rewriteSchema(base, SchemaReadConsistencyTest::reverseDefinitions, false);
            assertTrue(reordered.validateSchema(expected, new LoadValueErrs()));
            current = load(loading, reordered);
            verify(current, multilingual);
            ConfigPackage reorderedBlocks = rewrite(base, SchemaReadConsistencyTest::reverse);
            assertTrue(reorderedBlocks.validateSchema(expected, new LoadValueErrs()));
            current = load(loading, reorderedBlocks);
            verify(current, multilingual);

            for (Change change : incompatibleLayouts()) {
                ConfigPackage changed = rewriteSchema(base, change.apply(), true);
                var errors = new LoadValueErrs();
                assertFalse(changed.validateSchema(expected, errors), change.name());
                assertEquals(1, errors.getErrors().size(), change.name());
                assertInstanceOf(LoadValueErrs.SchemaMismatch.class, errors.getErrors().getFirst(), change.name());
                assertFailure(loading, changed, LoadValueErrs.SchemaMismatch.class, change.name());
                assertSame(current, loader.loadClass("fixture.ConfigMgr").getMethod("getMgr").invoke(null), change.name());
            }

            // Schema validation checks the declared layout; payload errors belong to the reader.
            for (ChangeBody change : invalidBodies()) {
                ConfigPackage changed = rewrite(base, blocks -> change.apply().accept(blocks));
                assertTrue(changed.validateSchema(expected, new LoadValueErrs()), change.name());
                assertFailure(loading, changed, change.error(), change.name());
                assertSame(current, loader.loadClass("fixture.ConfigMgr").getMethod("getMgr").invoke(null), change.name());
            }
        }
    }

    private Context fixture(boolean multilingual) throws Exception {
        Path data = Files.createDirectories(root.resolve("data"));
        String schema = """
                struct EmptyValue {}
                struct Point { x:int; y:long; }
                struct AlternatePoint { x:int; y:long; }
                interface Choice { struct Number { value:long; } struct Label { value:str; } }
                interface Solo { struct Only { value:bool; } }
                table sample[id] (json) {
                    id:int; flag:bool; small:int; large:long; ratio:float; label:str; text:text;
                    point:Point; empties:list<EmptyValue>; longs:list<long>; words:list<text>;
                    choice:Choice; single:Solo;
                }
                table mappings[id] { id:int; points:map<str,Point> (pack); texts:map<str,text> (pack); }
                table colors[id] (json,enum='name') { id:int; name:str; }
                table ranks[id] (json,enum='name') { id:int; name:str; power:long; }
                table entries[id] (json,entry='name') { id:int; name:str; label:str; }
                table sequence[id] (json) { id:int (seq); value:long; }
                """;
        InternalReferenceFixture.write(data, "sample", """
                {"id":1,"flag":true,"small":-2147483648,"large":9007199254740993,"ratio":1.25,
                 "label":"字符串","text":"原文","point":{"x":7,"y":-9223372036854775808},
                 "empties":[{},{}],"longs":[-9223372036854775808,9223372036854775807],"words":["原文","末尾"],
                 "choice":{"$type":"Number","value":9223372036854775807},"single":{"$type":"Only","value":false}}
                """);
        InternalReferenceFixture.write(data, "colors", "{\"id\":1,\"name\":\"Red\"}");
        Files.writeString(data.resolve("_colors/2.json"), "{\"id\":2,\"name\":\"Blue\"}");
        InternalReferenceFixture.write(data, "ranks", "{\"id\":1,\"name\":\"Low\",\"power\":9007199254740993}");
        InternalReferenceFixture.write(data, "entries", "{\"id\":1,\"name\":\"Home\",\"label\":\"entry\"}");
        InternalReferenceFixture.write(data, "sequence", "{\"id\":0,\"value\":9223372036854775807}");
        Map<String, String> csv = Map.of("mappings", "id,points,texts\nid,points,texts\n1,\"p,(8,9223372036854775807)\",\"t,原文\"\n");
        if (!multilingual) return TestCtx.newContext(data, schema, csv);
        Path languages = Files.createDirectories(root.resolve("languages"));
        Files.writeString(languages.resolve("en.csv"), "sample,原文,hello\nsample,末尾,tail\nmappings,原文,hello\n");
        return TestCtx.newLangSwitchContext(data, schema, csv, languages, "zh_cn");
    }

    private Path compile(Path generated) throws Exception {
        Path classes = Files.createDirectories(root.resolve("classes"));
        var args = new ArrayList<>(List.of("--release", "25", "-encoding", "UTF-8", "-classpath",
                Path.of(ConfigInput.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(), "-d", classes.toString()));
        try (var files = Files.walk(generated)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> args.add(p.toString()));
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)));
        return classes;
    }

    private static void verify(Object config, boolean multilingual) throws Exception {
        Object row = config.getClass().getMethod("getSample", int.class).invoke(config, 1);
        assertEquals(true, property(row, "getFlag"));
        assertEquals(Integer.MIN_VALUE, property(row, "getSmall"));
        assertEquals(9007199254740993L, property(row, "getLarge"));
        assertEquals(1.25f, property(row, "getRatio"));
        assertEquals("字符串", property(row, "getLabel"));
        checkText(property(row, "getText"), multilingual, "原文", "hello");
        assertEquals(7, property(property(row, "getPoint"), "getX"));
        assertEquals(Long.MIN_VALUE, property(property(row, "getPoint"), "getY"));
        assertEquals(2, ((List<?>) property(row, "getEmpties")).size());
        assertEquals(List.of(Long.MIN_VALUE, Long.MAX_VALUE), property(row, "getLongs"));
        var words = (List<?>) property(row, "getWords");
        checkText(words.get(0), multilingual, "原文", "hello");
        checkText(words.get(1), multilingual, "末尾", "tail");
        Object mappings = config.getClass().getMethod("getMappings", int.class).invoke(config, 1);
        assertEquals(Long.MAX_VALUE, property(((Map<?, ?>) property(mappings, "getPoints")).get("p"), "getY"));
        checkText(((Map<?, ?>) property(mappings, "getTexts")).get("t"), multilingual, "原文", "hello");
        assertEquals(Long.MAX_VALUE, property(property(row, "getChoice"), "getValue"));
        assertEquals(false, property(property(row, "getSingle"), "getValue"));
        Object rank = config.getClass().getMethod("getRanks", int.class).invoke(config, 1);
        assertEquals(9007199254740993L, property(rank, "getPower"));
        Object entry = config.getClass().getMethod("getEntries", int.class).invoke(config, 1);
        assertEquals("entry", property(entry, "getLabel"));
        Object sequence = config.getClass().getMethod("getSequence", int.class).invoke(config, 0);
        assertEquals(Long.MAX_VALUE, property(sequence, "getValue"));
    }

    private static void checkText(Object value, boolean multilingual, String source, String translated) throws Exception {
        if (multilingual) {
            assertEquals(source, property(value, "getZh_cn"));
            assertEquals(translated, property(value, "getEn"));
        } else {
            assertEquals(source, value);
        }
    }

    private static List<Change> incompatibleLayouts() {
        var changes = new ArrayList<Change>();
        var fields = Map.of("flag", SBool, "small", SInt, "large", SLong, "ratio", SFloat, "label", SStr, "text", SText);
        fields.forEach((field, original) -> {
            for (var replacement : SchemaPrimitive.values()) {
                if (replacement != original) changes.add(new Change(field + ": " + original + " -> " + replacement,
                        schema -> column(bean(schema, "sample"), field, replacement)));
            }
        });
        changes.add(new Change("field order", schema -> Collections.swap(bean(schema, "sample").columns, 1, 2)));
        changes.add(new Change("missing field", schema -> bean(schema, "sample").columns.removeLast()));
        changes.add(new Change("extra field", schema -> bean(schema, "sample").addColumn("extra", SInt)));
        changes.add(new Change("renamed field", schema -> bean(schema, "sample").columns.set(0, new SchemaBean.Column("renamed", SInt))));
        changes.add(new Change("list element", schema -> column(bean(schema, "sample"), "longs", new SchemaList(SInt))));
        changes.add(new Change("list text pool", schema -> column(bean(schema, "sample"), "words", new SchemaList(SStr))));
        changes.add(new Change("map key", schema -> column(bean(schema, "mappings"), "points", new SchemaMap(SInt, new SchemaRef("Point")))));
        changes.add(new Change("map value", schema -> column(bean(schema, "mappings"), "texts", new SchemaMap(SStr, SStr))));
        changes.add(new Change("reference type", schema -> column(bean(schema, "sample"), "point", new SchemaRef("AlternatePoint"))));
        changes.add(new Change("nested field", schema -> column(bean(schema, "Point"), "y", SInt)));
        changes.add(new Change("missing definition", schema -> schema.implementations.remove("AlternatePoint")));
        changes.add(new Change("extra definition", schema -> schema.addImp("Extra", new SchemaBean(false))));
        changes.add(new Change("table marker", schema -> {
            var replacement = new SchemaBean(false); replacement.columns.addAll(bean(schema, "sample").columns);
            schema.implementations.put("sample", replacement);
        }));
        changes.add(new Change("union member", schema -> column((SchemaBean) union(schema, "Choice").implementations.get("Number"), "value", SInt)));
        changes.add(new Change("missing union branch", schema -> union(schema, "Choice").implementations.remove("Label")));
        changes.add(new Change("extra union branch", schema -> union(schema, "Choice").addImp("Extra", new SchemaBean(false))));
        changes.add(new Change("single union branch", schema -> column((SchemaBean) union(schema, "Solo").implementations.get("Only"), "value", SInt)));
        for (String name : List.of("colors", "ranks", "entries_Entry")) {
            changes.add(new Change(name + " fixed ID", schema -> symbols(schema, name).values.replaceAll((symbol, id) -> id + 1)));
            changes.add(new Change(name + " extra symbol", schema -> symbols(schema, name).addValue("Extra", 99)));
            changes.add(new Change(name + " missing symbol", schema -> symbols(schema, name).values.remove(symbols(schema, name).values.keySet().iterator().next())));
        }
        return changes;
    }

    private record ChangeBody(String name, Consumer<Map<String, byte[]>> apply, Class<? extends LoadValueErrs.Error> error) {}

    private static List<ChangeBody> invalidBodies() {
        // Count + id + bool + int + long + float: label's pool index starts at byte 25.
        return List.of(
                new ChangeBody("truncated table", blocks -> blocks.put(".sample", new byte[]{0}), LoadValueErrs.OutOfBounds.class),
                new ChangeBody("trailing table bytes", blocks -> blocks.put(".sample", Arrays.copyOf(blocks.get(".sample"), blocks.get(".sample").length + 1)), LoadValueErrs.UnconsumedBytes.class),
                new ChangeBody("invalid string-pool index", blocks -> ByteBuffer.wrap(blocks.get(".sample")).order(ByteOrder.LITTLE_ENDIAN).putInt(25, Integer.MAX_VALUE), LoadValueErrs.InvalidPoolIndex.class));
    }

    private static void assertFailure(Class<?> loading, ConfigPackage pack, Class<? extends LoadValueErrs.Error> errorType, String name) {
        var failure = assertThrows(InvocationTargetException.class, () -> load(loading, pack), name);
        var errors = assertInstanceOf(ConfigErr.class, failure.getCause(), name).getErrors();
        assertEquals(1, errors.size(), name);
        assertInstanceOf(errorType, errors.getFirst(), name);
    }

    private static Object load(Class<?> loading, ConfigPackage pack) throws Exception {
        return loading.getMethod("loadPackagesAndSetMgr", ConfigPackage[].class).invoke(null, (Object) new ConfigPackage[]{pack});
    }

    private static Object property(Object value, String method) throws Exception {
        return value.getClass().getMethod(method).invoke(value);
    }

    private static SchemaBean bean(SchemaInterface schema, String name) { return (SchemaBean) schema.implementations.get(name); }
    private static SchemaInterface union(SchemaInterface schema, String name) { return (SchemaInterface) schema.implementations.get(name); }
    private static SchemaEnum symbols(SchemaInterface schema, String name) { return (SchemaEnum) schema.implementations.get(name); }

    private static void column(SchemaBean bean, String name, Schema type) {
        for (int i = 0; i < bean.columns.size(); i++) {
            if (bean.columns.get(i).name().equals(name)) {
                bean.columns.set(i, new SchemaBean.Column(name, type));
                return;
            }
        }
        throw new AssertionError(name);
    }

    private static void reverseDefinitions(SchemaInterface schema) {
        reverse(schema.implementations);
        for (Schema child : schema.implementations.values()) {
            if (child instanceof SchemaInterface union) reverseDefinitions(union);
            else if (child instanceof SchemaEnum symbols) reverse(symbols.values);
        }
    }

    private static <T> void reverse(Map<String, T> map) {
        var entries = new ArrayList<>(map.entrySet());
        map.clear();
        for (var entry : entries.reversed()) map.put(entry.getKey(), entry.getValue());
    }

    private static ConfigPackage rewriteSchema(ConfigPackage pack, Consumer<SchemaInterface> change, boolean poisonBody) {
        return rewrite(pack, blocks -> {
            SchemaInterface schema = (SchemaInterface) SchemaDeserializer.deserialize(new ConfigInput(blocks.get("schema")));
            change.accept(schema);
            blocks.put("schema", encode(out -> new SchemaSerializer(out).serializeInterface(schema)));
            if (poisonBody) blocks.put(".sample", new byte[]{0});
        });
    }

    private static ConfigPackage rewrite(ConfigPackage pack, Consumer<Map<String, byte[]>> change) {
        var blocks = new LinkedHashMap<String, byte[]>();
        for (String name : pack.blockNames()) {
            var input = pack.block(name);
            blocks.put(name, input.readRawBytes((int) input.remaining()));
        }
        change.accept(blocks);
        return ConfigPackage.read(new ConfigInput(encode(out -> PackageSerializer.write(out, pack.metadata(), blocks))));
    }

    private static byte[] encode(Consumer<ConfigOutput> write) {
        var stream = new ByteArrayOutputStream();
        try (var out = new ConfigOutput(stream)) { write.accept(out); }
        return stream.toByteArray();
    }
}
