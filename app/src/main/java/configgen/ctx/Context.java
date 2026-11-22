package configgen.ctx;

import org.jspecify.annotations.Nullable;

import configgen.data.*;
import configgen.i18n.*;
import configgen.util.Logger;
import configgen.schema.*;
import configgen.schema.CfgSchemas;
import configgen.value.CfgValue;
import configgen.value.ValueEnv;
import configgen.value.CfgValueParser;
import configgen.value.CfgValueErrs;

import java.nio.file.Path;
import java.util.Objects;


public class Context {
    public record ContextCfg(Path dataDir,
                             @Nullable ExplicitDir explicitDir,
                             HeadRow headRow,
                             String csvOrTsvDefaultEncoding,
                             I18nCfg i18n,
                             boolean allowValueErr) {

        public ContextCfg {
            Objects.requireNonNull(dataDir);
            Objects.requireNonNull(headRow);
            Objects.requireNonNull(csvOrTsvDefaultEncoding);
            Objects.requireNonNull(i18n);
        }

        public static ContextCfg of(Path dataDir) {
            return new ContextCfg(dataDir, null, HeadRows.A2_Default, "UTF-8",
                    I18nCfg.Disabled.INSTANCE, false);
        }
    }

    public sealed interface I18nCfg permits I18nCfg.Disabled, I18nCfg.Replace, I18nCfg.Switchable {
        enum Disabled implements I18nCfg {
            INSTANCE
        }

        record Replace(Path path) implements I18nCfg {
            public Replace {
                Objects.requireNonNull(path);
            }
        }

        record Switchable(Path dir, String defaultLang) implements I18nCfg {
            public Switchable {
                Objects.requireNonNull(dir);
                Objects.requireNonNull(defaultLang);
            }
        }
    }

    private sealed interface I18nState permits I18nState.Disabled, I18nState.Replace, I18nState.Switchable {
        enum Disabled implements I18nState {
            INSTANCE
        }

        record Replace(TranslationCatalog catalog) implements I18nState {}

        record Switchable(TranslationBundle translationBundle) implements I18nState {}
    }

    private final ContextCfg contextCfg;
    private final boolean usesSharedSchema;
    private final I18nState i18nState;

    private final DirectoryStructure sourceStructure;

    private final ExcelReader excelReader;
    private final ReadCsv csvReader;
    private final CfgSchema cfgSchema;
    private volatile CfgData cfgData;
    // 由 makeValue 和 updateDataAndValue 共用的 this 锁保护。
    private @Nullable CachedValue cachedValue;

    private record CachedValue(@Nullable String tag, CfgValue value, boolean strictlyValidated) {
        boolean canServe(@Nullable String requestedTag, boolean allowValueErr) {
            return Objects.equals(tag, requestedTag) && (strictlyValidated || allowValueErr);
        }
    }

    public record LoadResult(Context context, boolean schemaRewritten) {}

    public static Context load(Path dataDir) {
        return load(ContextCfg.of(dataDir));
    }

    public static Context load(ContextCfg cfg) {
        return load(cfg, new DirectoryStructure(cfg.dataDir, cfg.explicitDir), null).context();
    }

    /** Read sparse package data without aligning or writing the shared schema. */
    public static Context load(ContextCfg cfg, CfgSchema sharedSchema) {
        return load(cfg, new DirectoryStructure(cfg.dataDir, cfg.explicitDir), sharedSchema).context();
    }

    private Context(ContextCfg cfg, DirectoryStructure sourceStructure, I18nState i18nState,
                    ReadCsv csvReader, CfgSchema schema, CfgData data, boolean usesSharedSchema) {
        this.contextCfg = cfg;
        this.usesSharedSchema = usesSharedSchema;
        this.sourceStructure = sourceStructure;
        this.i18nState = i18nState;
        this.excelReader = ReadByFastExcel.INSTANCE;
        this.csvReader = csvReader;
        this.cfgSchema = schema;
        this.cfgData = data;
    }

    private static LoadResult load(ContextCfg cfg, DirectoryStructure sourceStructure, @Nullable CfgSchema sharedSchema) {
        I18nState i18nState = switch (cfg.i18n) {
            case I18nCfg.Disabled _ -> I18nState.Disabled.INSTANCE;
            case I18nCfg.Replace replace -> new I18nState.Replace(TranslationCatalog.read(replace.path()));
            case I18nCfg.Switchable switchable ->
                    new I18nState.Switchable(TranslationBundle.read(switchable.dir(), switchable.defaultLang()));
        };

        ReadCsv csvReader = new ReadCsv(cfg.csvOrTsvDefaultEncoding);
        CfgDataReader dataReader = new CfgDataReader(cfg.headRow, csvReader, ReadByFastExcel.INSTANCE);
        if (sharedSchema != null) {
            sharedSchema.requireResolved();
            CfgSchemaErrs errs = CfgSchemaErrs.of();
            CfgData cfgData = dataReader.readCfgData(sourceStructure.getExcelFiles(), sharedSchema, errs);
            errs.checkErrors("fixed schema data");
            for (String table : cfgData.tables().keySet()) {
                var tableSchema = sharedSchema.findTable(table);
                if (tableSchema == null) throw new IllegalArgumentException("Unknown table outside shared schema: " + table);
                if (tableSchema.meta().isJson()) throw new IllegalArgumentException("JSON table cannot also use CSV/Excel data: " + table);
            }
            for (String table : sourceStructure.jsonTableNames()) {
                var tableSchema = sharedSchema.findTable(table);
                if (!sourceStructure.jsonFilesOf(table).isEmpty() && (tableSchema == null || !tableSchema.meta().isJson())) throw new IllegalArgumentException("Unknown JSON table outside shared schema: " + table);
            }
            Context context = new Context(cfg, sourceStructure, i18nState, csvReader, sharedSchema, cfgData, true);
            return new LoadResult(context, false);
        }
        SchemaAndData loaded = readSchemaAndData(cfg, sourceStructure, dataReader);
        boolean schemaRewritten = !loaded.isAligned();
        if (schemaRewritten) {
            Logger.profile("schema aligned by data");
            CfgSchemas.writeToDir(sourceStructure.getRootDir().resolve(DirectoryStructure.ROOT_CONFIG_FILENAME),
                    loaded.alignedSchema());
            sourceStructure = sourceStructure.reload();
            Logger.profile("schema write");
            loaded = readSchemaAndData(cfg, sourceStructure, dataReader);
            if (!loaded.isAligned()) {
                loaded.schema().printDiff(loaded.alignedSchema());
                throw new RuntimeException("schema align failed");
            }
        }
        Context context = new Context(cfg, sourceStructure, i18nState, csvReader, loaded.schema(), loaded.data(), false);
        return new LoadResult(context, schemaRewritten);
    }

