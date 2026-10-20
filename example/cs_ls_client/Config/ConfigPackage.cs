using System;
using System.Collections.Generic;
using System.IO;
using System.Collections.ObjectModel;
using System.Text;

namespace Config
{
    public sealed class ConfigPackage
    {
        public const int FormatVersion = 1;
        private readonly Dictionary<string, ConfigReader> _blocks;
        private readonly string? _name;
        private readonly IReadOnlyList<string> _dependencies;
        private readonly string[] _defaultLanguages;
        private readonly List<ConfigPools> _pools = new List<ConfigPools>();
        public IReadOnlyDictionary<string, string> Metadata { get; }
        public string Name => _name ?? throw new ConfigException(new LoadError[] { new LoadError.MissingPackageName() });

        private ConfigPackage(IReadOnlyDictionary<string, string> metadata, Dictionary<string, ConfigReader> blocks,
            string? name, IReadOnlyList<string> dependencies)
        {
            Metadata = metadata; _blocks = blocks; _name = name; _dependencies = dependencies;
            _defaultLanguages = new[] { metadata.TryGetValue("defaultLanguage", out var language) ? language : "default" };
        }

        public static ConfigPackage ReadFile(string path) => Read(File.ReadAllBytes(path));

        public static ConfigPackage Read(byte[] bytes)
        {
            var input = new ConfigReader(bytes);
            var errors = input.Errors;
            int version = input.ReadInt32();
            if (version != FormatVersion) {
                throw errors.UnsupportedVersion(version, FormatVersion);
            }
            int count = input.ReadCount(8);
            var metadata = new Dictionary<string, string>(count);
            for (int i = 0; i < count; i++) {
                string key = input.ReadString(); string value = input.ReadString();
                if (!metadata.TryAdd(key, value)) {
                    errors.DuplicateKey("metadata", key);
                }
            }
            count = input.ReadCount(8);
            var blocks = new Dictionary<string, ConfigReader>(count);
            for (int i = 0; i < count; i++) {
                string block = input.ReadString(); var contents = input.Slice(input.ReadInt32());
                if (!blocks.TryAdd(block, contents)) {
                    errors.DuplicateKey("block", block);
                }
            }
            input.RequireEnd();
            metadata.TryGetValue("pack", out var name);
            if (name != null && (string.IsNullOrWhiteSpace(name) || name != name.Trim() || name.Contains(","))) {
                errors.InvalidPackageName(name);
            }
            var dependencies = ParseDependencies(metadata.TryGetValue("requires", out var requires) ? requires : "", name, errors);
            errors.CheckErrors();
            return new ConfigPackage(new ReadOnlyDictionary<string, string>(metadata), blocks, name, dependencies);
        }

        public bool HasBlock(string name) => _blocks.ContainsKey(name);

        public ConfigReader Block(string name, params string[] languages)
        {
            if (!_blocks.TryGetValue(name, out var raw)) {
                throw new LoadErrors().MissingBlock(name);
            }
            var input = raw.Fork();
            return name.StartsWith(".", StringComparison.Ordinal) ? input.WithPools(Pools(languages)) : input;
        }

        public ConfigPackage Attach(params ConfigPackage[] resources)
        {
            if (resources.Length == 0) {
                return this;
            }
            var errors = new LoadErrors();
            var blocks = new Dictionary<string, ConfigReader>(_blocks);
            foreach (var resource in resources) {
                if (Name != resource.Name) {
                    errors.ForeignLanguageResource(Name, resource.Name);
                }
                if (resource._blocks.Count == 0) {
                    errors.EmptyLanguageResource(resource.Name);
                }
                foreach (var pair in resource._blocks) {
                    if (!pair.Key.StartsWith("text/", StringComparison.Ordinal)) {
                        errors.UnexpectedLanguageBlock(pair.Key);
                    }
                    if (!blocks.TryAdd(pair.Key, pair.Value)) {
                        errors.DuplicateKey("language", pair.Key);
                    }
                }
            }
            errors.CheckErrors();
            return new ConfigPackage(Metadata, blocks, _name, _dependencies);
        }

