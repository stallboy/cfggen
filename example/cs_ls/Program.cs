using Config;

static class Program
{
    public static void Main()
    {
        ConfigLoadResult result = Loader.LoadFile("config.bytes", Processor.Process);
        
        Console.WriteLine("=== Issue ===");
        foreach (var issue in result.LoadIssues)
        {
            Console.WriteLine(issue);
        }
        
        Console.WriteLine("\n=== Test Data ===");
        Console.WriteLine(Config.Task.DTask.Get(1));
    }
}