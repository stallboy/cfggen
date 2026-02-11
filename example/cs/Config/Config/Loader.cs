using System.Collections.Generic;
using System.IO;
using System.Text;

namespace Config
{
    // 客户端模式：全局文本管理器（应用层负责语言切换）
    public static class TextPoolManager
    {
        private static string[] _globalTexts;

        public static void SetGlobalTexts(string[] texts)
        {
            _globalTexts = texts;
        }

        public static string GetText(int index)
        {
            return _globalTexts[index];
        }
    }

    public class Stream
    {
        private readonly List<BinaryReader> _byterList;

        private int _currentIndex;
        private BinaryReader _byter;

        // StringPool 和 LangTextPool 字段
        private string[] _stringPool;
        private string[] _langNames;
        private string[][] _langTextPools; // langTextPools[langIndex][textIndex]

        public Stream(List<BinaryReader> byterList)
        {
            _byterList = byterList;
            _currentIndex = 0;
            _byter = byterList[0];
        }

        public string ReadCfg()
        {
            try
            {
                var cfg = ReadString();
                return cfg;
            }
            catch (EndOfStreamException)
            {
                _currentIndex ++;
                if (_currentIndex < _byterList.Count)
                {
                    _byter = _byterList[_currentIndex];
                    var cfg = ReadString();
                    return cfg;
                }
            }
            return null;
        }

        public int ReadSize()
        {
            return _byter.ReadInt32();
        }

        public string ReadString()
        {
            var count = _byter.ReadInt32();
            return Encoding.UTF8.GetString(_byter.ReadBytes(count));
        }

        public int ReadInt32()
        {
            return _byter.ReadInt32();
        }

        public long ReadInt64()
        {
            return _byter.ReadInt64();
        }

        public bool ReadBool()
        {
            return _byter.ReadBoolean();
        }

        public float ReadSingle()
        {
            return _byter.ReadSingle();
        }

        // 从 StringPool 读取字符串（用于 STRING 类型字段）
        public string ReadStringInPool()
        {
            int index = ReadInt32();
            if (_stringPool == null)
                throw new Exception("StringPool not initialized");

            if (index < 0 || index >= _stringPool.Length)
                throw new Exception("index out of StringPool");

            return _stringPool[index];
        }

        // 读取全局 StringPool（在读取表数据之前调用）
        public void ReadStringPool()
        {
            int count = ReadInt32();
            _stringPool = new string[count];
            for (int i = 0; i < count; i++)
            {
                _stringPool[i] = ReadString();
            }
        }

        // 读取 LangTextPool（在读取表数据之前调用）
        public void ReadLangTextPool()
        {
            int langCount = ReadInt32();
            _langNames = new string[langCount];
            _langTextPools = new string[langCount][];

            for (int langIdx = 0; langIdx < langCount; langIdx++)
            {
                string langName = ReadString();
                _langNames[langIdx] = langName;

                int indexCount = ReadInt32();
                int[] indices = new int[indexCount];
                for (int i = 0; i < indexCount; i++)
                {
                    indices[i] = ReadInt32();
                }

                // 读取该语言的 StringPool
                int poolCount = ReadInt32();
                string[] pool = new string[poolCount];
                for (int i = 0; i < poolCount; i++)
                {
                    pool[i] = ReadString();
                }

                // 构建文本数组：texts[textIndex] = pool[indices[textIndex]]
                _langTextPools[langIdx] = new string[indexCount];
                for (int i = 0; i < indexCount; i++)
                {
                    _langTextPools[langIdx][i] = pool[indices[i]];
                }
            }
        }

        // 从 LangTextPool 读取所有语言文本（多语言 服务器端模式）
        public string[] ReadTextsInPool()
        {
            int index = ReadInt32();
            if (_langTextPools == null)
                throw new Exception("LangTextPool not initialized");

            string[] texts = new string[_langTextPools.Length];
            for (int i = 0; i < _langTextPools.Length; i++)
            {
                if (index < 0 || index >= _langTextPools[i].Length)
                    texts[i] = "";
                else
                    texts[i] = _langTextPools[i][index];
            }
            return texts;
        }

        // 从 LangTextPool 读取text （单语言模式）
        public string ReadTextInPool()
        {
            int index = ReadInt32();
            if (_langTextPools == null)
                throw new Exception("LangTextPool not initialized");

            if (index < 0 || index >= _langTextPools[0].Length)
                throw new Exception("index out of LangTextPool");

            return _langTextPools[0][index];
        }

        // 从 LangTextPool 读取文本索引（客户端模式）
        public int ReadTextIndex()
        {
            return ReadInt32();
        }

        // 访问接口：获取语言名称列表
        public string[] GetLangNames()
        {
            return _langNames;
        }

        // 访问接口：获取所有语言的文本池
        public string[][] GetLangTextPools()
        {
            return _langTextPools;
        }

        // 跳过指定字节数（用于跳过未知表的数据）
        public void SkipBytes(int count)
        {
            _byter.ReadBytes(count);
        }
    }


    public static class Loader
    {
        public delegate void ProcessConfigStream(Stream os, LoadErrors errors);

        public static Stream LoadBytes(byte[] data, ProcessConfigStream processor, LoadErrors errors)
        {
            MemoryStream memoryStream = new MemoryStream(data);
            var reader = new BinaryReader(memoryStream);
            var stream = new Stream(new List<BinaryReader>{ reader });

            // 1. 跳过 Schema（如果有）
            int schemaLength = reader.ReadInt32();
            if (schemaLength > 0)
            {
                reader.ReadBytes(schemaLength);
            }

            // 2. 读取 StringPool
            stream.ReadStringPool();

            // 3. 读取 LangTextPool
            stream.ReadLangTextPool();

            // 4. 处理表数据
            processor(stream, errors);

            memoryStream.Dispose();
            return stream;
        }
    }
}
