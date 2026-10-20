using System;
using Config;

static class Program
{
    public static void Main()
    {
        var pack = ConfigPackage.ReadFile("config.bytes");
        Processor.LoadPackagesAndSetMgr(pack);
        
        Console.WriteLine("\n=== Test Data ===");
        var langNames = new[] { "zh_cn", "en" };
        Console.Out.WriteLine("language Count: " + langNames.Length);
        for (int i = 0; i < langNames.Length; i++) 
        {
            Console.Out.WriteLine(langNames[i]);
            TextPoolManager.Language = langNames[i];
            Console.WriteLine(Config.Task.DTask.Get(1));
        }
    }
}
