
using System.Collections;

using System.Diagnostics.CodeAnalysis;

using System.Text;

namespace Config
{
    public interface IConfigExtension
    {
        void OnConfigReady(ConfigMgr config);
    }

    public abstract record LoadError
    {
        public sealed record MissingReference(string Source, string Field, string Target, object Key) : LoadError;
        public sealed record DuplicateKey(string Field, object Key) : LoadError;
        public sealed record DuplicateRecord(string Table, string Pack, object Key) : LoadError;
        public sealed record DuplicateUniqueKey(string Table, string Field, object RowKey, object Key, IReadOnlyList<string> Packs) : LoadError
        {
            public IReadOnlyList<string> Packs { get; } = Array.AsReadOnly(Packs.ToArray());
            public override string ToString() => $"DuplicateUniqueKey {{ Table = {Table}, Field = {Field}, RowKey = {RowKey}, Key = {Key}, Packs = [{string.Join(",", Packs)}] }}";
        }
        public sealed record InvalidSeq(string Table, string Field, object Key, int Value, int Size, IReadOnlyList<string> Packs) : LoadError
        {
            public IReadOnlyList<string> Packs { get; } = Array.AsReadOnly(Packs.ToArray());
            public override string ToString() => $"InvalidSeq {{ Table = {Table}, Field = {Field}, Key = {Key}, Value = {Value}, Size = {Size}, Packs = [{string.Join(",", Packs)}] }}";
        }
        public sealed record UnknownSymbol(string Table, string Symbol, object Key, IReadOnlyList<string> Packs) : LoadError
        {
            public IReadOnlyList<string> Packs { get; } = Array.AsReadOnly(Packs.ToArray());
            public override string ToString() => $"UnknownSymbol {{ Table = {Table}, Symbol = {Symbol}, Key = {Key}, Packs = [{string.Join(",", Packs)}] }}";
        }
        public sealed record ChangedSymbolId(string Table, string Symbol, object Expected, object Actual, IReadOnlyList<string> Packs) : LoadError
        {
            public IReadOnlyList<string> Packs { get; } = Array.AsReadOnly(Packs.ToArray());
            public override string ToString() => $"ChangedSymbolId {{ Table = {Table}, Symbol = {Symbol}, Expected = {Expected}, Actual = {Actual}, Packs = [{string.Join(",", Packs)}] }}";
        }
        public sealed record MissingSymbol(string Table, string Symbol, object Key) : LoadError;
        public sealed record UnknownVariant(string Union, string Tag) : LoadError;
        public sealed record SchemaMismatch(string Pack) : LoadError;
        public sealed record UnsupportedVersion(int Version, int Supported) : LoadError;
        public sealed record OutOfBounds(int Length, int Remaining) : LoadError;
        public sealed record UnconsumedBytes(int Remaining) : LoadError;
        public sealed record InvalidCount(int Count, int MinimumBytes, int Remaining) : LoadError;
        public sealed record InvalidPoolIndex(string Pool, int Index, int Size) : LoadError;
        public sealed record LanguageCountMismatch(string Language, int Expected, int Actual) : LoadError;
        public sealed record UnsupportedSchemaTag(int Tag) : LoadError;
        public sealed record MissingBlock(string Block) : LoadError;
        public sealed record MissingPackageName() : LoadError;
        public sealed record InvalidPackageName(string Name) : LoadError;
        public sealed record InvalidDependency(string Pack, string Dependency) : LoadError;
        public sealed record MissingDependency(string Pack, string Dependency) : LoadError;
        public sealed record DuplicatePackage(string Pack) : LoadError;
        public sealed record MissingBasePackage() : LoadError;
        public sealed record ForeignLanguageResource(string Pack, string ResourcePack) : LoadError;
        public sealed record EmptyLanguageResource(string Pack) : LoadError;
        public sealed record UnexpectedLanguageBlock(string Block) : LoadError;
    }

    public sealed class ConfigException : Exception
    {
        public IReadOnlyList<LoadError> Errors { get; }
        public ConfigException(IReadOnlyList<LoadError> errors) : base(string.Join(Environment.NewLine, errors))
        { Errors = Array.AsReadOnly(errors.ToArray()); }
    }

