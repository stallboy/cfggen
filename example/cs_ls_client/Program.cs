using System;
using Config;

static class Program
{
    public static void Main()
    {
        var pack = ConfigPackage.ReadFile("base.bytes");
        var config = Processor.LoadPackagesAndSetMgr(pack);
        ExampleChecks.Verify(config);
        var name = Config.Task.DTask.Get(1)!.Name[0];
        
        Console.WriteLine("\n=== Test Data ===");
        var langNames = new[] { "zh_cn", "en" };
        Console.Out.WriteLine("language Count: " + langNames.Length);
        for (int i = 0; i < langNames.Length; i++) 
        {
            Console.Out.WriteLine(langNames[i]);
            TextPoolManager.Language = langNames[i];
            ExampleChecks.Check(name.T == (i == 0 ? "杀个怪" : "kill a monster"), "client language switch");
            Console.WriteLine(Config.Task.DTask.Get(1));
        }
        TextPoolManager.Language = "zh_cn";
        ExampleChecks.Check(name.T == "杀个怪", "switch back without reloading");
        Console.WriteLine("C# Unity syntax, client text and internal references: ok");
    }
}
