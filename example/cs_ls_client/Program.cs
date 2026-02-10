using Config;
using Stream = Config.Stream;

class Program
{
    public static void Main()
    {
        byte[] bytes = File.ReadAllBytes("config.bytes");

        Config.Loader.Processor = Config.Processor.Process;
        Stream stream = Config.Loader.LoadBytes(bytes);
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