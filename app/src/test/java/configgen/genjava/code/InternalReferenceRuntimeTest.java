package configgen.genjava.code;

import configgen.InternalReferenceFixture;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import configgen.genjava.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class InternalReferenceRuntimeTest {
    @TempDir Path root;

    @Test
    void bindsOccurrenceScopesCyclesAndFinalModObjectsWithinEachSnapshot() throws Exception {
        Path data = root.resolve("data");
        InternalReferenceFixture.create(data);
        Files.writeString(data.resolve("config.cfg"), """
                struct ConfigRelations { x:int; }
                table pair[a,b] (json) { a:int (seq); b:int; fixed:ConfigRelations; ->point:[] ->::fixed; }
                table extended[id] (json,enum='name',javaBase='hand.SymbolBase') { id:int; name:str; }
                table palette[id] (json) { id:int; ->symbol:[::id] ->extended; }
                """, StandardOpenOption.APPEND);
        InternalReferenceFixture.write(data, "pair", "{\"a\":0,\"b\":1,\"fixed\":{\"x\":7}}");
        InternalReferenceFixture.write(data, "extended", "{\"id\":1,\"name\":\"On\"}");
        InternalReferenceFixture.write(data, "palette", "{\"id\":1}");
        Context context = new Context(data);
        Path generated = root.resolve("generated");
        Files.createDirectories(generated.resolve("config"));
        Files.writeString(generated.resolve("config/$ConfigRelations$.java"), "// obsolete generated adapter");
        new JavaCodeGenerator(new ParameterParser("java,dir:" + generated + ",pkg:config")).generate(context);
        context.outputFiles().finalizeRun();
        assertFalse(Files.exists(generated.resolve("config/$ConfigRelations$.java")),
                "Relationship binding belongs in the existing loader, without an extra generated class");
        Files.createDirectories(generated.resolve("hand"));
        Files.writeString(generated.resolve("hand/SymbolBase.java"),
                "package hand; public class SymbolBase extends config.ConfigExtension {}\n");
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        InternalReferenceFixture.writeInvalidPackages(root.resolve("base/base.bytes"));
        List<String> args = new ArrayList<>(List.of("--release", "25", "-classpath", System.getProperty("java.class.path"), "-d", root.resolve("classes").toString()));
        try (var files = Files.walk(generated)) { files.filter(p -> p.toString().endsWith(".java")).forEach(p -> args.add(p.toString())); }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)));
        try (var loader = new URLClassLoader(new java.net.URL[]{root.resolve("classes").toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> loading = loader.loadClass("config.ConfigMgrLoader");
            ConfigPackage base = ConfigPackage.read(root.resolve("base/base.bytes"));
            for (String name : List.of("bad-map.bytes", "bad-index.bytes")) {
                var error = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> load(loading, ConfigPackage.read(root.resolve("base/" + name))));
                assertTrue(error.getCause().getMessage().toLowerCase(Locale.ROOT).contains("duplicate"), error.getCause().getMessage());
            }
            Object first = load(loading, base);
            Object second = load(loading, base);
            Object branch = check(first);
            assertNotSame(branch, check(second));
            Path mod = Files.createDirectories(root.resolve("mod"));
            InternalReferenceFixture.write(mod, "ability", "{\"id\":1,\"branches\":[{\"code\":\"left\",\"phases\":[{\"key\":\"A\",\"next\":\"A\"}],\"uses\":[{\"key\":\"A\"}]}]}");
            new BytesGenerator(new ParameterParser("bytes,pack:mod,requires:base,deps:" + root.resolve("base/base.bytes") + ",dir:" + root.resolve("modpack")))
                    .generate(new Context(Context.ContextCfg.of(mod), context.cfgSchema()));
            Object combined = load(loading, base, ConfigPackage.read(root.resolve("modpack/mod.bytes")));
            Object finalBranch = property(property(combined, "getCaller", 1), "refTarget");
            assertNotSame(branch, finalBranch);
            Object phase = ((List<?>) property(finalBranch, "getPhases")).getFirst();
            assertSame(phase, property(phase, "nullableRefNext"));
            check(first);
        }
    }

    private Object check(Object config) throws Exception {
        Object combat = property(config, "getCombatAbility", 1001);
        Object machine = ((List<?>) property(combat, "getMachines")).getFirst();
        assertSame(((List<?>) property(machine, "getPhases")).getFirst(), property(machine, "refInitial"));
        Object cast = property(config, "getCombatCast", 1);
        Object book = config.getClass().getMethod("getCombatPhasebook", String.class, int.class, boolean.class).invoke(config, "Fire", 0, false);
        assertSame(property(book, "getEntry"), property(cast, "refBook"));
        assertSame(book, property(cast, "refBookRecord"));
        Object outer = property(property(config, "getCombatEncounter", 1), "getCommand");
        var outerPhases = (List<?>) property(outer, "getPhases");
        Object inner = property(outerPhases.get(1), "getAction");
        var innerPhases = (List<?>) property(inner, "getPhases");
        Object missing = property(innerPhases.getFirst(), "getAction");
        Object local = property(innerPhases.get(1), "getAction");
        assertEquals(property(outerPhases.getFirst(), "getKey"), property(missing, "getKey"));
        assertNull(property(missing, "nullableRefKey"));
        assertSame(innerPhases.getFirst(), property(local, "nullableRefKey"));
        assertSame(property(config, "getExtended", 1), property(property(config, "getPalette", 1), "refSymbol"));
        Object addressed = property(config, "getAddressed", 1);
        Object dispatched = property(config, "getDispatched", 1);
        Object group = ((List<?>) property(dispatched, "getGroups")).getFirst();
        Object link = ((List<?>) property(group, "getLinks")).getFirst();
        assertSame(((List<?>) property(dispatched, "getItems")).getFirst(), property(link, "refIndexed"));
        assertSame(((List<?>) property(addressed, "getItems")).getFirst(), property(link, "refExternal"));
        assertSame(property(dispatched, "getAnchor"), property(link, "refFixed"));
        Object mapping = property(config, "getMapping", 1);
        assertSame(((Map<?, ?>) property(mapping, "getActions")).values().iterator().next(), property(mapping, "refMapped"));
        var branches = (List<?>) property(property(config, "getAbility", 1), "getBranches");
        for (Object branch : branches) {
            Object phase = ((List<?>) property(branch, "getPhases")).getFirst();
            Object use = ((List<?>) property(branch, "getUses")).getFirst();
            assertSame(phase, property(use, "refKey"));
        }
        Object first = ((List<?>) property(branches.getFirst(), "getPhases")).getFirst();
        assertSame(first, property(property(first, "nullableRefNext"), "nullableRefNext"));
        assertSame(branches.getFirst(), property(property(config, "getCaller", 1), "refTarget"));
        return branches.getFirst();
    }

    private static Object load(Class<?> loader, ConfigPackage... packages) throws Exception {
        return loader.getMethod("loadPackages", ConfigPackage[].class).invoke(null, (Object) packages);
    }

    private static Object property(Object value, String method, Object... arguments) throws Exception {
        return value.getClass().getMethod(method, arguments.length == 0 ? new Class<?>[0] : new Class<?>[]{int.class}).invoke(value, arguments);
    }

}
