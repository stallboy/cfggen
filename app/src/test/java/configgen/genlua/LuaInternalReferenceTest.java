package configgen.genlua;

import configgen.InternalReferenceFixture;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("lua")
class LuaInternalReferenceTest {
    @TempDir Path root;
    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void preservesOccurrenceIdentityAndCollectsReferenceCycles(boolean shared, boolean preload) throws Exception {
        Path data = root.resolve("data");
        InternalReferenceFixture.create(data);
        String schema = Files.readString(data.resolve("config.cfg")).replace("uses:list<Use>;", "uses:list<Use>; anchor:Anchor; empties:list<EmptyUse>;");
        Files.writeString(data.resolve("config.cfg"), schema + """
                struct Anchor {} struct EmptyUse { ->chosen:[] ->@Branch::anchor; }
                table rank[id] { id:int; name:str; }
                table keyed[key] { [code]; key:AddressKey (pack); code:str; }
                table emode[id] (enum='name') { id:int; name:str; }
                table flags[b] { a:bool; n:int; b:bool; }
                table legacy[id] { id:int; rank:int ->rank; links:list<int> ->rank (sep='|'); values:map<str,int> ->rank (pack); }
                struct LongPhase { key:long; }
                table precise[id] (json) { id:int; phases:list<LongPhase> (index='key'); selected:long ->::phases (nullable); }
                """);
        Files.writeString(data.resolve("rank.csv"), "id,name\nid,name\n2,R\n");
        Files.writeString(data.resolve("keyed.csv"), "key,code\nkey,code\n\"fire,2\",Fire\n");
        Files.writeString(data.resolve("emode.csv"), "id,name\nid,name\n1,Ready\n");
        Files.writeString(data.resolve("flags.csv"), "a,n,b\na,n,b\nfalse,7,true\n");
        Files.writeString(data.resolve("legacy.csv"), "id,rank,links,values\nid,rank,links,values\n1,2,2,\"a,2\"\n");
        InternalReferenceFixture.write(data, "precise", "{\"id\":1,\"phases\":[{\"key\":9007199254740993}],\"selected\":9007199254740992}");
        Path ability = data.resolve("_ability/1.json");
        Files.writeString(ability, Files.readString(ability).replace("\"code\":", "\"anchor\":{},\"empties\":[{},{}],\"code\":"));
        Path project = Files.createDirectories(root.resolve("project"));
        new LuaCodeGenerator(new ParameterParser("lua,packbool,dir:" + project + ",pkg:cfg,mkCfgDir:" + project.resolve("common")
                + (shared ? ",shared,sharedEmptyTable" : "") + (preload ? ",preload" : ""))).generate(new Context(data));
        Files.writeString(project.resolve("test.lua"), """
                require('common.mkcfg').btest = function(value, bit) return ((value >> bit) & 1) ~= 0 end
                local function unload()
                    for name in pairs(package.loaded) do if name:match('^cfg%.') then package.loaded[name] = nil end end
                end
                local function load()
                    local cfg = require 'cfg._cfgs'
                    require 'cfg._beans'
                    LOAD_TABLES
                    assert(cfg.rank.Metadata.fields[1] == 'id' and cfg.rank.Metadata.fields[2] == 'name')
                    assert(cfg.rank.Metadata.uniqkeys[1][3] == 1)
                    assert(cfg.emode.Metadata.enumidx == 2)
                    assert(cfg.flags.Metadata.uniqkeys[1][3] == 1)
                    assert(cfg.flags.get(true).a == false and cfg.flags.get(true).n == 7)
                    assert(#cfg.legacy.Metadata.refs == 3)
                    local added = cfg.rank._add(99, 'Temporary')
                    assert(rawequal(added, cfg.rank.get(99)))
                    cfg.rank._del(99)
                    assert(cfg.rank.get(99) == nil)
                    assert(rawequal(cfg.keyed.get(cfg._beans.addresskey('fire', 2)), cfg.keyed.getByCode('Fire')))
                    local dispatched = cfg.dispatched.get(1)
                    local link = dispatched.groups[1].links[1]
                    assert(rawequal(link.RefIndexed, dispatched.items[1]))
                    assert(rawequal(link.RefExternal, cfg.addressed.get(1).items[1]))
                    assert(rawequal(link.RefFixed, dispatched.anchor))
                    local mapping = cfg.mapping.get(1)
                    for _, action in pairs(mapping.actions) do assert(rawequal(action, mapping.RefMapped)) end
                    local branches = cfg.ability.get(1).branches
                    for _, branch in ipairs(branches) do
                        assert(rawequal(branch.phases[1], branch.uses[1].RefKey))
                        assert(rawequal(branch.anchor, branch.empties[1].RefChosen))
                        assert(not rawequal(branch.empties[1], branch.empties[2]))
                    end
                    assert(rawequal(branches[1].phases[1], branches[1].phases[1].NullableRefNext.NullableRefNext))
                    assert(branches[2].phases[1].NullableRefNext == nil)
                    assert(rawequal(branches[1], cfg.caller.get(1).RefTarget))
                    assert(cfg.precise.get(1).NullableRefSelected == nil)
                    assert(cfg.precise.get(1).phases[1].key == 9007199254740993)
                    local legacy = cfg.legacy.get(1)
                    assert(rawequal(legacy.RefRank, cfg.rank.get(2)))
                    assert(rawequal(legacy.RefLinks[1], cfg.rank.get(2)))
                    assert(rawequal(legacy.RefValues.a, cfg.rank.get(2)))
                    return cfg, branches[1]
                end
                local old, branch = load()
                unload()
                local current, other = load()
                assert(not rawequal(branch, other))
                assert(rawequal(branch.phases[1], branch.phases[1].NullableRefNext.NullableRefNext))
                local weak = setmetatable({branch}, {__mode='v'})
                old, branch = nil, nil
                collectgarbage('collect'); collectgarbage('collect')
                assert(weak[1] == nil, 'old snapshot leaked')
                current, other = nil, nil
                unload(); collectgarbage('collect'); collectgarbage('collect')
                print('runtime-ok')
                """.replace("LOAD_TABLES", preload ? "require 'cfg._loads'" : ""));
        run(project, "runtime-ok");
    }

