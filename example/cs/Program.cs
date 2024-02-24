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

    public static void Main(string[] args)
    {
        byte[] bytes = File.ReadAllBytes("config.bytes");
        if (args.Length > 0)
        {
            string cipher = args[0];
            Console.WriteLine("cipher: " + cipher);
            XorBytesWithCipher(bytes, cipher);
        }

        Config.CSVLoader.Processor = Config.CSVProcessor.Process;
        Config.CSVLoader.LoadBytes(bytes);

        Console.WriteLine(Config.Task.DataTask.Get(1));
    }
}