        private ConfigPools Pools(string[] languages)
        {
            lock (_pools) {
                if (languages.Length == 0) {
                    languages = _defaultLanguages;
                }
                foreach (var cached in _pools) {
                    if (cached.Matches(languages)) {
                        return cached;
                    }
                }
                var input = Block("strings");
                string[] strings = ReadStrings(input); input.RequireEnd();
                var texts = new string[languages.Length][];
                int textCount = -1;
                for (int i = 0; i < languages.Length; i++) {
                    input = Block("text/" + languages[i]);
                    int count = input.ReadCount(4);
                    if (textCount >= 0 && textCount != count) {
                        throw input.Errors.LanguageCountMismatch(languages[i], textCount, count);
                    }
                    textCount = count;
                    var indices = new int[count];
                    for (int j = 0; j < count; j++) {
                        indices[j] = input.ReadInt32();
                    }
                    string[] pool = ReadStrings(input); input.RequireEnd();
                    texts[i] = new string[count];
                    for (int j = 0; j < count; j++) {
                        if (indices[j] < 0 || indices[j] >= pool.Length) {
                            throw input.Errors.InvalidPoolIndex(languages[i], indices[j], pool.Length);
                        }
                        texts[i][j] = pool[indices[j]];
                    }
                }
                var result = new ConfigPools(strings, languages, texts);
                _pools.Add(result); return result;
            }
        }

        private static IReadOnlyList<string> ParseDependencies(string value, string? name, LoadErrors errors)
        {
            if (value.Length == 0) {
                return Array.Empty<string>();
            }
            var result = new List<string>(); var seen = new HashSet<string>();
            foreach (string item in value.Split(',')) {
                string dependency = item.Trim();
                if (dependency.Length == 0 || dependency == name || !seen.Add(dependency)) {
                    errors.InvalidDependency(name ?? "", item);
                }
                else {
                    result.Add(dependency);
                }
            }
            return result.AsReadOnly();
        }

        public IReadOnlyList<string> Dependencies() => _dependencies;

        public static void ValidateOrder(IReadOnlyList<ConfigPackage> packages)
        {
            var errors = new LoadErrors();
            if (packages.Count == 0) {
                errors.MissingBasePackage();
            }
            var seen = new HashSet<string>();
            foreach (var pack in packages) {
                string name = pack.Name;
                if (!seen.Add(name)) {
                    errors.DuplicatePackage(name);
                }
                foreach (string dependency in pack._dependencies) {
                    if (!seen.Contains(dependency)) {
                        errors.MissingDependency(name, dependency);
                    }
                }
            }
            errors.CheckErrors();
        }

        private static string[] ReadStrings(ConfigReader input)
        {
            var result = new string[input.ReadCount(4)];
            for (int i = 0; i < result.Length; i++) {
                result[i] = input.ReadString();
            }
            return result;
        }

        internal void ValidateSchema(string expected, LoadErrors errors)
        {
            var input = Block("schema"); input.SetErrors(errors);
            string actual = SchemaKey(input); input.RequireEnd();
            if (expected != actual) {
                errors.SchemaMismatch(Name);
            }
        }

        internal static string SchemaKey(ConfigReader input)
        {
            int kind = input.ReadInt32();
            var result = new StringBuilder(); result.Append(kind).Append(':');
            if (kind >= 1 && kind <= 6) {
                return result.ToString();
            }
            if (kind == 7) {
                Part(result, input.ReadString());
                return result.ToString();
            }
            if (kind == 8) {
                Part(result, SchemaKey(input));
                return result.ToString();
            }
            if (kind == 9) {
                Part(result, SchemaKey(input));
                Part(result, SchemaKey(input));
                return result.ToString();
            }
            if (kind == 10) {
                result.Append(input.ReadBool()).Append(':'); int count = input.ReadCount(); result.Append(count).Append(':');
                var names = new HashSet<string>();
                for (int i = 0; i < count; i++) {
                    string name = input.ReadString();
                    if (!names.Add(name)) {
                        input.Errors.DuplicateKey("schema field", name);
                    }
                    Part(result, name); Part(result, SchemaKey(input));
                }
                return result.ToString();
            }
            var entries = new SortedDictionary<string, string>(StringComparer.Ordinal);
            if (kind == 11 || kind == 12) {
                bool integer = false;
                if (kind == 12) {
                    result.Append(input.ReadBool());
                    integer = input.ReadBool();
                    result.Append(integer).Append(':');
                }
                int count = input.ReadCount();
                for (int i = 0; i < count; i++) {
                    string name = input.ReadString();
                    string value = kind == 11 ? SchemaKey(input) : integer ? input.ReadInt32().ToString(System.Globalization.CultureInfo.InvariantCulture) : "";
                    if (!entries.TryAdd(name, value)) {
                        input.Errors.DuplicateKey("schema symbol", name);
                    }
                }
            } else {
                throw input.Errors.UnsupportedSchemaTag(kind);
            }
            result.Append(entries.Count).Append(':');
            foreach (var pair in entries) {
                Part(result, pair.Key);
                Part(result, pair.Value);
            }
            return result.ToString();
        }

        private static void Part(StringBuilder target, string value) { target.Append(value.Length).Append(':').Append(value); }
    }
}
