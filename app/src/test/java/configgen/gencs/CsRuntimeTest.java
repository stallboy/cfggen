package configgen.gencs;

import configgen.TestCtx;
import configgen.ctx.Context;
import configgen.gen.ParameterParser;
import configgen.genbytes.BytesGenerator;
import configgen.genbytes.PackageSerializer;
import configgen.genjava.ConfigInput;
import configgen.genjava.ConfigOutput;
import configgen.genjava.ConfigPackage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit dotnetTest task: compiles and runs real generated C#, including the Unity C# 9 profile. */
@Tag("dotnet")
class CsRuntimeTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void singleLanguageUsesEachPackagesDefaultLanguage(boolean unity) throws Exception {
        Path project = Files.createDirectories(root.resolve("project"));
        for (String language : java.util.List.of("zh_cn", "en")) {
            boolean base = language.equals("zh_cn");
            Path data = Files.createDirectories(root.resolve(language));
            Context ctx = TestCtx.newContext(data, "table message[id] { id:int; desc:text; }",
                    Map.of("message", "id,desc\nid,desc\n" + (base ? "1,甲\n" : "1,B\n2,C\n")));
            if (base) new CsCodeGenerator(new ParameterParser("cs,dir:" + project + (unity ? ",unity" : ""))).generate(ctx);
            new BytesGenerator(new ParameterParser("bytes,dir:" + data)).generate(ctx);
            var pack = ConfigPackage.read(new ConfigInput(data.resolve("config.bytes")));
            var metadata = new java.util.LinkedHashMap<>(pack.metadata());
            metadata.put("defaultLanguage", language);
            if (!base) { metadata.put("pack", "mod"); metadata.put("requires", "base"); }
            var blocks = new java.util.LinkedHashMap<String, byte[]>();
            for (String block : pack.blockNames()) {
                var input = pack.block(block);
                blocks.put(block.equals("text/default") ? "text/" + language : block, input.readRawBytes((int) input.remaining()));
            }
            var bytes = new java.io.ByteArrayOutputStream();
            try (var out = new ConfigOutput(bytes)) { PackageSerializer.write(out, metadata, blocks); }
            Files.write(data.resolve("localized.bytes"), bytes.toByteArray());
        }
        Files.writeString(project.resolve("runtime.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk"><PropertyGroup>
                <OutputType>Exe</OutputType><TargetFramework>net10.0</TargetFramework>
                <Nullable>enable</Nullable><ImplicitUsings>disable</ImplicitUsings><LangVersion>%s</LangVersion>
                <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
                </PropertyGroup></Project>
                """.formatted(unity ? "9.0" : "latest"));
        Files.writeString(project.resolve("Program.cs"), """
                using System;
                using Config;
                class Program {
                    static void Main(string[] args) {
                        var original = Processor.LoadPackages(ConfigPackage.ReadFile(args[0]));
                        var pack = ConfigPackage.ReadFile(args[1]);
                        var first = pack.Block(".message"); var second = pack.Block(".message");
                        if (!ReferenceEquals(first.CaptureTextPool(), second.CaptureTextPool())) throw new Exception("pools not cached");
                        var baseConfig = Processor.LoadPackages(pack);
                        var combined = Processor.LoadPackages(pack, ConfigPackage.ReadFile(args[2]));
                        if (DMessage.Get(original, 1)!.Desc != "甲" || DMessage.Get(baseConfig, 1)!.Desc != "甲"
                            || DMessage.Get(combined, 1)!.Desc != "B" || DMessage.Get(combined, 2)!.Desc != "C") throw new Exception("wrong default language");
                        if (combined.Explain("message", 1).Count != 2 || combined.Explain("message", "1").Count != 0
                            || combined.Explain("unknown", 1).Count != 0) throw new Exception("wrong origins");
                        Console.WriteLine("runtime-ok");
                    }
                }
                """);
        runProject(project, root.resolve("dotnet.log"), root.resolve("zh_cn/config.bytes").toString(),
                root.resolve("zh_cn/localized.bytes").toString(), root.resolve("en/localized.bytes").toString());
    }

    @ParameterizedTest
    @CsvSource({"false,false,false", "true,false,false", "false,true,false", "true,true,false", "false,false,true", "false,true,true"})
    void instancesReferencesAndExtensionsRemainIndependent(boolean unity, boolean server, boolean implicitUsings) throws Exception {
        String schema = """
                table rank[id] (enum='name') { id:int; name:str; multiplier:int; }
                enum mode { First = 7; Second = 12; }
                table rankgroup[id] { id:int; multiplier:int =>rank[multiplier]; }
                struct EmptyValue { }
                table hollow[id] { id:int; values:list<EmptyValue> (pack); }
                struct Cached { value:int; }
                table caches[id] { id:int; values:map<int,Cached> (pack); }
                interface Tree {
                    struct Leaf { }
                    struct Branch { child:Tree; }
                    struct Pair { left:Tree; right:Tree; }
                }
                table forest[id] { id:int; root:Tree (pack); }
                interface Solo { struct Only { value:int; } }
                table only[id] { id:int; value:Solo (pack); }
                table arguments[id] { id:int; key:int; value:int; i:int; reader:int; errors:int; config:int; entries:map<int,int> (pack); }
                table signal[name] (enum='name') { name:str; power:int; }
                table endpoint[name] (entry='name') { name:str; power:int; }
                table empty[id] (entry='name') { id:int; name:str; }
                table flag[id] (enum='name') { id:int; name:str; }
                table monster[id] (javaBase='hand.IgnoredByCSharp') {
                    id:int; attack:int; rank:int ->rank; tags:list<str> (sep='|'); desc:text;
                }
                struct Key (javaBase='hand.KeyBase') { rank:int ->rank; }
                interface Choice {
                    struct Wrapped { key:Key; }
                    struct None { }
                }
                table holder[id] { id:int; links:map<Key,int> (pack); list:list<Key> (pack); choices:list<Choice> (pack); }
                table step[id] { id:int (seq); name:str; }
                """;
        Path aDir = Files.createDirectories(root.resolve("a"));
        Path bDir = Files.createDirectories(root.resolve("b"));
        for (Path dir : java.util.List.of(aDir, bDir)) {
            Files.writeString(dir.resolve("holder.csv"), "id,links,list,choices\nid,links,list,choices\n1,\"(10),1\",(10),\"Wrapped((10)),None()\"\n");
            Files.writeString(dir.resolve("step.csv"), "id,name\nid,name\n0,zero\n1,one\n");
            Files.writeString(dir.resolve("signal.csv"), "name,power\nname,power\n" + (dir.equals(aDir) ? "High,3\nLow,2\n" : "Low,20\nHigh,30\n"));
            Files.writeString(dir.resolve("endpoint.csv"), "name,power\nname,power\nStart,5\nEnd,9\n");
            Files.writeString(dir.resolve("empty.csv"), "id,name\nid,name\n1,\n");
            Files.writeString(dir.resolve("flag.csv"), "id,name\nid,name\n");
            Files.writeString(dir.resolve("rankgroup.csv"), "id,multiplier\nid,multiplier\n1,2\n");
            Files.writeString(dir.resolve("hollow.csv"), "id,values\nid,values\n1,\"(),()\"\n");
            Files.writeString(dir.resolve("caches.csv"), "id,values\nid,values\n1,\"1,(7),2,(7)\"\n");
            Files.writeString(dir.resolve("forest.csv"), "id,root\nid,root\n1,\"Branch(Pair(Leaf(),Leaf()))\"\n");
            Files.writeString(dir.resolve("only.csv"), "id,value\nid,value\n1,Only(7)\n");
            Files.writeString(dir.resolve("arguments.csv"), "id,key,value,i,reader,errors,config,entries\nid,key,value,i,reader,errors,config,entries\n1,1,2,3,4,5,6,\"1,2\"\n");
            Path other = Files.createDirectories(dir.resolve("other"));
            Files.writeString(other.resolve("other.cfg"), """
                    table pair[id,code] { id:int; code:str; name:str; [name]; }
                    table preset[id] (entry='name') { id:int; name:str; power:int; }
                    """);
            Files.writeString(other.resolve("pair.csv"), "id,code,name\nid,code,name\n1,a," + (dir.equals(aDir) ? "old\n" : "changed\n2,b,new\n"));
            Files.writeString(other.resolve("preset.csv"), "id,name,power\nid,name,power\n1,Home," + (dir.equals(aDir) ? "10\n" : "20\n"));
        }
        Path languages = Files.createDirectories(root.resolve("languages"));
        Files.writeString(languages.resolve("en.csv"), "monster,甲,A\nmonster,乙,B\n");
        Context a = TestCtx.newLangSwitchContext(aDir, schema, Map.of(
                "rank", "id,name,multiplier\nid,name,multiplier\n10,Green,2\n",
                "monster", "id,attack,rank,tags,desc\nid,attack,rank,tags,desc\n1,10,10,a|b,甲\n"), languages, "zh_cn");
        Context b = TestCtx.newLangSwitchContext(bDir, schema, Map.of(
                "rank", "id,name,multiplier\nid,name,multiplier\n10,Green,3\n",
                "monster", "id,attack,rank,tags,desc\nid,attack,rank,tags,desc\n1,20,10,new,乙\n"), languages, "zh_cn");
        Path project = Files.createDirectories(root.resolve("project"));
        new CsCodeGenerator(new ParameterParser("cs,dir:" + project + (unity ? ",unity" : "") + (server ? ",serverText" : "") + (implicitUsings ? ",implicitUsings" : ""))).generate(a);
        String processor = Files.readString(project.resolve("Config/Processor.cs"));
        assertEquals(1, processor.lines().filter(line -> line.contains("\"zh_cn\"")).count());
        assertFalse(processor.contains("params string[] languages"));
        new BytesGenerator(new ParameterParser("bytes,dir:" + aDir)).generate(a);
        new BytesGenerator(new ParameterParser("bytes,dir:" + bDir)).generate(b);
        // Empty structs occupy zero bytes; encode two explicitly to exercise collection count bounds.
        for (Path directory : java.util.List.of(aDir, bDir)) {
            rewritePackage(directory, "config", blocks -> blocks.put(".hollow", buffer(new byte[12]).putInt(1).putInt(1).putInt(2).array()));
        }
        rewritePackage(aDir, "bad-symbol", blocks -> buffer(blocks.get(".rank")).putInt(8, buffer(blocks.get(".other.preset")).getInt(8)));
        rewritePackage(aDir, "bad-rank-id", blocks -> buffer(blocks.get(".rank")).putInt(4, 11));
        rewritePackage(aDir, "bad-entry-id", blocks -> buffer(blocks.get(".other.preset")).putInt(4, 9));
        rewritePackage(aDir, "bad-mode-id", blocks -> buffer(blocks.get(".mode")).putInt(8, 999));
        rewritePackage(aDir, "bad-missing", blocks -> blocks.put(".rank", new byte[4]));
        rewritePackage(aDir, "bad-cache", blocks -> buffer(blocks.get(".monster")).putInt(8, -1));
        rewritePackage(aDir, "bad-reference", blocks -> buffer(blocks.get(".monster")).putInt(12, 999));
        rewritePackage(aDir, "bad-nested-reference", blocks -> {
            buffer(blocks.get(".holder")).putInt(12, 999).putInt(24, 999);
        });
        rewritePackage(aDir, "bad-count", blocks -> buffer(blocks.get(".holder")).putInt(20, -1));
        rewritePackage(aDir, "bad-pool", blocks -> buffer(blocks.get(".monster")).putInt(20, Integer.MAX_VALUE));
        rewritePackage(aDir, "bad-variant", blocks -> buffer(blocks.get(".holder")).putInt(32, buffer(blocks.get(".other.preset")).getInt(8)));
        rewritePackage(aDir, "bad-single-variant", blocks -> buffer(blocks.get(".only")).putInt(8, buffer(blocks.get(".other.preset")).getInt(8)));
        rewritePackage(aDir, "bad-schema", blocks -> blocks.put("schema", buffer(new byte[8]).putInt(11).putInt(0).array()));
        rewritePackage(aDir, "bad-extra", blocks -> blocks.put(".monster", java.util.Arrays.copyOf(blocks.get(".monster"), blocks.get(".monster").length + 1)));
        rewritePackage(aDir, "bad-indices", blocks -> {
            byte[] original = blocks.get(".other.pair");
            var duplicate = buffer(new byte[28]);
            duplicate.putInt(2).put(original, 4, 12).putInt(2).put(original, 8, 8);
            blocks.put(".other.pair", duplicate.array());
            buffer(blocks.get(".step")).putInt(4, 9);
        });
        rewritePackage(aDir, "bad-map", blocks -> {
            byte[] original = blocks.get(".holder");
            var duplicate = buffer(new byte[original.length + 16]);
            duplicate.put(original, 0, 8).putInt(3);
            for (int i = 0; i < 3; i++) duplicate.put(original, 12, 8);
            duplicate.put(original, 20, original.length - 20);
            blocks.put(".holder", duplicate.array());
        });
        rewritePackage(aDir, "bad-duplicates", blocks -> {
            byte[] original = blocks.get(".other.pair");
            var duplicate = buffer(new byte[40]); duplicate.putInt(3);
            for (int i = 0; i < 3; i++) duplicate.put(original, 4, 12);
            blocks.put(".other.pair", duplicate.array());
        });
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("basepack") + ",pack:base,langSeparated")).generate(a);
        Files.writeString(bDir.resolve("step.csv"), "id,name\nid,name\n3,three\n2,two\n");
        Context mod = new Context(b.contextCfg(), a.cfgSchema());
        new BytesGenerator(new ParameterParser("bytes,dir:" + root.resolve("modpack") + ",pack:map,langSeparated,requires:base,deps:" + root.resolve("basepack/config.bytes"))).generate(mod);
        Files.writeString(project.resolve("runtime.csproj"), """
                <Project Sdk="Microsoft.NET.Sdk"><PropertyGroup>
                <OutputType>Exe</OutputType><TargetFramework>net10.0</TargetFramework>
                <Nullable>enable</Nullable><ImplicitUsings>%s</ImplicitUsings><LangVersion>%s</LangVersion>
                <TreatWarningsAsErrors>true</TreatWarningsAsErrors>
                </PropertyGroup></Project>
                """.formatted(implicitUsings ? "enable" : "disable", unity ? "9.0" : "latest"));
        Files.writeString(project.resolve("MonsterExtension.cs"), """
                namespace Config {
                    public partial class DEmptyValue : IConfigExtension {
                        public int Calls;
                        void IConfigExtension.OnConfigReady(ConfigMgr config) { Calls++; }
                    }
                    public partial class DCached : IConfigExtension {
                        public static int ReadyCalls;
                        public int Calls;
                        void IConfigExtension.OnConfigReady(ConfigMgr config) { Calls++; ReadyCalls++; }
                    }
                    public partial class DRankInfo : IConfigExtension {
                        public int Calls;
                        void IConfigExtension.OnConfigReady(ConfigMgr config) { Calls++; }
                    }
                    public partial class DKey : IConfigExtension {
                        public static int ReadyCalls;
                        public int Calls { get; private set; }
                        public int Multiplier { get; private set; }
                        void IConfigExtension.OnConfigReady(ConfigMgr config) { Calls++; ReadyCalls++; Multiplier = RefRank.Info(config).Multiplier; }
                    }
                    public partial class DMonster : IConfigExtension {
                        public static int ReadyCalls;
                        public int Power { get; private set; }
                        public int Calls { get; private set; }
                        public ConfigMgr Owner { get; private set; } = null!;
                        void IConfigExtension.OnConfigReady(ConfigMgr config) {
                            if (Attack < 0) throw new System.InvalidOperationException("cache failed");
                            Owner = config; Calls++; ReadyCalls++; Power = Attack * RefRank.Info(config).Multiplier;
                        }
                    }
                }
                namespace Config.Choice {
                    public partial class DWrapped : Config.IConfigExtension {
                        public int Calls;
                        void Config.IConfigExtension.OnConfigReady(Config.ConfigMgr config) { Calls++; }
                    }
                    public partial class DNone : Config.IConfigExtension {
                        public int Calls;
                        void Config.IConfigExtension.OnConfigReady(Config.ConfigMgr config) { Calls++; }
                    }
                }
                namespace Config.Tree {
                    public partial class DLeaf : Config.IConfigExtension {
                        public static int ReadyCalls;
                        public int Calls;
                        void Config.IConfigExtension.OnConfigReady(Config.ConfigMgr config) { Calls++; ReadyCalls++; }
                    }
                    public partial class DBranch : Config.IConfigExtension {
                        public static int ReadyCalls;
                        public int Calls;
                        void Config.IConfigExtension.OnConfigReady(Config.ConfigMgr config) { Calls++; ReadyCalls++; }
                    }
                    public partial class DPair : Config.IConfigExtension {
                        public static int ReadyCalls;
                        public int Calls;
                        void Config.IConfigExtension.OnConfigReady(Config.ConfigMgr config) { Calls++; ReadyCalls++; }
                    }
                }
                namespace Config.Solo {
                    public partial class DOnly : Config.IConfigExtension {
                        public int Calls;
                        void Config.IConfigExtension.OnConfigReady(Config.ConfigMgr config) { Calls++; }
                    }
                }
                """);
        String program = """
                using System;
                using System.Collections.Generic;
                using Config;
                class Program {
                    static ConfigMgr Load(string path) {
                        return Processor.LoadPackages(ConfigPackage.ReadFile(path));
                    }
                    static void Check(bool condition, [System.Runtime.CompilerServices.CallerLineNumber] int line = 0) { if (!condition) throw new Exception("runtime assertion failed at line " + line); }
                    static void Main(string[] args) {
                        var a = Load(args[0]); var b = Load(args[1]);
                        Check(DHollow.Get(a, 1)!.Values.Count == 2 && DHollow.Get(a, 1)!.Values[0].Calls == 1 && DHollow.Get(a, 1)!.Values[1].Calls == 1);
                        Check(DCaches.Get(a, 1)!.Values[1].Calls == 1 && DCaches.Get(a, 1)!.Values[2].Calls == 1);
                        var branch = (Config.Tree.DBranch)DForest.Get(a, 1)!.Root;
                        var pair = (Config.Tree.DPair)branch.Child;
                        Check(branch.Calls == 1 && pair.Calls == 1 && ((Config.Tree.DLeaf)pair.Left).Calls == 1 && ((Config.Tree.DLeaf)pair.Right).Calls == 1);
                        Check(((Config.Solo.DOnly)DOnly.Get(a, 1)!.Value).Calls == 1);
                        Check(DRankgroup.Get(a, 1)!.ListRefMultiplier.Count == 1 && DRankgroup.Get(b, 1)!.ListRefMultiplier.Count == 0);
                        Check(DArguments.Get(a, 1)!.Reader == 4 && DArguments.Get(a, 1)!.Entries[1] == 2);
                        var raw = new ConfigReader(BitConverter.GetBytes(2)); Check(raw.ReadCount(0) == 2); raw.RequireEnd();
                        var reusable = ConfigPackage.ReadFile(args[0]);
                        var first = reusable.Block(".monster", "zh_cn", "en"); var second = reusable.Block(".monster", "zh_cn", "en");
                        Check(ReferenceEquals(first.CaptureTextPool(), second.CaptureTextPool()));
                        Check(first.ReadInt32() == 1 && second.ReadInt32() == 1 && !ReferenceEquals(first.Errors, second.Errors));
                        Check((object)a._rank_Symbols is DRankInfo[]);
                        Check((int)DRank.Green == 0 && DRank.Green.Info(a).Id == 10 && DRank.Green.Info(a).Calls == 1);
                        Check(typeof(ConfigMgr).GetField("_other_dpreset_Symbol_Home", System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic) != null);
                        var x = DMonster.Get(a, 1)!; var y = DMonster.Get(b, 1)!;
                        Check(x.Power == 20 && y.Power == 60 && x.Calls == 1 && y.Calls == 1);
                        Check(ReferenceEquals(x.Owner, a) && ReferenceEquals(y.Owner, b));
                        foreach (var key in DHolder.Get(a, 1)!.Links.Keys) Check(key.Calls == 1 && key.Multiplier == 2);
                        Check(DHolder.Get(a, 1)!.List[0].Calls == 1);
                        Check(((Config.Choice.DWrapped)DHolder.Get(a, 1)!.Choices[0]).Key.Calls == 1);
                        Check(((Config.Choice.DWrapped)DHolder.Get(a, 1)!.Choices[0]).Calls == 1);
                        Check(((Config.Choice.DNone)DHolder.Get(a, 1)!.Choices[1]).Calls == 1);
                        ConfigMgr.SetMgr(b);
                        Check(ReferenceEquals(DMonster.Get(1), y));
                        Check(DRank.Green.Info(a).Multiplier == 2 && DRank.Green.Info().Multiplier == 3);
                        Check(DSignal.High.Info(a).Power == 3 && DSignal.High.Info(b).Power == 30);
                        Check(DSignal.Low.Info(a).Power == 2 && DSignal.Low.Info(b).Power == 20);
                        Check(DEndpoint.StartIn(a).Power == 5 && DEndpoint.EndIn(b).Power == 9);
                        int failedKeys = DKey.ReadyCalls, failedCaches = DCached.ReadyCalls;
                        foreach (var test in new (string, Type[])[] {
                            ("bad-symbol", new[] { typeof(LoadError.UnknownSymbol) }),
                            ("bad-rank-id", new[] { typeof(LoadError.ChangedSymbolId), typeof(LoadError.MissingSymbol) }),
                            ("bad-entry-id", new[] { typeof(LoadError.ChangedSymbolId), typeof(LoadError.MissingSymbol) }),
                            ("bad-mode-id", new[] { typeof(LoadError.ChangedSymbolId) }),
                            ("bad-missing", new[] { typeof(LoadError.MissingSymbol) }),
                            ("bad-reference", new[] { typeof(LoadError.MissingReference) }),
                            ("bad-nested-reference", new[] { typeof(LoadError.MissingReference), typeof(LoadError.MissingReference) }),
                            ("bad-count", new[] { typeof(LoadError.InvalidCount) }),
                            ("bad-pool", new[] { typeof(LoadError.InvalidPoolIndex) }),
                            ("bad-variant", new[] { typeof(LoadError.UnknownVariant) }),
                            ("bad-single-variant", new[] { typeof(LoadError.UnknownVariant) }),
                            ("bad-schema", new[] { typeof(LoadError.SchemaMismatch) }),
                            ("bad-extra", new[] { typeof(LoadError.UnconsumedBytes) }),
                            ("bad-indices", new[] { typeof(LoadError.DuplicateUniqueKey), typeof(LoadError.InvalidSeq) }),
                            ("bad-map", new[] { typeof(LoadError.DuplicateKey), typeof(LoadError.DuplicateKey) }),
                            ("bad-duplicates", new[] { typeof(LoadError.DuplicateRecord), typeof(LoadError.DuplicateRecord) }) }) {
                            try {
                                Processor.LoadPackagesAndSetMgr(ConfigPackage.ReadFile(System.IO.Path.Combine(System.IO.Path.GetDirectoryName(args[0])!, test.Item1 + ".bytes")));
                                throw new Exception("invalid package was accepted: " + test.Item1);
                            } catch (ConfigException failure) {
                                Check(failure.Errors.Count == test.Item2.Length);
                                var actual = new List<Type>();
                                foreach (var error in failure.Errors) actual.Add(error.GetType());
                                foreach (var expected in test.Item2) Check(actual.Remove(expected));
                                foreach (var error in failure.Errors) {
                                    if (error is LoadError.MissingReference missing) {
                                        Check(missing.Target == "rank" && (int)missing.Key == 999);
                                        Check(missing.Source == (test.Item1 == "bad-reference" ? "monster" : "Key"));
                                    }
                                    if (error is LoadError.DuplicateUniqueKey duplicate) Check(duplicate.Table == "other.pair" && duplicate.Packs.Count == 1 && failure.Message.Contains("Packs = [base]"));
                                    if (error is LoadError.InvalidSeq seq) Check(seq.Table == "step" && seq.Value == 9 && seq.Size == 2);
                                }
                            }
                            Check(ReferenceEquals(ConfigMgr.GetMgr(), b));
                            Check(DKey.ReadyCalls == failedKeys && DCached.ReadyCalls == failedCaches);
                            Check(DRank.Green.Info().Multiplier == 3 && Config.Other.DPreset.Home.Power == 20);
                        }
                        try {
                            Processor.LoadPackagesAndSetMgr(ConfigPackage.ReadFile(System.IO.Path.Combine(System.IO.Path.GetDirectoryName(args[0])!, "bad-cache.bytes")));
                            throw new Exception("bad cache was accepted");
                        } catch (InvalidOperationException failure) { Check(failure.Message == "cache failed"); }
                        Check(ReferenceEquals(ConfigMgr.GetMgr(), b));
                        var collector = new LoadErrors();
                        collector.MissingReference("source", "field", "target", 1);
                        var snapshot = collector.Errors;
                        try { collector.CheckErrors(); } catch (ConfigException failure) {
                            collector.MissingSymbol("table", "symbol", 2);
                            Check(snapshot.Count == 1 && failure.Errors.Count == 1 && collector.Errors.Count == 2);
                            try { ((IList<LoadError>)failure.Errors).Clear(); throw new Exception("mutable errors"); } catch (NotSupportedException) { }
                        }
                        var basePack = ConfigPackage.ReadFile(args[2]).Attach(ConfigPackage.ReadFile(System.IO.Path.Combine(System.IO.Path.GetDirectoryName(args[2])!, "en.bytes")));
                        var modPack = ConfigPackage.ReadFile(args[3]).Attach(ConfigPackage.ReadFile(System.IO.Path.Combine(System.IO.Path.GetDirectoryName(args[3])!, "en.bytes")));
                        int readyBefore = DMonster.ReadyCalls, keysBefore = DKey.ReadyCalls, cachedBefore = DCached.ReadyCalls;
                        int branchesBefore = Config.Tree.DBranch.ReadyCalls, leavesBefore = Config.Tree.DLeaf.ReadyCalls, pairsBefore = Config.Tree.DPair.ReadyCalls;
                        var combined = Processor.LoadPackages(basePack, modPack);
                        Check(DMonster.ReadyCalls == readyBefore + 1 && DKey.ReadyCalls == keysBefore + 3);
                        Check(DCached.ReadyCalls == cachedBefore + 2 && DRank.Green.Info(combined).Calls == 1);
                        Check(Config.Tree.DBranch.ReadyCalls == branchesBefore + 1 && Config.Tree.DLeaf.ReadyCalls == leavesBefore + 2 && Config.Tree.DPair.ReadyCalls == pairsBefore + 1);
                        Check(DMonster.Get(combined, 1)!.Power == 60);
                        Check(DRank.Green.Info(combined).Multiplier == 3 && DRank.Green.Info(a).Multiplier == 2);
                        foreach (var key in DHolder.Get(combined, 1)!.Links.Keys) Check(key.Calls == 1 && key.Multiplier == 3);
                        Check(ReferenceEquals(DMonster.Get(1), y));
                        Check(combined.Explain("monster", 1).Count == 2);
                        Check(combined.Explain("monster", "1").Count == 0);
                        Check(combined.Explain("monster", 1L).Count == 0);
                        Check(combined.Explain("monster", (int?)1).Count == 2);
                        Check(combined.Explain<string?>("monster", null).Count == 0);
                        Check(combined.Explain("monster", 999).Count == 0);
                        Check(combined.Explain("unknown", 1).Count == 0);
                        Check(combined.Explain("signal", "High").Count == 2);
                        try { ((IList<string>)combined.Explain("monster", 1))[0] = "mutated"; throw new Exception("mutable origins"); }
                        catch (NotSupportedException) { }
                        Check(Config.Other.DPair.Get(a, 1, "a")!.Name == "old");
                        Check(Config.Other.DPair.Get(combined, 1, "a")!.Name == "changed");
                        Check(Config.Other.DPair.GetByName(combined, "old") == null);
                        Check(combined.Explain("other.pair", new Config.Other.DPair.IdCodeKey(1, "a")).Count == 2);
                        Check(Config.Other.DPreset.HomeIn(a).Power == 10 && Config.Other.DPreset.HomeIn(combined).Power == 20);
                        ConfigMgr.SetMgr(combined);
                        Check(DRank.Green.Info().Multiplier == 3 && Config.Other.DPreset.Home.Power == 20);
                        ConfigMgr.SetMgr(a);
                        Check(DRank.Green.Info().Multiplier == 2 && Config.Other.DPreset.Home.Power == 10);
                        Check(DRank.Green.Info(combined).Multiplier == 3 && Config.Other.DPreset.HomeIn(b).Power == 20);
                        ConfigMgr.SetMgr(b);
                        Check(DStep.All(combined).Count == 4 && DStep.All(combined)[2].Id == 2 && DStep.All(combined)[3].Id == 3);
                        Check(x.Desc.T == "甲" && y.Desc.T == "乙" && DMonster.Get(combined, 1)!.Desc.T == "乙");
                        TextPoolManager.Language = "en";
                        Check(x.Desc.T == "A" && y.Desc.T == "B" && DMonster.Get(combined, 1)!.Desc.T == "B");
                        try { ((ICollection<string>)x.Tags).Add("mutated"); throw new Exception("mutable collection exposed"); }
                        catch (NotSupportedException) { }
                        Console.WriteLine("runtime-ok");
                    }
                }
                """;
        if (server) {
            int switchPosition = program.indexOf("TextPoolManager.Language = \"en\";");
            program = program.substring(0, switchPosition).replace(".Desc.T", ".Desc.Zh_cn")
                    + program.substring(switchPosition).replace(".Desc.T", ".Desc.En");
        }
        Files.writeString(project.resolve("Program.cs"), program);
        Path log = root.resolve("dotnet.log");
        runProject(project, log, aDir.resolve("config.bytes").toString(), bDir.resolve("config.bytes").toString(),
                root.resolve("basepack/config.bytes").toString(), root.resolve("modpack/config.bytes").toString());
    }

    private static void runProject(Path project, Path log, String... arguments) throws Exception {
        var command = new java.util.ArrayList<>(java.util.List.of("dotnet", "run", "--project", project.resolve("runtime.csproj").toString(),
                "--configuration", "Release", "--verbosity", "quiet", "--"));
        command.addAll(java.util.List.of(arguments));
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(90, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("dotnet timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
        assertTrue(Files.readString(log).contains("runtime-ok"));
    }

    private static java.nio.ByteBuffer buffer(byte[] bytes) {
        return java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    }

    private static void rewritePackage(Path directory, String name, java.util.function.Consumer<Map<String, byte[]>> mutate) throws Exception {
        var pack = ConfigPackage.read(new ConfigInput(directory.resolve("config.bytes")));
        var blocks = new java.util.LinkedHashMap<String, byte[]>();
        for (String block : pack.blockNames()) {
            var input = pack.block(block);
            blocks.put(block, input.readRawBytes((int) input.remaining()));
        }
        mutate.accept(blocks);
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new ConfigOutput(bytes)) { PackageSerializer.write(out, pack.metadata(), blocks); }
        Files.write(directory.resolve(name + ".bytes"), bytes.toByteArray());
    }
}
