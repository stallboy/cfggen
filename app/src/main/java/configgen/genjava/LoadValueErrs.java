package configgen.genjava;

import java.util.ArrayList;
import java.util.List;

public final class LoadValueErrs {
    public sealed interface Error permits MissingReference, DuplicateKey, DuplicateRecord, DuplicateUniqueKey,
            InvalidSeq, UnknownSymbol, ChangedSymbolId, MissingSymbol, UnknownVariant, SchemaMismatch,
            UnsupportedVersion, OutOfBounds, UnconsumedBytes, InvalidCount, InvalidPoolIndex,
            LanguageCountMismatch, UnsupportedSchemaTag, MissingBlock, MissingPackageName, InvalidPackageName,
            InvalidDependency, MissingDependency, DuplicatePackage, MissingBasePackage,
            ForeignLanguageResource, EmptyLanguageResource, UnexpectedLanguageBlock {}

    public record MissingReference(String source, String field, String target, Object key) implements Error {}
    public record DuplicateKey(String field, Object key) implements Error {}
    public record DuplicateRecord(String table, String pack, Object key) implements Error {}
    public record DuplicateUniqueKey(String table, String field, Object rowKey, Object key, List<String> packs) implements Error {
        public DuplicateUniqueKey { packs = List.copyOf(packs); }
    }
    public record InvalidSeq(String table, String field, Object key, int value, int size, List<String> packs) implements Error {
        public InvalidSeq { packs = List.copyOf(packs); }
    }
    public record UnknownSymbol(String table, String symbol, Object key, List<String> packs) implements Error {
        public UnknownSymbol { packs = List.copyOf(packs); }
    }
    public record ChangedSymbolId(String table, String symbol, int expected, int actual, List<String> packs) implements Error {
        public ChangedSymbolId { packs = List.copyOf(packs); }
    }
    public record MissingSymbol(String table, String symbol, Object key) implements Error {}
    public record UnknownVariant(String union, String tag) implements Error {}
    public record SchemaMismatch(String pack, String reason) implements Error {}
    public record UnsupportedVersion(int version, int supported) implements Error {}
    public record OutOfBounds(int length, long remaining) implements Error {}
    public record UnconsumedBytes(long remaining) implements Error {}
    public record InvalidCount(int count, int minimumBytes, long remaining) implements Error {}
    public record InvalidPoolIndex(String pool, int index, int size) implements Error {}
    public record LanguageCountMismatch(String language, int expected, int actual) implements Error {}
    public record UnsupportedSchemaTag(int tag) implements Error {}
    public record MissingBlock(String block) implements Error {}
    public record MissingPackageName() implements Error {}
    public record InvalidPackageName(String name) implements Error {}
    public record InvalidDependency(String pack, String dependency) implements Error {}
    public record MissingDependency(String pack, String dependency) implements Error {}
    public record DuplicatePackage(String pack) implements Error {}
    public record MissingBasePackage() implements Error {}
    public record ForeignLanguageResource(String pack, String resourcePack) implements Error {}
    public record EmptyLanguageResource(String pack) implements Error {}
    public record UnexpectedLanguageBlock(String block) implements Error {}

    private final List<Error> errors = new ArrayList<>();

    public List<Error> getErrors() { return List.copyOf(errors); }
    public void checkErrors() {
        if (!errors.isEmpty()) {
            throw new ConfigErr(errors);
        }
    }

    private void add(Error error) { errors.add(error); }
    private ConfigErr fail(Error error) { add(error); return new ConfigErr(errors); }

    public void missingReference(String source, String field, String target, Object key) {
        add(new MissingReference(source, field, target, key));
    }

    public void duplicateKey(String field, Object key) { add(new DuplicateKey(field, key)); }
    public void duplicateRecord(String table, String pack, Object key) { add(new DuplicateRecord(table, pack, key)); }

    public void duplicateUniqueKey(String table, String field, Object rowKey, Object key, List<String> packs) {
        add(new DuplicateUniqueKey(table, field, rowKey, key, packs));
    }

    public void invalidSeq(String table, String field, Object key, int value, int size, List<String> packs) {
        add(new InvalidSeq(table, field, key, value, size, packs));
    }

    public void unknownSymbol(String table, String symbol, Object key, List<String> packs) {
        add(new UnknownSymbol(table, symbol, key, packs));
    }

    public void changedSymbolId(String table, String symbol, int expected, int actual, List<String> packs) {
        add(new ChangedSymbolId(table, symbol, expected, actual, packs));
    }

    public void missingSymbol(String table, String symbol, Object key) {
        add(new MissingSymbol(table, symbol, key));
    }

    public void schemaMismatch(String pack, String reason) {
        add(new SchemaMismatch(pack, reason));
    }

    public ConfigErr unknownVariant(String union, String tag) { return fail(new UnknownVariant(union, tag)); }
    ConfigErr unsupportedVersion(int version, int supported) { return fail(new UnsupportedVersion(version, supported)); }
    ConfigErr outOfBounds(int length, long remaining) { return fail(new OutOfBounds(length, remaining)); }
    ConfigErr unconsumedBytes(long remaining) { return fail(new UnconsumedBytes(remaining)); }
    ConfigErr invalidCount(int count, int minimumBytes, long remaining) { return fail(new InvalidCount(count, minimumBytes, remaining)); }
    ConfigErr invalidPoolIndex(String pool, int index, int size) { return fail(new InvalidPoolIndex(pool, index, size)); }
    ConfigErr languageCountMismatch(String language, int expected, int actual) { return fail(new LanguageCountMismatch(language, expected, actual)); }
    ConfigErr unsupportedSchemaTag(int tag) { return fail(new UnsupportedSchemaTag(tag)); }

    void invalidPackageName(String name) { add(new InvalidPackageName(name)); }
    void invalidDependency(String pack, String dependency) { add(new InvalidDependency(pack, dependency)); }
    void missingDependency(String pack, String dependency) { add(new MissingDependency(pack, dependency)); }
    void duplicatePackage(String pack) { add(new DuplicatePackage(pack)); }
    void missingBasePackage() { add(new MissingBasePackage()); }
    void foreignLanguageResource(String pack, String resourcePack) { add(new ForeignLanguageResource(pack, resourcePack)); }
    void emptyLanguageResource(String pack) { add(new EmptyLanguageResource(pack)); }
    void unexpectedLanguageBlock(String block) { add(new UnexpectedLanguageBlock(block)); }
}
