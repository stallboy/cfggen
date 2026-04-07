using System.Text;

namespace Config;

// 客户端模式：全局文本管理器（应用层负责语言切换）
public static class TextPoolManager
{
    private static string[]? _globalTexts;

    public static void SetGlobalTexts(string[] texts)
    {
        _globalTexts = texts;
    }

    public static string GetText(int index)
    {
        if (_globalTexts == null)
            throw new InvalidOperationException("Global texts have not been initialized.");

        if (index < 0 || index >= _globalTexts.Length)
            throw new ArgumentOutOfRangeException(nameof(index), $"Text index {index} is out of range.");

        return _globalTexts[index];
    }
}

public abstract record LoadIssue(string Table)
{
    public abstract bool IsError { get; }
}

public abstract record LoadWarn(string Table) : LoadIssue(Table)
{
    public override bool IsError => false;
}

public abstract record LoadError(string Table) : LoadIssue(Table)
{
    public override bool IsError => true;
}

public sealed record TableNotInData(string Table) : LoadWarn(Table);

public sealed record TableNotInCode(string Table) : LoadWarn(Table);

public sealed record EnumNotInCode(string Table, string EnumName) : LoadWarn(Table);

public sealed record EnumNotInData(string Table, string EnumName) : LoadError(Table);

public sealed record EnumDuplicateInData(string Table, string EnumName) : LoadError(Table);

public sealed record RefNotFound(string Table, string StructName, string FiledName, string FieldValue)
    : LoadError(Table);

public sealed class NotFoundImpl(string TableName, string ImplName, string InterfaceName) : Exception
{
    public override string Message =>
        $"Implementation '{ImplName}' for interface '{InterfaceName}' not found in table '{TableName}'.";
}

public record ConfigLoadResult(
    IReadOnlyList<LoadIssue> LoadIssues,
    string[]? LangNames,
    string[][]? LangTextPools);

public class ConfigReader(BinaryReader reader) : IDisposable
{
    private byte[] _stringBuffer = new byte[256]; // 预分配一个缓冲区
    private string _curTableName = "";

    // StringPool 和 LangTextPool 字段
    private string[]? _stringPool;
    private string[]? _langNames;
    private string[][]? _langTextPools;
    private readonly List<LoadIssue> _loadIssues = [];

    public string ReadTableName()
    {
        _curTableName = ReadString();
        return _curTableName;
    }

    public string ReadString()
    {
        var count = reader.ReadInt32();
        if (count <= 0) return string.Empty;

        // 扩容机制
        if (_stringBuffer.Length < count)
        {
            Array.Resize(ref _stringBuffer, Math.Max(_stringBuffer.Length * 2, count));
        }

        reader.BaseStream.ReadExactly(_stringBuffer, 0, count);
        return Encoding.UTF8.GetString(_stringBuffer, 0, count);
    }

    public int ReadInt32() => reader.ReadInt32();
    public long ReadInt64() => reader.ReadInt64();
    public bool ReadBool() => reader.ReadBoolean();
    public float ReadSingle() => reader.ReadSingle();

    public string ReadStringInPool()
    {
        int index = ReadInt32();
        if (_stringPool == null)
            throw new InvalidOperationException("StringPool not initialized");

        if (index < 0 || index >= _stringPool.Length)
            throw new IndexOutOfRangeException($"Index {index} out of StringPool range.");

        return _stringPool[index];
    }

    public void ReadStringPool()
    {
        int count = ReadInt32();
        _stringPool = new string[count];
        for (int i = 0; i < count; i++)
        {
            _stringPool[i] = ReadString();
        }
    }

    public void ReadLangTextPool()
    {
        int langCount = ReadInt32();
        _langNames = new string[langCount];
        _langTextPools = new string[langCount][];

        for (int langIdx = 0; langIdx < langCount; langIdx++)
        {
            _langNames[langIdx] = ReadString();

            int indexCount = ReadInt32();
            int[] indices = new int[indexCount];
            for (int i = 0; i < indexCount; i++)
            {
                indices[i] = ReadInt32();
            }

            int poolCount = ReadInt32();
            string[] pool = new string[poolCount];
            for (int i = 0; i < poolCount; i++)
            {
                pool[i] = ReadString();
            }

            _langTextPools[langIdx] = new string[indexCount];
            for (int i = 0; i < indexCount; i++)
            {
                _langTextPools[langIdx][i] = pool[indices[i]];
            }
        }
    }

