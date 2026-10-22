package configgen;

import configgen.ctx.Context;
import configgen.genbytes.PackageSerializer;
import configgen.genjava.ConfigOutput;
import configgen.genjava.ConfigPackage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** The same containment and cycle fixture is compiled by each generated runtime. */
public final class InternalReferenceFixture {
    private InternalReferenceFixture() {}
    public static final String SCHEMA = """
            struct Phase { key:str; next:str ->@Branch::phases (nullable); }
            struct Use { key:str ->@Branch::phases; }
            struct Branch { code:str; phases:list<Phase> (index='key'); uses:list<Use>; }
            table ability[id] (json) { id:int; branches:list<Branch> (index='code'); }
            table caller[id] (json) {
                id:int; ability:int; key:str;
                ->target:[ability,key] ->ability::branches;
            }
            """;
    public static Context create(Path data) throws IOException {
        Files.createDirectories(data);
        Files.writeString(data.resolve("config.cfg"), SCHEMA);
        write(data, "ability", """
                {"id":1,"branches":[
                 {"code":"left","phases":[{"key":"A","next":"B"},{"key":"B","next":"A"}],"uses":[{"key":"A"}]},
                 {"code":"right","phases":[{"key":"A","next":""}],"uses":[{"key":"A"}]}]}
                """);
        write(data, "caller", "{\"id\":1,\"ability\":1,\"key\":\"left\"}");
        addAddressingCases(data);
        copyCombatExample(data);
        return new Context(data);
    }
    public static void copyCombatExample(Path data) throws IOException {
        Path source = Path.of("../example/config/combat");
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = data.resolve("combat").resolve(source.relativize(path));
                if (Files.isDirectory(path)) Files.createDirectories(target);
                else Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    public static void addAddressingCases(Path data) throws IOException {
        Files.writeString(data.resolve("config.cfg"), """
                struct Indexed { flag:bool; sequence:int; label:str; }
                struct AddressAnchor {}
                struct AddressLink {
                    sequence:int; label:str;
                    ->indexed:[@AddressGroup::flag,sequence,label] ->::items;
                    ->external:[::code,@AddressGroup::flag,sequence,label] ->addressed[code]::items;
                    ->fixed:[] ->::anchor;
                }
                struct AddressGroup { flag:bool; links:list<AddressLink>; }
                table addressed[id] (json) {
                    [code]; id:int; code:str; items:list<Indexed> (index='flag,sequence,label');
                    anchor:AddressAnchor; groups:list<AddressGroup>;
                }
                table dispatched[id] (json) {
                    id:int; code:str; items:list<Indexed> (index='flag,sequence,label');
                    anchor:AddressAnchor; groups:list<AddressGroup>;
                }
                struct AddressKey { label:str; variant:int; }
                interface Effect { struct Idle {} struct Cast { power:int; } }
                table mapping[id] {
                    id:int; key:AddressKey (pack); actions:map<AddressKey,Effect> (pack);
                    ->mapped:[key] ->::actions;
                }
                struct UnusedIndex { key:int; }
                table rawmap[id] { id:int; values:map<int,int> (pack); unused:list<UnusedIndex> (pack,index='key'); }
                """, StandardOpenOption.APPEND);
        String row = """
                {"id":1,"code":"target","items":[{"flag":false,"sequence":0,"label":""}],
                 "anchor":{},"groups":[{"flag":false,"links":[{"sequence":0,"label":""}]}]}
                """;
        write(data, "addressed", row);
        write(data, "dispatched", row);
        Files.writeString(data.resolve("mapping.csv"), "id,key,actions\nid,key,actions\n1,\"fire,2\",\"(fire,2),Cast(9)\"\n");
        Files.writeString(data.resolve("rawmap.csv"), "id,values,unused\nid,values,unused\n1,\"0,10\",(5)\n");
    }
    /** Keep the valid schema, corrupt only a table's payload. */
    public static void writeInvalidPackages(Path packagePath) throws IOException {
        var pack = ConfigPackage.read(packagePath);
        var blocks = new LinkedHashMap<String, byte[]>();
        for (String name : pack.blockNames()) {
            var input = pack.block(name);
            blocks.put(name, input.readRawBytes((int) input.remaining()));
        }
        for (String kind : List.of("map", "index")) {
            var bytes = new ByteArrayOutputStream();
            try (var out = new ConfigOutput(bytes)) {
                out.writeInt(1); out.writeInt(1); // one row, id=1
                out.writeInt(kind.equals("map") ? 2 : 1);
                out.writeInt(0); out.writeInt(10);
                if (kind.equals("map")) { out.writeInt(0); out.writeInt(20); }
                out.writeInt(kind.equals("index") ? 2 : 1);
                out.writeInt(5);
                if (kind.equals("index")) out.writeInt(5);
            }
            blocks.put(".rawmap", bytes.toByteArray());
            try (var out = new ConfigOutput(Files.newOutputStream(packagePath.resolveSibling("bad-" + kind + ".bytes")))) {
                PackageSerializer.write(out, pack.metadata(), blocks);
            }
        }
    }
    public static void write(Path data, String table, String json) throws IOException {
        Files.createDirectories(data.resolve("_" + table));
        Files.writeString(data.resolve("_" + table + "/1.json"), json);
    }
}
