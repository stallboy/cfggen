class Program
{
    public static void Main()
    {
        byte[] bytes = File.ReadAllBytes("config.bytes");

        Config.Loader.Processor = Config.Processor.Process;
        Config.Loader.LoadBytes(bytes);

        Console.WriteLine(Config.Task.DataTask.Get(1));
    }
}