    public string[] ReadTextsInPool()
    {
        int index = ReadInt32();
        if (_langTextPools == null)
            throw new InvalidOperationException("LangTextPool not initialized");

        string[] texts = new string[_langTextPools.Length];
        for (int i = 0; i < _langTextPools.Length; i++)
        {
            texts[i] = (index < 0 || index >= _langTextPools[i].Length)
                ? ""
                : _langTextPools[i][index];
        }

        return texts;
    }

    public string ReadTextInPool()
    {
        int index = ReadInt32();
        if (_langTextPools == null || _langTextPools.Length == 0)
            throw new InvalidOperationException("LangTextPool not initialized");

        if (index < 0 || index >= _langTextPools[0].Length)
            throw new IndexOutOfRangeException($"Index {index} out of LangTextPool.");

        return _langTextPools[0][index];
    }

    public int ReadTextIndex() => ReadInt32();

    public void SkipBytes(int count)
    {
        reader.BaseStream.Seek(count, SeekOrigin.Current);
    }

    public NotFoundImpl NotFoundImpl(string implName, string interfaceName)
    {
        return new NotFoundImpl(_curTableName, implName, interfaceName);
    }

    public void TableNotInData(string tableName)
    {
        _loadIssues.Add(new TableNotInData(tableName));
    }


    public void TableNotInCode()
    {
        _loadIssues.Add(new TableNotInCode(_curTableName));
    }

    public void EnumNotInCode(string enumName)
    {
        _loadIssues.Add(new EnumNotInCode(_curTableName, enumName));
    }

    public void EnumNotInData(string enumName)
    {
        _loadIssues.Add(new EnumNotInData(_curTableName, enumName));
    }

    public void EnumDuplicateInData(string enumName)
    {
        _loadIssues.Add(new EnumDuplicateInData(_curTableName, enumName));
    }

    public void RefNotFound(string structName, string fieldName, string fieldValue)
    {
        _loadIssues.Add(new RefNotFound(_curTableName, structName, fieldName, fieldValue));
    }


    public ConfigLoadResult MakeResult()
    {
        return new ConfigLoadResult(_loadIssues, _langNames, _langTextPools);
    }

    public void Dispose()
    {
        reader.Dispose();
    }
}

public static class Loader
{
    public delegate void ProcessConfigStream(ConfigReader reader);

    private static ConfigLoadResult Load(ConfigReader reader, ProcessConfigStream processor)
    {
        int schemaLength = reader.ReadInt32();
        if (schemaLength > 0)
        {
            reader.SkipBytes(schemaLength);
        }

        reader.ReadStringPool();
        reader.ReadLangTextPool();

        processor(reader);
        return reader.MakeResult();
    }

    public static ConfigLoadResult LoadFile(string filePath, ProcessConfigStream processor)
    {
        using var fileStream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.Read,
            bufferSize: 4096, FileOptions.SequentialScan);

        using var reader = new ConfigReader(new BinaryReader(fileStream));

        return Load(reader, processor);
    }

    public static ConfigLoadResult LoadBytes(byte[] data, ProcessConfigStream processor)
    {
        using var memoryStream = new MemoryStream(data);
        using var reader = new ConfigReader(new BinaryReader(memoryStream));

        return Load(reader, processor);
    }
}
public static class StringUtil
{
    public static string ToString<T>(List<T> data)
    {
        if (data.Count == 0) return "[]";
        return $"[{string.Join(", ", data.ConvertAll(d => d is null ? "null" : d.ToString()))}]";
    }

    public static string UpperFirstChar(string input)
    {
        if (string.IsNullOrEmpty(input))
            return input;

        return string.Create(input.Length, input, (span, str) =>
        {
            span[0] = char.ToUpper(str[0]);
            str.AsSpan(1).CopyTo(span[1..]);
        });
    }
}
