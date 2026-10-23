package configgen.genjava.code;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import configgen.genbytes.PackageSerializer;
import configgen.genjava.ConfigInput;
import configgen.genjava.ConfigOutput;
import configgen.genjava.ConfigPackage;
import configgen.genjava.ConfigErr;
import configgen.genjava.LoadValueErrs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeExtensionTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void generatedSuperclassInitializesOnceAndKeepsInstanceContext(boolean sealed) throws Exception {
        Files.createDirectories(root.resolve("data"));
        writeHolder(root.resolve("data"));
        Context ctx = TestCtx.newContext(root.resolve("data"), """
                table monster[id] (javaBase='hand.MonsterBase') {
                    id:int;
                    attack:int;
                    tags:list<str> (sep='|');
                    rank:int ->rank;
                }
                table rank[id] (enum='name') { id:int; name:str; multiplier:int; }
                enum mode { First = 7; Second = 12; }
                table rankgroup[multiplier] { multiplier:int =>rank[multiplier]; }
                table signal[name] (enum='name') { name:str; power:int; }
                table entrance[name] (entry='name') { name:str; power:int; }
                struct Key (javaBase='hand.KeyBase') { rank:int ->rank; }
                struct LoadValueErrs { values:map<int,int> (pack); }
                interface tags.LoadValueErrs { struct Plain {} }
                interface Choice {
                    struct Direct (javaBase='hand.KeyBase') { rank:int ->rank; }
                    struct Wrapped { key:Key; }
                    struct None { }
                }
                table holder[id] { id:int =>monster[id]; links:map<Key,int> (pack); list:list<Key> (pack); choices:list<Choice> (pack); }
                table pair[id,code] { id:int; code:str; name:str; [name]; }
                table step[id] { id:int (seq); name:str; }
                table marker[id] (entry='name') { id:int; name:str; }
                table flag[id] (enum='name') { id:int; name:str; }
                """, Map.of("monster", "id,attack,tags,rank\nid,attack,tags,rank\n1,10,a|b,10\n", "rank", "id,name,multiplier\nid,name,multiplier\n10,Green,2\n"));
        Path generated = root.resolve("generated");
        Files.writeString(root.resolve("builders.txt"), "monster\n");
        new JavaCodeGenerator(new ParameterParser("java,dir:" + generated + ",configgenDir:" + generated + ",pkg:config,builders:" + root.resolve("builders.txt") + (sealed ? "" : ",noSealed"))).generate(ctx);
        assertFalse(Files.exists(generated.resolve("config/ConfigNode.java")));
        assertFalse(Files.exists(generated.resolve("config/ConfigLoader.java")));
        assertFalse(Files.readString(generated.resolve("config/ConfigMgrLoader.java")).contains("loaders.put(\"flag\""),
                "The known pure enum table has a block but needs no runtime loader");
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("bytes"))).generate(ctx);
        Files.createDirectories(generated.resolve("hand"));
        Files.writeString(generated.resolve("hand/MonsterBase.java"), """
                package hand;
                public abstract class MonsterBase extends config.ConfigExtension {
                    public abstract int getAttack();
                    private int power;
                    private int calls;
                    protected void onConfigReady() {
                        if (getAttack() < 0) throw new IllegalArgumentException("cache failed");
                        power = getAttack() * 2; calls++;
                    }
                    public int power() { return power; }
                    public int calls() { return calls; }
                    public config.ConfigMgr owner() { return config(); }
                }
                """);
        Files.writeString(generated.resolve("hand/KeyBase.java"), """
                package hand;
                public abstract class KeyBase extends config.ConfigExtension {
                    public abstract config.Rank refRank();
                    public int calls;
                    public int multiplier;
                    protected void onConfigReady() { multiplier = refRank().ref(config()).getMultiplier(); calls++; }
                }
                """);
        Path classes = root.resolve("classes");
        Files.createDirectories(classes);
        List<String> args = new ArrayList<>(List.of("--release", "25", "-classpath",
                Path.of(ConfigInput.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(),
                "-d", classes.toString()));
        try (var files = Files.walk(generated)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> args.add(p.toString()));
        }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)),
                "Generated Java plus handwritten superclass must compile");
        try (var loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> loading = loader.loadClass("config.ConfigMgrLoader");
            Object a = load(loading);
            Object b = load(loading);
            Class<?> mgr = loader.loadClass("config.ConfigMgr");
            Class<?> monster = loader.loadClass("config.Monster");
            assertThrows(NoSuchMethodException.class, () -> loader.loadClass("config.ConfigExtension").getDeclaredMethod("_initialize"));
            assertEquals(0, mgr.getFields().length, "Table containers must not be public replaceable fields");
            assertTrue(java.util.Arrays.stream(mgr.getDeclaredFields()).noneMatch(f -> f.getType() == ConfigInput.class
                    || f.getName().equals("sourceTables")), "Published configurations must not retain input bytes");
            assertEquals(0, monster.getDeclaredClasses().length, "Ordinary tables do not need loader subclasses");
            Object rowA = mgr.getMethod("getMonster", int.class).invoke(a, 1);
            Object rowB = mgr.getMethod("getMonster", int.class).invoke(b, 1);
            assertNotSame(rowA, rowB);
            assertEquals(20, monster.getMethod("power").invoke(rowA));
            assertEquals(1, monster.getMethod("calls").invoke(rowA));
            assertNotNull(monster.getMethod("refRank").invoke(rowA), "Builder-enabled tables must still resolve loaded references");
            Object builder = loader.loadClass("config.MonsterBuilder").getConstructor().newInstance();
            Object detached = builder.getClass().getMethod("build").invoke(builder);
            assertEquals(0, monster.getMethod("calls").invoke(detached));
            assertNull(monster.getMethod("owner").invoke(detached));
            assertSame(a, monster.getMethod("owner").invoke(rowA));
            assertNestedKeys(loader, mgr, a, 2);
            assertEquals("old", property(mgr.getMethod("getPair", int.class, String.class).invoke(a, 1, "a"), "getName"));
            mgr.getMethod("setMgr", mgr).invoke(null, b);
            assertSame(rowB, monster.getMethod("get", int.class).invoke(null, 1));
            Object rank = loader.loadClass("config.Rank").getEnumConstants()[0];
            Object marker = loader.loadClass("config.Marker_Entry").getField("MAIN").get(null);
            assertCachedSymbolRef(mgr, a, rank, "rank_All", 10);
            assertCachedSymbolRef(mgr, b, rank, "rank_All", 10);
            assertCachedSymbolRef(mgr, a, marker, "marker_All", 1);
            assertCachedSymbolRef(mgr, b, marker, "marker_All", 1);
            for (Object signal : loader.loadClass("config.Signal").getEnumConstants()) {
                assertCachedSymbolRef(mgr, b, signal, "signal_All", property(signal, "getName"));
            }
            Object entrance = loader.loadClass("config.Entrance_Entry").getField("START").get(null);
            assertCachedSymbolRef(mgr, b, entrance, "entrance_All", "Start");
            Object group = mgr.getMethod("getRankgroup", int.class).invoke(a, 2);
            assertEquals(List.of(rank), property(group, "listRefMultiplier"), "Symbol caches must be available during reference resolution");
            assertSame(property(rank, "ref"), rank.getClass().getMethod("ref", mgr).invoke(rank, b));
            assertSame(a, monster.getMethod("owner").invoke(rowA));
            @SuppressWarnings("unchecked") var tags = (List<String>) monster.getMethod("getTags").invoke(rowA);
            assertThrows(UnsupportedOperationException.class, () -> tags.add("mutate"));

            new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base") + ",pack:base")).generate(ctx);
            Files.createDirectories(root.resolve("moddata"));
            writeHolder(root.resolve("moddata"));
            Context mod = TestCtx.newContext(root.resolve("moddata"), ctx.cfgSchema().stringify(),
                    Map.of("monster", "id,attack,tags,rank\nid,attack,tags,rank\n1,30,new,10\n2,50,extra,10\n", "rank", "id,name,multiplier\nid,name,multiplier\n10,Green,3\n"));
            new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("mod") + ",pack:map,requires:base,deps:" + root.resolve("base/base.bytes"))).generate(mod);
            ConfigPackage basePack = ConfigPackage.read(root.resolve("base/base.bytes"));
            ConfigPackage modPack = ConfigPackage.read(root.resolve("mod/map.bytes"));
            assertTrue(basePack.blockNames().contains(".flag"), "Known pure enum blocks remain accepted without a loader");
            for (String block : List.of(".unexpected", ".marker_Entry", ".rank_Detail")) {
                var unknownTable = rewritePackage(modPack, blocks -> {
                    blocks.put(block, new byte[]{1});
                    blocks.put(".monster", new byte[]{1}); // The unknown block must fail before any table is decoded.
                });
                assertEquals(List.of(new LoadValueErrs.UnknownTableBlock("map", block)),
                        loadFailure(loading, basePack, unknownTable));
            }
            assertSame(b, mgr.getMethod("getMgr").invoke(null));
            var sparseWithTools = rewritePackage(modPack, blocks -> {
                blocks.keySet().removeIf(name -> name.startsWith("."));
                blocks.put("tools/checksum", new byte[]{1});
            });
            Object sparse = loading.getMethod("loadPackages", ConfigPackage[].class)
                    .invoke(null, (Object) new ConfigPackage[]{basePack, sparseWithTools});
            assertEquals(20, property(mgr.getMethod("getMonster", int.class).invoke(sparse, 1), "power"));
            var duplicate = duplicatePairRow(modPack);
            var duplicateFailure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> loading.getMethod("loadPackagesAndSetMgr", ConfigPackage[].class)
                            .invoke(null, (Object) new ConfigPackage[]{basePack, duplicate}));
            var duplicateErrors = assertInstanceOf(ConfigErr.class, duplicateFailure.getCause()).getErrors();
            assertEquals(1, duplicateErrors.size());
            assertInstanceOf(LoadValueErrs.DuplicateRecord.class, duplicateErrors.getFirst());
            assertSame(b, mgr.getMethod("getMgr").invoke(null));
            var invalid = invalidIndexes(modPack);
            var failureIndexes = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> loading.getMethod("loadPackagesAndSetMgr", ConfigPackage[].class)
                            .invoke(null, (Object) new ConfigPackage[]{basePack, invalid}));
            var indexErrors = assertInstanceOf(ConfigErr.class, failureIndexes.getCause()).getErrors();
            assertEquals(4, indexErrors.size(), indexErrors.toString());
            assertTrue(indexErrors.stream().anyMatch(e -> e instanceof LoadValueErrs.DuplicateUniqueKey d
                    && d.field().equals("name") && d.table().equals("pair")
                    && d.rowKey().toString().equals("(2,b)") && d.packs().equals(List.of("map"))));
            assertEquals(3, indexErrors.stream().filter(e -> e instanceof LoadValueErrs.InvalidSeq s
                    && s.field().equals("id") && s.table().equals("step")).count());
            assertSame(b, mgr.getMethod("getMgr").invoke(null));
            assertEquals(1, monster.getMethod("calls").invoke(rowB));
            // A valid build after failures on the same thread needs no global reset.
            assertNotNull(loading.getMethod("loadPackages", ConfigPackage[].class)
                    .invoke(null, (Object) new ConfigPackage[]{basePack, modPack}));
            try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
                var attempts = List.<java.util.concurrent.Callable<List<LoadValueErrs.Error>>>of(
                        () -> loadFailure(loading, basePack, duplicate),
                        () -> loadFailure(loading, basePack, invalid));
                var results = executor.invokeAll(attempts);
                assertEquals(1, results.get(0).get().size());
                assertEquals(4, results.get(1).get().size());
                assertSame(b, mgr.getMethod("getMgr").invoke(null));
            }
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.DuplicateKey.class, LoadValueErrs.DuplicateKey.class), basePack, rewritePackage(modPack, blocks -> {
                var original = new ConfigInput(blocks.get(".holder"));
                int count = original.readInt();
                int id = original.readInt();
                assertEquals(1, original.readInt());
                var first = original.readRawBytes(8);
                var rest = original.readRawBytes((int) original.remaining());
                var table = new java.io.ByteArrayOutputStream();
                try (var out = new ConfigOutput(table)) {
                    out.writeInt(count); out.writeInt(id); out.writeInt(3);
                    out.writeRawBytes(first); out.writeRawBytes(first); out.writeRawBytes(first); out.writeRawBytes(rest);
                }
                blocks.put(".holder", table.toByteArray());
            }));
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.UnknownSymbol.class, LoadValueErrs.UnknownSymbol.class),
                    basePack, rewritePackage(modPack, blocks -> {
                var pair = java.nio.ByteBuffer.wrap(blocks.get(".pair")).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                int unknownSymbol = pair.getInt(8); // A valid string-pool index for "a", outside the fixed symbol set.
                java.nio.ByteBuffer.wrap(blocks.get(".rank")).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(8, unknownSymbol);
                java.nio.ByteBuffer.wrap(blocks.get(".marker")).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(8, unknownSymbol);
            }));
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.ChangedSymbolId.class, LoadValueErrs.MissingSymbol.class),
                    rewritePackage(basePack, blocks -> java.nio.ByteBuffer.wrap(blocks.get(".rank"))
                            .order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(4, 11)));
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.ChangedSymbolId.class),
                    rewritePackage(basePack, blocks -> java.nio.ByteBuffer.wrap(blocks.get(".mode"))
                            .order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(8, 999)));
            java.util.function.Consumer<Map<String, byte[]>> incompatibleSchema = blocks -> {
                var bytes = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                bytes.putInt(configgen.genjava.Schema.INTERFACE).putInt(0);
                blocks.put("schema", bytes.array());
            };
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.SchemaMismatch.class, LoadValueErrs.SchemaMismatch.class),
                    rewritePackage(basePack, incompatibleSchema), rewritePackage(modPack, incompatibleSchema));
            var unknownChoice = rewritePackage(modPack, blocks -> {
                var table = java.nio.ByteBuffer.wrap(blocks.get(".holder")).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                int tag = java.nio.ByteBuffer.wrap(blocks.get(".pair")).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(8);
                table.putInt(32, tag); // First Choice tag, after the embedded map and list.
            });
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.UnknownVariant.class), basePack, unknownChoice);
            for (int offset : List.of(8, 20)) {
                assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.InvalidCount.class), basePack,
                        rewritePackage(modPack, blocks -> java.nio.ByteBuffer.wrap(blocks.get(".holder"))
                                .order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(offset, -1)));
            }
            assertPackageErrors(loading, mgr, b, List.of(LoadValueErrs.MissingReference.class, LoadValueErrs.MissingReference.class),
                    basePack, rewritePackage(modPack, blocks -> {
                        var table = java.nio.ByteBuffer.wrap(blocks.get(".holder")).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                        table.putInt(12, 999);
                        table.putInt(24, 999);
                    }));
            assertEquals(1, monster.getMethod("calls").invoke(rowB));
            Object combined = loading.getMethod("loadPackages", ConfigPackage[].class)
                    .invoke(null, (Object) new ConfigPackage[]{basePack, modPack});
            Object changed = mgr.getMethod("getMonster", int.class).invoke(combined, 1);
            Object added = mgr.getMethod("getMonster", int.class).invoke(combined, 2);
            assertEquals(60, monster.getMethod("power").invoke(changed));
            assertEquals(100, monster.getMethod("power").invoke(added));
            assertNestedKeys(loader, mgr, combined, 3);
            assertNestedKeys(loader, mgr, a, 2);
            assertSame(combined, monster.getMethod("owner").invoke(changed));
            Object baseRank = rank.getClass().getMethod("ref", mgr).invoke(rank, a);
            Object modRank = rank.getClass().getMethod("ref", mgr).invoke(rank, combined);
            assertEquals(2, property(baseRank, "getMultiplier"));
            assertEquals(3, property(modRank, "getMultiplier"));
            assertNotSame(modRank, property(rank, "ref"), "Loading another instance must not replace default symbol refs");
            mgr.getMethod("setMgr", mgr).invoke(null, combined);
            assertSame(modRank, property(rank, "ref"));
            mgr.getMethod("setMgr", mgr).invoke(null, b);
            assertEquals(2, property(property(rank, "ref"), "getMultiplier"));
            assertSame(baseRank, rank.getClass().getMethod("ref", mgr).invoke(rank, a));
            assertEquals(20, monster.getMethod("power").invoke(rowA));
            assertSame(rowB, monster.getMethod("get", int.class).invoke(null, 1), "Loading does not activate the new instance");
            assertEquals(List.of(), mgr.getMethod("explain", String.class, Object.class).invoke(a, "monster", 1));
            assertEquals(List.of("map"), mgr.getMethod("explain", String.class, Object.class)
                    .invoke(combined, "monster", 1));
            assertEquals("changed", property(mgr.getMethod("getPair", int.class, String.class).invoke(combined, 1, "a"), "getName"));
            assertNull(mgr.getMethod("getPairByName", String.class).invoke(combined, "old"));
            assertEquals("three", property(mgr.getMethod("getStep", int.class).invoke(combined, 3), "getName"));
            var pairKey = loader.loadClass("config.Pair$IdCodeKey").getConstructor(int.class, String.class).newInstance(1, "a");
            assertEquals(List.of("map"), mgr.getMethod("explain", String.class, Object.class).invoke(combined, "pair", pairKey));
            var sources = (List<?>) mgr.getMethod("explain", String.class, Object.class).invoke(combined, "pair", pairKey);
            assertThrows(UnsupportedOperationException.class, sources::clear);

            Files.writeString(root.resolve("moddata/step.csv"), "id,name\nid,name\n0,zero\n1,one\n2,two\n3,three\n");
            Context complete = new Context(mod.contextCfg(), ctx.cfgSchema());
            new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("replacement"))).generate(complete);
            var replacementPack = ConfigPackage.read(root.resolve("replacement/base.bytes"));
            Object rebuilt = loading.getMethod("loadPackages", ConfigPackage[].class).invoke(null, (Object) new ConfigPackage[]{replacementPack});
            mgr.getMethod("setMgr", mgr).invoke(null, rebuilt);
            assertEquals(20, monster.getMethod("power").invoke(rowA));
            Object reloaded = monster.getMethod("get", int.class).invoke(null, 1);
            assertNotSame(rowA, reloaded);
            assertEquals(60, monster.getMethod("power").invoke(reloaded));

            Files.writeString(root.resolve("moddata/monster.csv"), "id,attack,tags,rank\nid,attack,tags,rank\n1,-1,bad,10\n");
            Context invalidCache = new Context(mod.contextCfg(), ctx.cfgSchema());
            new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("fail") + ",pack:broken,requires:base,deps:" + root.resolve("base/base.bytes"))).generate(invalidCache);
            var broken = ConfigPackage.read(root.resolve("fail/broken.bytes"));
            Object published = mgr.getMethod("getMgr").invoke(null);
            var failure = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> loading.getMethod("loadPackagesAndSetMgr", ConfigPackage[].class)
                    .invoke(null, (Object) new ConfigPackage[]{basePack, broken}));
            assertEquals("cache failed", failure.getCause().getMessage());
            assertSame(published, mgr.getMethod("getMgr").invoke(null));
            assertEquals(20, monster.getMethod("power").invoke(rowA));
        }
    }

    private static void writeHolder(Path dir) throws Exception {
        Files.writeString(dir.resolve("holder.csv"), "id,links,list,choices\nid,links,list,choices\n1,\"(10),1\",(10),\"Direct(10),Wrapped((10)),None()\"\n");
        Files.writeString(dir.resolve("marker.csv"), "id,name\nid,name\n1,Main\n");
        Files.writeString(dir.resolve("rankgroup.csv"), "multiplier\nmultiplier\n2\n");
        Files.writeString(dir.resolve("signal.csv"), "name,power\nname,power\nHigh,3\nLow,2\n");
        Files.writeString(dir.resolve("entrance.csv"), "name,power\nname,power\nStart,5\nEnd,9\n");
        Files.writeString(dir.resolve("flag.csv"), "id,name\nid,name\n1,Enabled\n");
        boolean mod = dir.getFileName().toString().equals("moddata");
        Files.writeString(dir.resolve("pair.csv"), "id,code,name\nid,code,name\n1,a," + (mod ? "changed\n2,b,new\n" : "old\n"));
        Files.writeString(dir.resolve("step.csv"), "id,name\nid,name\n" + (mod ? "3,three\n2,two\n0,zero\n1,one\n" : "0,zero\n1,one\n"));
    }

    private static Object property(Object row, String name) throws Exception {
        return row.getClass().getMethod(name).invoke(row);
    }

    private static void assertCachedSymbolRef(Class<?> mgr, Object config, Object symbol, String table, Object key) throws Exception {
        var field = mgr.getDeclaredField(table);
        field.setAccessible(true);
        @SuppressWarnings("unchecked") var rows = (Map<Object, Object>) field.get(config);
        Object expected = rows.get(key);
        assertNotNull(expected);
        // Detect table lookups directly, without relying on unstable timing measurements.
        field.set(config, new java.util.AbstractMap<>() {
            @Override public java.util.Set<Entry<Object, Object>> entrySet() { return rows.entrySet(); }
            @Override public Object get(Object ignored) { throw new AssertionError("Symbol refs must be cached at load time"); }
        });
        try {
            for (int i = 0; i < 10; i++) {
                assertSame(expected, symbol.getClass().getMethod("ref", mgr).invoke(symbol, config));
                if (mgr.getMethod("getMgr").invoke(null) == config) assertSame(expected, property(symbol, "ref"));
            }
        } finally {
            field.set(config, rows);
        }
    }

    private static void assertPackageErrors(Class<?> loading, Class<?> mgr, Object published,
                                            List<Class<? extends LoadValueErrs.Error>> expected, ConfigPackage... packages) throws Exception {
        var errors = loadFailure(loading, packages);
        assertEquals(expected.stream().map(Class::getName).sorted().toList(),
                errors.stream().map(e -> e.getClass().getName()).sorted().toList());
        for (var error : errors) {
            if (error instanceof LoadValueErrs.UnknownVariant unknown) {
                assertEquals("Choice", unknown.union());
                assertEquals("a", unknown.tag());
            }
        }
        assertSame(published, mgr.getMethod("getMgr").invoke(null));
    }

    private static List<LoadValueErrs.Error> loadFailure(Class<?> loading, ConfigPackage... packages) {
        var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> loading.getMethod("loadPackagesAndSetMgr", ConfigPackage[].class).invoke(null, (Object) packages));
        return assertInstanceOf(ConfigErr.class, failure.getCause()).getErrors();
    }

    private static ConfigPackage rewritePackage(ConfigPackage pack, java.util.function.Consumer<Map<String, byte[]>> rewrite) {
        var blocks = new java.util.LinkedHashMap<String, byte[]>();
        for (String name : pack.blockNames()) {
            var block = pack.block(name);
            blocks.put(name, block.readRawBytes((int) block.remaining()));
        }
        rewrite.accept(blocks);
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new ConfigOutput(bytes)) { PackageSerializer.write(out, pack.metadata(), blocks); }
        return ConfigPackage.read(new ConfigInput(bytes.toByteArray()));
    }

    private static ConfigPackage duplicatePairRow(ConfigPackage pack) {
        var blocks = new java.util.LinkedHashMap<String, byte[]>();
        for (String name : pack.blockNames()) {
            var block = pack.block(name);
            blocks.put(name, block.readRawBytes((int) block.remaining()));
        }
        var pair = new ConfigInput(blocks.get(".pair"));
        int count = pair.readInt();
        var rows = pair.readRawBytes((int) pair.remaining());
        var tableBytes = new java.io.ByteArrayOutputStream();
        try (var out = new ConfigOutput(tableBytes)) {
            out.writeInt(count + 1);
            out.writeRawBytes(rows);
            out.writeRawBytes(rows, 0, 12); // Pair's three fields each occupy four bytes.
        }
        blocks.put(".pair", tableBytes.toByteArray());
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new ConfigOutput(bytes)) { PackageSerializer.write(out, pack.metadata(), blocks); }
        return ConfigPackage.read(new ConfigInput(bytes.toByteArray()));
    }

    private static ConfigPackage invalidIndexes(ConfigPackage pack) {
        var blocks = new java.util.LinkedHashMap<String, byte[]>();
        for (String name : pack.blockNames()) {
            var block = pack.block(name);
            blocks.put(name, block.readRawBytes((int) block.remaining()));
        }
        var pair = java.nio.ByteBuffer.wrap(blocks.get(".pair")).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        pair.putInt(24, pair.getInt(12)); // Two different primary keys share the first row's unique name.
        var step = java.nio.ByteBuffer.wrap(blocks.get(".step")).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        step.putInt(4, -1);
        step.putInt(12, 10);
        step.putInt(20, 11); // Three invalid seq rows; aggregation must avoid out-of-bounds list writes.
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new ConfigOutput(bytes)) { PackageSerializer.write(out, pack.metadata(), blocks); }
        return ConfigPackage.read(new ConfigInput(bytes.toByteArray()));
    }

    private static void assertNestedKeys(ClassLoader loader, Class<?> mgr, Object config, int multiplier) throws Exception {
        Object holder = mgr.getMethod("getHolder", int.class).invoke(config, 1);
        var keys = ((Map<?, ?>) holder.getClass().getMethod("getLinks").invoke(holder)).keySet();
        Class<?> base = loader.loadClass("hand.KeyBase");
        for (Object key : keys) {
            assertEquals(1, base.getField("calls").get(key));
            assertEquals(multiplier, base.getField("multiplier").get(key));
        }
        var list = (List<?>) holder.getClass().getMethod("getList").invoke(holder);
        assertEquals(1, base.getField("calls").get(list.getFirst()), "Value-equal embedded objects must each initialize once");
        var choices = (List<?>) holder.getClass().getMethod("getChoices").invoke(holder);
        Object direct = choices.get(0);
        Object wrapped = property(choices.get(1), "getKey");
        for (Object key : List.of(direct, wrapped)) {
            assertEquals(1, base.getField("calls").get(key));
            assertEquals(multiplier, base.getField("multiplier").get(key));
        }
    }

    private Object load(Class<?> loader) throws Exception {
        var pack = ConfigPackage.read(root.resolve("bytes/base.bytes"));
        return loader.getMethod("loadPackages", ConfigPackage[].class).invoke(null, (Object) new ConfigPackage[]{pack});
    }
}
