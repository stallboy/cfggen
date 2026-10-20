using Config;

static class Program
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
    
        Processor.LoadPackagesAndSetMgr(ConfigPackage.Read(bytes));
        
        Console.WriteLine("\n=== Test Data ===");
        Console.WriteLine(Config.Task.DTask.Get(1));
    }
}