    public sealed class LoadErrors
    {
        private readonly List<LoadError> _errors = new List<LoadError>();
        public IReadOnlyList<LoadError> Errors => Array.AsReadOnly(_errors.ToArray());
        public void CheckErrors()
        {
            if (_errors.Count != 0) {
                throw new ConfigException(_errors);
            }
        }
        private void Add(LoadError error) => _errors.Add(error);
        private ConfigException Fail(LoadError error) { Add(error); return new ConfigException(_errors); }
        public void MissingReference(string source, string field, string target, object key) => Add(new LoadError.MissingReference(source, field, target, key));
        public void DuplicateKey(string field, object key) => Add(new LoadError.DuplicateKey(field, key));
        public void DuplicateRecord(string table, string pack, object key) => Add(new LoadError.DuplicateRecord(table, pack, key));
        public void DuplicateUniqueKey(string table, string field, object rowKey, object key, IReadOnlyList<string> packs) => Add(new LoadError.DuplicateUniqueKey(table, field, rowKey, key, packs));
        public void InvalidSeq(string table, string field, object key, int value, int size, IReadOnlyList<string> packs) => Add(new LoadError.InvalidSeq(table, field, key, value, size, packs));
        public void UnknownSymbol(string table, string symbol, object key, IReadOnlyList<string> packs) => Add(new LoadError.UnknownSymbol(table, symbol, key, packs));
        public void ChangedSymbolId(string table, string symbol, object expected, object actual, IReadOnlyList<string> packs) => Add(new LoadError.ChangedSymbolId(table, symbol, expected, actual, packs));
        public void MissingSymbol(string table, string symbol, object key) => Add(new LoadError.MissingSymbol(table, symbol, key));
        public ConfigException UnknownVariant(string union, string tag) => Fail(new LoadError.UnknownVariant(union, tag));
        public void SchemaMismatch(string pack) => Add(new LoadError.SchemaMismatch(pack));
        internal ConfigException UnsupportedVersion(int version, int supported) => Fail(new LoadError.UnsupportedVersion(version, supported));
        internal ConfigException OutOfBounds(int length, int remaining) => Fail(new LoadError.OutOfBounds(length, remaining));
        internal ConfigException UnconsumedBytes(int remaining) => Fail(new LoadError.UnconsumedBytes(remaining));
        internal ConfigException InvalidCount(int count, int minimumBytes, int remaining) => Fail(new LoadError.InvalidCount(count, minimumBytes, remaining));
        internal ConfigException InvalidPoolIndex(string pool, int index, int size) => Fail(new LoadError.InvalidPoolIndex(pool, index, size));
        internal ConfigException LanguageCountMismatch(string language, int expected, int actual) => Fail(new LoadError.LanguageCountMismatch(language, expected, actual));
        internal ConfigException UnsupportedSchemaTag(int tag) => Fail(new LoadError.UnsupportedSchemaTag(tag));
        internal ConfigException MissingBlock(string block) => Fail(new LoadError.MissingBlock(block));
        internal ConfigException MissingPackageName() => Fail(new LoadError.MissingPackageName());
        internal void InvalidPackageName(string name) => Add(new LoadError.InvalidPackageName(name));
        internal void InvalidDependency(string pack, string dependency) => Add(new LoadError.InvalidDependency(pack, dependency));
        internal void MissingDependency(string pack, string dependency) => Add(new LoadError.MissingDependency(pack, dependency));
        internal void DuplicatePackage(string pack) => Add(new LoadError.DuplicatePackage(pack));
        internal void MissingBasePackage() => Add(new LoadError.MissingBasePackage());
        internal void ForeignLanguageResource(string pack, string resourcePack) => Add(new LoadError.ForeignLanguageResource(pack, resourcePack));
        internal void EmptyLanguageResource(string pack) => Add(new LoadError.EmptyLanguageResource(pack));
        internal void UnexpectedLanguageBlock(string block) => Add(new LoadError.UnexpectedLanguageBlock(block));
    }

    public static class TextPoolManager
    {
        public static string? Language { get; set; }
    }

    internal sealed class ConfigPools
    {
        internal readonly string[] Strings, Languages;
        internal readonly string[][] Texts;
        internal ConfigPools(string[] strings, string[] languages, string[][] texts)
        { Strings = strings; Languages = (string[])languages.Clone(); Texts = texts; }
        internal bool Matches(string[] languages)
        {
            if (Languages.Length != languages.Length) {
                return false;
            }
            for (int i = 0; i < languages.Length; i++) {
                if (Languages[i] != languages[i]) {
                    return false;
                }
            }
            return true;
        }
        internal string GetText(int index)
        {
            string? language = TextPoolManager.Language;
            int position = language == null ? 0 : Array.IndexOf(Languages, language);
            if (position < 0) {
                throw new InvalidOperationException("Language is not loaded: " + language);
            }
            return Texts[position][index];
        }
    }

