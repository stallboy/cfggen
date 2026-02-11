class Program
{
    public static void XorBytesWithCipher(byte[] target, string cipher)
    {
        byte[] cipherBytes = System.Text.Encoding.UTF8.GetBytes(cipher);
        for (int i = 0; i < target.Length; i++)
        {
            target[i] = (byte)(target[i] ^ cipherBytes[i % cipherBytes.Length]);
        }
    }

    public static void Main()
    {
        byte[] bytes = File.ReadAllBytes("config.bytes");
        string cipher = "xyz";
        Console.WriteLine("cipher: " + cipher);
        XorBytesWithCipher(bytes, cipher);
    
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
        Console.WriteLine(Config.Task.DataTask.Get(1));
    }
}