    @ParameterizedTest
    @CsvSource({"ice,2,false", "fire,3,false", "fire,2,true"})
    void validatesUnreferencedStructMapKeysDuringLoading(String name, int variant, boolean duplicate) throws Exception {
        Path data = Files.createDirectories(root.resolve("data"));
        Files.writeString(data.resolve("config.cfg"), """
                struct MapKey { name:str; variant:int; }
                struct Point { x:int; }
                table holder[id] {
                    id:int; values:map<MapKey,int> (pack); point:Point (pack);
                    ->chosen:[] ->::point;
                }
                """);
        Files.writeString(data.resolve("holder.csv"), "id,values,point\nid,values,point\n1,\"(fire,2),10\",7\n");
        Path project = Files.createDirectories(root.resolve("project"));
        new LuaCodeGenerator(new ParameterParser("lua,dir:" + project + ",pkg:cfg,mkCfgDir:" + project.resolve("common")))
                .generate(new Context(data));
        Files.writeString(project.resolve("test.lua"), """
                local cfg = require 'cfg._cfgs'
                local beans = require 'cfg._beans'
                local complete = cfg[0].complete
                local inserted = false
                cfg[0].complete = function(name)
                    local row = cfg.holder.get(1)
                    local original = next(row.values)
                    assert(original.name == 'fire' and original.variant == 2)
                    local extra = beans.mapkey('%s', %d)
                    assert(not rawequal(extra, original))
                    row.values[extra] = 20
                    inserted = true
                    complete(name)
                end
                local ok, failure = pcall(require, 'cfg.holder')
                assert(inserted, 'struct map must be changed before relationship validation')
                if %s then
                    assert(not ok and tostring(failure):find('Duplicate map key:', 1, true), tostring(failure))
                else
                    assert(ok, tostring(failure))
                    local row = cfg.holder.get(1)
                    assert(rawequal(row.RefChosen, row.point))
                    local count = 0
                    for _ in pairs(row.values) do count = count + 1 end
                    assert(count == 2)
                end
                print('runtime-ok')
                """.formatted(name, variant, duplicate));
        run(project, "runtime-ok");
    }

    @Test
    void runsTheExistingGameExampleScriptWithInternalReferencesEnabled() throws Exception {
        var context = new Context(new Context.ContextCfg(Path.of("../example/config"), null,
                configgen.data.HeadRows.A2_Default, "GBK", null, null, null, false));
        Path project = Files.createDirectories(root.resolve("project"));
        new LuaCodeGenerator(new ParameterParser("lua,shared,sharedEmptyTable,dir:" + project + ",pkg:cfg,mkCfgDir:" + project.resolve("common"))).generate(context);
        Files.copy(Path.of("../example/lua/test.lua"), project.resolve("test.lua"));
        Files.copy(Path.of("../example/combat_assertions.lua"), root.resolve("combat_assertions.lua"));
        run(project, "ok");
    }

    private void run(Path project, String expectedOutput) throws Exception {
        List<String> command;
        String python = System.getProperty("cfggen.luaPython");
        if (python == null) command = List.of(System.getProperty("cfggen.lua", "lua"), "test.lua");
        else command = List.of(python, "-c", "import sys; sys.path.insert(0, sys.argv[1]); from lupa.lua54 import LuaRuntime; LuaRuntime().execute(open('test.lua',encoding='utf-8').read())",
                Path.of(System.getProperty("cfggen.luaModules", "build/lua-runtime")).toAbsolutePath().toString());
        Path log = project.resolve("runtime.log");
        var process = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Lua timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
        assertTrue(Files.readString(log).contains(expectedOutput));
    }
}