    public sealed class ConfigReader
    {
        private readonly byte[] _bytes;
        private readonly int _end;
        private int _position;
        private LoadErrors? _errors;
        private ConfigPools? _pools;
        public LoadErrors Errors => _errors ??= new LoadErrors();
        public int Remaining => _end - _position;
        public ConfigReader(byte[] bytes) : this(bytes, 0, bytes.Length) { }
        private ConfigReader(byte[] bytes, int start, int end) { _bytes = bytes; _position = start; _end = end; }
        internal void SetErrors(LoadErrors errors) => _errors = errors;
        internal ConfigReader WithPools(ConfigPools pools) { _pools = pools; return this; }
        internal ConfigPools CaptureTextPool() => _pools ?? throw new InvalidOperationException("Text pools not loaded");
        internal ConfigReader Fork() => new ConfigReader(_bytes, _position, _end) { _pools = _pools };
        internal ConfigReader Slice(int length)
        {
            Require(length);
            var input = new ConfigReader(_bytes, _position, _position + length) { _errors = _errors };
            _position += length;
            return input;
        }
        internal void Require(int length)
        {
            if (length < 0 || length > Remaining) {
                throw Errors.OutOfBounds(length, Remaining);
            }
        }
        public void RequireEnd()
        {
            if (Remaining != 0) {
                throw Errors.UnconsumedBytes(Remaining);
            }
        }
        public int ReadInt32()
        {
            Require(4); int p = _position; _position += 4;
            return _bytes[p] | (_bytes[p + 1] << 8) | (_bytes[p + 2] << 16) | (_bytes[p + 3] << 24);
        }
        public long ReadInt64() => (uint)ReadInt32() | ((long)ReadInt32() << 32);
        public bool ReadBool() { Require(1); return _bytes[_position++] != 0; }
        public float ReadSingle() => BitConverter.Int32BitsToSingle(ReadInt32());
        public int ReadCount(int minimumBytes = 1)
        {
            int count = ReadInt32();
            if (count < 0 || minimumBytes > 0 && count > Remaining / minimumBytes) {
                throw Errors.InvalidCount(count, minimumBytes, Remaining);
            }
            return count;
        }
        public string ReadString()
        {
            int length = ReadInt32(); Require(length);
            string value = Encoding.UTF8.GetString(_bytes, _position, length); _position += length; return value;
        }
        public string ReadStringInPool()
        {
            int index = ReadInt32(); var strings = CaptureTextPool().Strings;
            if (index < 0 || index >= strings.Length) {
                throw Errors.InvalidPoolIndex("strings", index, strings.Length);
            }
            return strings[index];
        }
        public int ReadTextIndex()
        {
            int index = ReadInt32(); int size = CaptureTextPool().Texts[0].Length;
            if (index < 0 || index >= size) {
                throw Errors.InvalidPoolIndex("text", index, size);
            }
            return index;
        }
        public string ReadTextInPool() => CaptureTextPool().Texts[0][ReadTextIndex()];
    }

    public sealed class ConfigMap<TKey, TValue> : IReadOnlyDictionary<TKey, TValue> where TKey : notnull
    {
        internal static readonly ConfigMap<TKey, TValue> Empty = new ConfigMap<TKey, TValue>();
        private readonly Dictionary<TKey, TValue> _lookup;
        private readonly List<KeyValuePair<TKey, TValue>> _entries;
        internal ConfigMap(int capacity = 0)
        { _lookup = new Dictionary<TKey, TValue>(capacity); _entries = new List<KeyValuePair<TKey, TValue>>(capacity); }
        internal bool TryAdd(TKey key, TValue value)
        {
            if (!_lookup.TryAdd(key, value)) {
                return false;
            }
            _entries.Add(new KeyValuePair<TKey, TValue>(key, value)); return true;
        }
        public int Count => _entries.Count;
        public TValue this[TKey key] => _lookup[key];
        public IEnumerable<TKey> Keys
        {
            get
            {
                foreach (var pair in _entries) {
                    yield return pair.Key;
                }
            }
        }
        public IEnumerable<TValue> Values
        {
            get
            {
                foreach (var pair in _entries) {
                    yield return pair.Value;
                }
            }
        }
        public bool ContainsKey(TKey key) => _lookup.ContainsKey(key);
        public bool TryGetValue(TKey key, [MaybeNullWhen(false)] out TValue value) => _lookup.TryGetValue(key, out value);
        public IEnumerator<KeyValuePair<TKey, TValue>> GetEnumerator() => _entries.GetEnumerator();
        IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
    }

    public static class StringUtil
    {
        public static string ToString<T>(IReadOnlyList<T> data) => "[" + string.Join(", ", data) + "]";
    }
}
