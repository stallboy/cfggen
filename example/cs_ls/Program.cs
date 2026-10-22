using Config;

static class Program
{
    public static void Main()
    {
        var config = Processor.LoadPackagesAndSetMgr(ConfigPackage.ReadFile("base.bytes"));
        ExampleChecks.Verify(config);
        var name = Config.Task.DTask.Get(1)!.Name[0];
        ExampleChecks.Check(name.Zh_cn == "杀个怪" && name.En == "kill a monster", "server text translations");
        
        Console.WriteLine("\n=== Test Data ===");
        Console.WriteLine(Config.Task.DTask.Get(1));
        Console.WriteLine("C# server text and internal references: ok");
    }
}
