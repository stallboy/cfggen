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
        var langNames = result.LangNames!;
        Console.Out.WriteLine("language Count: " + langNames.Length);
        for (int i = 0; i < langNames.Length; i++) 
        {
            Console.Out.WriteLine(langNames[i]);
            TextPoolManager.SetGlobalTexts(result.LangTextPools![i]);
            Console.WriteLine(Config.Task.DTask.Get(1));
        }
    }
}