    private record SchemaAndData(CfgSchema schema, CfgData data, CfgSchema alignedSchema) {
        boolean isAligned() {
            return schema.equals(alignedSchema);
        }
    }

    private static SchemaAndData readSchemaAndData(ContextCfg cfg, DirectoryStructure sourceStructure,
                                                  CfgDataReader dataReader) {
        CfgSchema schema = CfgSchemas.readFromDir(sourceStructure.getCfgFiles());
        Logger.profile("schema read");

        CfgSchemaErrs errs = schema.resolve();
        errs.checkErrors("schema");
        schema.verbosePrintStat();
        Logger.profile("schema resolve");


        CfgSchemaErrs alignErr = CfgSchemaErrs.of();
        CfgData data = dataReader.readCfgData(sourceStructure.getExcelFiles(), schema, alignErr);
        data.verbosePrintStat();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(cfg.headRow()).align(schema, data, alignErr);
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.checkErrors("aligned schema");


        return new SchemaAndData(schema, data, alignedSchema);
    }

    public LoadResult reloadFrom(DirectoryStructure structure) {
        return load(contextCfg, structure, usesSharedSchema ? cfgSchema : null);
    }

    public ContextCfg contextCfg() {
        return contextCfg;
    }

    public boolean usesSharedSchema() { return usesSharedSchema; }

    public DirectoryStructure sourceStructure() {
        return sourceStructure;
    }

    public Path rootDir() {
        return sourceStructure.getRootDir();
    }

    public ExcelReader excelReader() {
        return excelReader;
    }

    public ReadCsv csvReader() {
        return csvReader;
    }

    /**
     * @return 完整的（非partial）schema
     */
    public CfgSchema cfgSchema() {
        return cfgSchema;
    }

    /**
     * @return 完整的data
     */
    public CfgData cfgData() {
        return cfgData;
    }

    /**
     * 直接国际化,直接改成对应国家语言
     */
    public @Nullable TranslationCatalog translationCatalog() {
        return i18nState instanceof I18nState.Replace(TranslationCatalog catalog) ? catalog : null;
    }

    /**
     * 这个是要实现客户端可在多国语言间切换语言，所以客户端服务器都需要完整的多国语言信息，而不能如i18n那样直接替换
     */
    public @Nullable TranslationBundle translationBundle() {
        return i18nState instanceof I18nState.Switchable(TranslationBundle translationBundle) ? translationBundle : null;
    }

    public CfgValue makeValue() {
        return makeValue(null);
    }

    public CfgValue makeValue(@Nullable String tag) {
        return makeValue(tag, contextCfg.allowValueErr);
    }

    public synchronized CfgValue makeValue(@Nullable String tag, boolean allowValueErr) {
        if (tag != null && tag.isEmpty()) {
            throw new IllegalArgumentException("tag不能为空");
        }

        CachedValue cached = cachedValue;
        if (cached != null && cached.canServe(tag, allowValueErr)) {
            return cached.value();
        }
        cachedValue = null; // 让旧值可以尽快回收

        CfgSchema tagSchema;
        if (tag != null) {
            CfgSchemaErrs errs = CfgSchemaErrs.of();
            tagSchema = new CfgSchemaFilterByTag(cfgSchema, tag, errs).filter();
            new CfgSchemaResolver(tagSchema, errs).resolve();
            errs.checkErrors(String.format("[%s] filtered schema", tag));
            Logger.profile(String.format("schema filtered by %s", tag));
        } else {
            tagSchema = cfgSchema;
        }

        CfgValueErrs valueErrs = CfgValueErrs.of();
        ValueEnv env = new ValueEnv(cfgSchema, cfgData, contextCfg.headRow(), translationCatalog(), sourceStructure);
        CfgValueParser clientValueParser = new CfgValueParser(tagSchema, env, valueErrs);
        CfgValue cfgValue = clientValueParser.parseCfgValue();
        String prefix = tag == null ? "value" : String.format("[%s] filtered value", tag);
        valueErrs.checkErrors(prefix, allowValueErr);

        cachedValue = new CachedValue(tag, cfgValue, !allowValueErr);
        return cfgValue;
    }

    // 增量校验不会重新解析未编辑的表。严格调用须重新校验整份配置。
    public synchronized void updateDataAndValue(CfgData cfgData, CfgValue cfgValue) {
        this.cfgData = cfgData;
        this.cachedValue = new CachedValue(null, cfgValue, false);
    }
}
