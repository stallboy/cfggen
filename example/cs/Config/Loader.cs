using System.Collections.Generic;
using System.IO;
using System.Text;

namespace Config
{
    public class Stream
    {
        private readonly List<BinaryReader> _byterList;

        private int _currentIndex;
        private BinaryReader _byter;

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
    }


    public static class Loader
    {
        public delegate void ProcessConfigStream(Stream os);

        public static ProcessConfigStream Processor;

        public static void LoadBytes(byte[] data)
        {
            MemoryStream memoryStream = new MemoryStream(data);
            var reader = new BinaryReader(memoryStream);
            Processor(new Stream( new List<BinaryReader>{ reader }) );
            memoryStream.Dispose();
        }
    }
}
