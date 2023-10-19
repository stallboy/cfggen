byte[] bytes = File.ReadAllBytes("../config.bytes");

Config.CSVLoader.Processor = Config.CSVProcessor.Process;
Config.CSVLoader.LoadBytes(bytes);

Console.WriteLine(Config.Task.DataTask.Get(1));
