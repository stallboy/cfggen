using Config;

static class Program
{
    public static void Main()
    {
        Processor.LoadPackagesAndSetMgr(ConfigPackage.ReadFile("base.bytes"));
        
        Console.WriteLine("\n=== Test Data ===");
        Console.WriteLine(Config.Task.DTask.Get(1));
    }
}
