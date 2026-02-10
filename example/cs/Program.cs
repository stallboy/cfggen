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
    
        Config.Loader.Processor = Config.Processor.Process;
        Config.Stream stream = Config.Loader.LoadBytes(bytes);
        Console.WriteLine(Config.Task.DataTask.Get(1));
    }
}