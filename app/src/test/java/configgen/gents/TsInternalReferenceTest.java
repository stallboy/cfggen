package configgen.gents;

import configgen.InternalReferenceFixture;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("otherRuntimes")
class TsInternalReferenceTest {
    @TempDir Path root;
    @Test
    void bindsTypedOccurrencesAcrossSnapshots() throws Exception {
        InternalReferenceFixture.create(root.resolve("data"));
        Files.writeString(root.resolve("data/config.cfg"), """
                struct LongPhase { key:long; }
                table precise[id] (json) { id:int; phases:list<LongPhase> (index='key'); selected:long ->::phases (nullable); }
                """, StandardOpenOption.APPEND);
        InternalReferenceFixture.write(root.resolve("data"), "precise", "{\"id\":1,\"phases\":[{\"key\":9007199254740993}],\"selected\":9007199254740992}");
        var context = new configgen.ctx.Context(root.resolve("data"));
        Path project = Files.createDirectories(root.resolve("project"));
        new TsCodeGenerator(new ParameterParser("ts,dir:" + project)).generate(context);
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("base"))).generate(context);
        InternalReferenceFixture.writeInvalidPackages(root.resolve("base/base.bytes"));
        Path compiler = Path.of("../cfgeditor/node_modules/typescript/bin/tsc").toAbsolutePath();
        run(project, List.of("node", compiler.toString(), "--target", "es2022", "--module", "commonjs", "--strict", "--exactOptionalPropertyTypes", "--skipLibCheck", "--outDir", "out", "Config.ts", "ConfigUtil.ts"));
        Files.writeString(project.resolve("test.cjs"), """
                const fs = require('fs'); const {Config} = require('./out/Config.js');
                const {ConfigPackage, LoadErrors} = require('./out/ConfigUtil.js');
                const read = () => { const b = fs.readFileSync(process.env.CFGGEN_FIXTURE); return ConfigPackage.Read(b.buffer.slice(b.byteOffset,b.byteOffset+b.byteLength)); };
                const check = ok => { if (!ok) throw Error('reference assertion'); };
                const load = () => { const errors = new LoadErrors(); Config.Processor.ProcessPackage(read(), errors); check(errors.Errors.length === 0); };
                for (const name of ['bad-map.bytes', 'bad-index.bytes']) {
                  const b = fs.readFileSync(require('path').join(require('path').dirname(process.env.CFGGEN_FIXTURE), name));
                  let rejected = false;
                  try { Config.Processor.ProcessPackage(ConfigPackage.Read(b.buffer.slice(b.byteOffset,b.byteOffset+b.byteLength)), new LoadErrors()); }
                  catch (error) { rejected = error.message.toLowerCase().includes('duplicate'); }
                  check(rejected);
                }
                const verify = () => {
                  const combat = Config.Combat_Ability.Get(1001), cast = Config.Combat_Cast.Get(1);
                  check(combat.Machines[0].RefInitial === combat.Machines[0].Phases[0]);
                  check(combat.Machines[0].RefFrame.Tick === 9007199254740993n);
                  const book = Config.Combat_Phasebook.Get('Fire', 0, false);
                  check(cast.RefBook === book.Entry && cast.RefBookRecord === book);
                  const outer = Config.Combat_Encounter.Get(1).Command, inner = outer.Phases[1].Action;
                  const missing = inner.Phases[0].Action, local = inner.Phases[1].Action;
                  check(outer.Phases[0].Key === missing.Key);
                  check(missing.NullableRefKey === undefined);
                  check(local.NullableRefKey === inner.Phases[0]);
                  const dispatched = Config.Dispatched.Get(1), link = dispatched.Groups[0].Links[0];
                  check(link.RefIndexed === dispatched.Items[0]);
                  check(link.RefExternal === Config.Addressed.Get(1).Items[0]);
                  check(link.RefFixed === dispatched.Anchor);
                  const mapping = Config.Mapping.Get(1);
                  check(mapping.RefMapped === [...mapping.Actions.values()][0]);
                  const branches = Config.Ability.Get(1).Branches;
                  for (const branch of branches) check(branch.Phases[0] === branch.Uses[0].RefKey);
                  check(branches[0].Phases[0] === branches[0].Phases[0].NullableRefNext.NullableRefNext);
                  check(branches[1].Phases[0].NullableRefNext === undefined);
                  check(branches[0] === Config.Caller.Get(1).RefTarget);
                  return branches[0];
                };
                load(); const old = verify(); load(); check(old !== verify());
                check(Config.Precise.Get(1).NullableRefSelected === undefined);
                check(Config.Precise.Get(1).Phases[0].Key === 9007199254740993n);
                check(old.Phases[0].NullableRefNext.NullableRefNext === old.Phases[0]);
                console.log('runtime-ok');
                """);
        run(project, List.of("node", "test.cjs"));
    }

    private void run(Path project, List<String> command) throws Exception {
        Path log = project.resolve("runtime.log");
        var builder = new ProcessBuilder(command).directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CFGGEN_FIXTURE", root.resolve("base/base.bytes").toString());
        var process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("runtime timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }
}
