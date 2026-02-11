using Config;
using Stream = Config.Stream;

class Program
{
    public static void Main()
    {
        byte[] bytes = File.ReadAllBytes("config.bytes");

        
        Config.LoadErrors errs = new Config.LoadErrors();
        Config.Stream stream = Config.Loader.LoadBytes(bytes, Config.Processor.Process, errs);

        // 打印警告信息
        Console.WriteLine("=== Warns ===");
        foreach (var warn in errs.Warns)
        {
            Console.WriteLine(warn);
        }

        // 打印错误信息
        Console.WriteLine("\n=== Errors ===");
        foreach (var err in errs.Errors)
        {
            Console.WriteLine(err);
        }

        Console.WriteLine("\n=== Test Data ===");
        var langNames = stream.GetLangNames();
        Console.Out.WriteLine("language Count: " + langNames.Length);
        for (int i = 0; i < langNames.Length; i++) 
        {
            Console.Out.WriteLine(langNames[i]);
            TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[i]);
            Console.WriteLine(Config.Task.DataTask.Get(1));
        }
        
        
    }
}