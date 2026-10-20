using Config;

using Config.Equip;
using Config.Other;
using Config.Task;
using Config.Task.Completecondition;

static class Program
{
    public static void XorBytesWithCipher(byte[] target, string cipher)
    {
        byte[] cipherBytes = System.Text.Encoding.UTF8.GetBytes(cipher);
        for (int i = 0; i < target.Length; i++)
        {
            target[i] = (byte)(target[i] ^ cipherBytes[i % cipherBytes.Length]);
        }
    }

    private static ConfigPackage ReadPackage(string path)
    {
        byte[] bytes = File.ReadAllBytes(path);
        XorBytesWithCipher(bytes, "xyz");
        return ConfigPackage.Read(bytes);
    }

    public static void Main()
    {
        var basePack = ReadPackage("base.bytes");
        var original = Processor.LoadPackagesAndSetMgr(basePack);
        var modified = Processor.LoadPackages(basePack, ReadPackage("demo-mod.bytes"));
        ConfigMgr.SetMgr(modified);
        var hunt = DTask.Get(1001)!;
        var condition = (DKillMonster)hunt.Completecondition;

        Console.WriteLine("\n=== Mod example ===");
        Console.WriteLine($"monster 1: base x={DMonster.Get(original, 1)!.PosList[0].X}, mod x={DMonster.Get(1)!.PosList[0].X}");
        Console.WriteLine($"monster 2 unchanged: x={DMonster.Get(2)!.PosList[0].X}");
        Console.WriteLine($"monster 1001: base={DMonster.Get(original, 1001)?.ToString() ?? "null"}, mod={DMonster.Get(1001)}");
        Console.WriteLine($"green: base={DRank.Green.Info(original).RankShowName}, mod={DRank.Green.Info().RankShowName}");
        Console.WriteLine($"task 1001: [{string.Join(", ", hunt.Name)}], target monster={condition.RefMonsterid.Id}, exp={hunt.Exp}");
        Console.WriteLine($"monster 1 sources=[{string.Join(", ", modified.Explain("other.monster", 1))}]");
        Console.WriteLine($"monster 1001 sources=[{string.Join(", ", modified.Explain("other.monster", 1001))}]");
    }
}
