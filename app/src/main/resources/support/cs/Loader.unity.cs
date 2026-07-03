using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace Config
{
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

    public sealed class NotFoundImpl : Exception
    {
        private readonly string _tableName;
        private readonly string _implName;
        private readonly string _interfaceName;

        public NotFoundImpl(string tableName, string implName, string interfaceName)
        {
            _tableName = tableName;
            _implName = implName;
            _interfaceName = interfaceName;
        }

        public override string Message =>
            $"Implementation '{_implName}' for interface '{_interfaceName}' not found in table '{_tableName}'.";
    }

    public record ConfigLoadResult(
        IReadOnlyList<LoadIssue> LoadIssues,
        string[]? LangNames,
        string[][]? LangTextPools);

    public interface IIssueHandler
    {
        void EnumNotInCode(string enumName);
        void EnumNotInData(string enumName);
        void EnumDuplicateInData(string enumName);
        void RefNotFound(string structName, string fieldName, string fieldValue);
    }

    public class ConfigReader : IIssueHandler, IDisposable
    {
        private readonly BinaryReader _reader;
        private byte[] _stringBuffer = new byte[256]; // 预分配一个缓冲区
        private string _curTableName = "";

        // StringPool 和 LangTextPool 字段
        private string[]? _stringPool;
        private string[]? _langNames;
        private string[][]? _langTextPools;
        private readonly List<LoadIssue> _loadIssues = new List<LoadIssue>();

        public ConfigReader(BinaryReader reader)
        {
            _reader = reader;
        }

        public string ReadTableName()
        {
            _curTableName = ReadString();
            return _curTableName;
        }

        public string ReadString()
        {
            int count = _reader.ReadInt32();
            if (count <= 0) return string.Empty;

            // 扩容机制
            if (_stringBuffer.Length < count)
            {
                Array.Resize(ref _stringBuffer, Math.Max(_stringBuffer.Length * 2, count));
            }

            ReadExact(_reader.BaseStream, _stringBuffer, 0, count);
            return Encoding.UTF8.GetString(_stringBuffer, 0, count);
        }

        public int ReadInt32() => _reader.ReadInt32();
        public long ReadInt64() => _reader.ReadInt64();
        public bool ReadBool() => _reader.ReadBoolean();
        public float ReadSingle() => _reader.ReadSingle();

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
            _reader.BaseStream.Seek(count, SeekOrigin.Current);
        }

        // 读取精确字节数（替代 .NET 7 的 Stream.ReadExactly）
        private static void ReadExact(Stream stream, byte[] buffer, int offset, int count)
        {
            int read = 0;
            while (read < count)
            {
                int n = stream.Read(buffer, offset + read, count - read);
                if (n == 0)
                    throw new EndOfStreamException();
                read += n;
            }
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
            _reader.Dispose();
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

            return char.ToUpper(input[0]) + input.Substring(1);
        }
    }


    public class SimpleIssueHandler : IIssueHandler
    {
        private readonly string _tableName;
        private readonly List<LoadIssue> _issues;

        public SimpleIssueHandler(string tableName, List<LoadIssue> issues)
        {
            _tableName = tableName;
            _issues = issues;
        }

        public void EnumNotInCode(string enumName)
        {
            _issues.Add(new EnumNotInCode(_tableName, enumName));
        }

        public void EnumNotInData(string enumName)
        {
            _issues.Add(new EnumNotInData(_tableName, enumName));
        }

        public void EnumDuplicateInData(string enumName)
        {
            _issues.Add(new EnumDuplicateInData(_tableName, enumName));
        }

        public void RefNotFound(string structName, string fieldName, string fieldValue)
        {
            _issues.Add(new RefNotFound(_tableName, structName, fieldName, fieldValue));
        }
    }

    // 保持插入顺序的泛型字典（替代 .NET 9 才有的 System.Collections.Generic.OrderedDictionary<TKey,TValue>）
#nullable disable
    public class OrderedDictionary<TKey, TValue> : IEnumerable<KeyValuePair<TKey, TValue>>
    {
        private readonly Dictionary<TKey, TValue> _dict;
        private readonly List<TKey> _keys;

        public OrderedDictionary()
        {
            _dict = new Dictionary<TKey, TValue>();
            _keys = new List<TKey>();
        }

        public OrderedDictionary(int capacity)
        {
            _dict = new Dictionary<TKey, TValue>(capacity);
            _keys = new List<TKey>(capacity);
        }

        public void Add(TKey key, TValue value)
        {
            _dict.Add(key, value);
            _keys.Add(key);
        }

        public int Count => _dict.Count;

        public TValue this[TKey key] => _dict[key];

        public bool ContainsKey(TKey key) => _dict.ContainsKey(key);

        public bool TryGetValue(TKey key, out TValue value) => _dict.TryGetValue(key, out value);

        // 按插入顺序遍历的值集合
        public IEnumerable<TValue> OrderedValues
        {
            get
            {
                foreach (var k in _keys)
                    yield return _dict[k];
            }
        }

        public IEnumerator<KeyValuePair<TKey, TValue>> GetEnumerator()
        {
            foreach (var k in _keys)
                yield return new KeyValuePair<TKey, TValue>(k, _dict[k]);
        }

        IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
    }
#nullable restore
}
