package configgen.ctx;

import org.jspecify.annotations.Nullable;
import configgen.data.*;
import configgen.i18n.*;
import configgen.layout.DirectoryStructure;
import configgen.layout.SchemaLayout;
import configgen.util.Logger;
import configgen.schema.*;
import configgen.schema.CfgSchemaErrs.JsonTableNotSupportExcel;
import configgen.schema.CfgSchemaErrs.DataTableNotInSchema;
import configgen.schema.CfgSchemaErrs.DataTableNotMarkedJson;
import configgen.value.CfgValue;
import configgen.value.ValueEnv;
import configgen.value.CfgValueParser;
import configgen.value.CfgValueErrs;

import java.nio.file.Path;
import java.util.Objects;


public class Context {
    public record ContextCfg(Path dataDir,
                             HeadRow headRow,
                             String csvDefaultEncoding,
                             I18nCfg i18n,
                             boolean allowValueErr) {

        public ContextCfg {
            Objects.requireNonNull(dataDir);
            Objects.requireNonNull(headRow);
            Objects.requireNonNull(csvDefaultEncoding);
            Objects.requireNonNull(i18n);
        }

        public static ContextCfg of(Path dataDir) {
            return new ContextCfg(dataDir, HeadRows.A2_Default, "UTF-8",
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

        record Replace(TranslationCatalog catalog) implements I18nState {
        }

        record Switchable(TranslationBundle translationBundle) implements I18nState {
        }
    }

    private final ContextCfg contextCfg;
    private final I18nState i18nState;

    private final DirectoryStructure sourceStructure;
    private final boolean alignSchemaOnReload;
    private final boolean usesSharedSchema;

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

    public record LoadResult(Context context, boolean schemaRewritten) {
    }

    public static Context load(Path dataDir) {
        return load(ContextCfg.of(dataDir));
    }

    public static Context load(ContextCfg cfg) {
        DirectoryStructure structure = new DirectoryStructure(cfg.dataDir);
        return loadFixedSchema(cfg, structure, readSchema(structure), false);
    }

    public static Context load(ContextCfg cfg, CfgSchema sharedSchema) {
        Objects.requireNonNull(sharedSchema);
        return loadFixedSchema(cfg, new DirectoryStructure(cfg.dataDir), sharedSchema, true);
    }

    public static Context loadAndAlignSchema(ContextCfg cfg) {
        return loadAndAlignSchema(cfg, new DirectoryStructure(cfg.dataDir)).context();
    }

    private Context(ContextCfg cfg, DirectoryStructure sourceStructure, I18nState i18nState,
                    ReadCsv csvReader, CfgSchema schema, CfgData data,
                    boolean alignSchemaOnReload, boolean usesSharedSchema) {
        this.contextCfg = cfg;
        this.alignSchemaOnReload = alignSchemaOnReload;
        this.usesSharedSchema = usesSharedSchema;
        this.sourceStructure = sourceStructure;
        this.i18nState = i18nState;
        this.excelReader = ReadByFastExcel.INSTANCE;
        this.csvReader = csvReader;
        this.cfgSchema = schema;
        this.cfgData = data;
    }

    private static I18nState loadI18n(I18nCfg i18n) {
        return switch (i18n) {
            case I18nCfg.Disabled _ -> I18nState.Disabled.INSTANCE;
            case I18nCfg.Replace replace -> new I18nState.Replace(TranslationCatalog.read(replace.path()));
            case I18nCfg.Switchable switchable ->
                    new I18nState.Switchable(TranslationBundle.read(switchable.dir(), switchable.defaultLang()));
        };
    }

    private static CfgDataReader createDataReader(ContextCfg cfg) {
        return new CfgDataReader(cfg.headRow, new ReadCsv(cfg.csvDefaultEncoding),
                ReadByFastExcel.INSTANCE);
    }

    private static Context loadFixedSchema(ContextCfg cfg, DirectoryStructure sourceStructure,
                                           CfgSchema schema, boolean usesSharedSchema) {
        I18nState i18nState = loadI18n(cfg.i18n);
        CfgDataReader dataReader = createDataReader(cfg);
        schema.requireResolved();
        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgData data = dataReader.readCfgData(sourceStructure.getExcelFiles(), schema, errs);
        data.verbosePrintStat();
        for (CfgData.DTable table : data.tables().values()) {
            TableSchema tableSchema = schema.findTable(table.tableName());
            if (tableSchema == null) {
                errs.addErr(new DataTableNotInSchema(table.tableName(),
                        table.rawSheets().stream().map(CfgData.DRawSheet::id).toList()));
            } else if (tableSchema.isJson()) {
                errs.addErr(new JsonTableNotSupportExcel(table.tableName(),
                        table.rawSheets().stream().map(CfgData.DRawSheet::id).toList()));
            }
        }
        for (String table : sourceStructure.jsonTableNames().stream().sorted().toList()) {
            var files = sourceStructure.getJsonFilesByTable(table);
            if (files.isEmpty()) {
                continue;
            }
            TableSchema tableSchema = schema.findTable(table);
            if (tableSchema == null || !tableSchema.isJson()) {
                var paths = files.stream().map(file -> file.relativePath().toString()).toList();
                errs.addErr(tableSchema == null ? new DataTableNotInSchema(table, paths)
                        : new DataTableNotMarkedJson(table, paths));
            }
        }
        errs.checkErrors("fixed schema data");
        return new Context(cfg, sourceStructure, i18nState, dataReader.csvReader(), schema, data,
                false, usesSharedSchema);
    }

    private static LoadResult loadAndAlignSchema(ContextCfg cfg, DirectoryStructure sourceStructure) {
        I18nState i18nState = loadI18n(cfg.i18n);
        CfgDataReader dataReader = createDataReader(cfg);
        SchemaAndData loaded = readSchemaAndData(sourceStructure, dataReader);
        if (loaded.isAligned()) {
            Context context = new Context(cfg, sourceStructure, i18nState, dataReader.csvReader(),
                    loaded.schema(), loaded.data(), true, false);
            return new LoadResult(context, false);
        }

        Logger.profile("schema aligned by data");
        CfgSchemas.writeToDir(SchemaLayout.cfgRootFile(sourceStructure.getRootDir()), loaded.alignedSchema());
        DirectoryStructure rewrittenStructure = sourceStructure.reload();
        Logger.profile("schema write");

        SchemaAndData rewritten = readSchemaAndData(rewrittenStructure, dataReader);
        if (!rewritten.isAligned()) {
            rewritten.schema().printDiff(rewritten.alignedSchema());
            throw new RuntimeException("schema align failed");
        }
        Context context = new Context(cfg, rewrittenStructure, i18nState, dataReader.csvReader(),
                rewritten.schema(), rewritten.data(), true, false);
        return new LoadResult(context, true);
    }

    private record SchemaAndData(CfgSchema schema, CfgData data, CfgSchema alignedSchema) {
        boolean isAligned() {
            return schema.equals(alignedSchema);
        }
    }

    private static CfgSchema readSchema(DirectoryStructure sourceStructure) {
        CfgSchema schema = CfgSchemas.readFromDir(sourceStructure.getCfgFiles());
        Logger.profile("schema read");

        schema.resolve().checkErrors("schema");
        schema.verbosePrintStat();
        Logger.profile("schema resolve");
        return schema;
    }

    private static SchemaAndData readSchemaAndData(DirectoryStructure sourceStructure, CfgDataReader dataReader) {
        CfgSchema schema = readSchema(sourceStructure);
        CfgSchemaErrs alignErr = CfgSchemaErrs.of();
        CfgData data = dataReader.readCfgData(sourceStructure.getExcelFiles(), schema, alignErr);
        data.verbosePrintStat();
        CfgSchema alignedSchema = new CfgSchemaAlignToData().align(schema, data, alignErr);
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.checkErrors("aligned schema");
        return new SchemaAndData(schema, data, alignedSchema);
    }

    public LoadResult reloadFrom(DirectoryStructure structure) {
        if (alignSchemaOnReload) {
            return loadAndAlignSchema(contextCfg, structure);
        }
        CfgSchema schema = usesSharedSchema ? cfgSchema : readSchema(structure);
        return new LoadResult(loadFixedSchema(contextCfg, structure, schema, usesSharedSchema), false);
    }

    public ContextCfg contextCfg() {
        return contextCfg;
    }

    /** Whether the caller supplied the schema for this context. */
    public boolean usesSharedSchema() {
        return usesSharedSchema;
    }

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
        return i18nState instanceof I18nState.Switchable(
                TranslationBundle translationBundle
        ) ? translationBundle : null;
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
        ValueEnv env = new ValueEnv(cfgSchema, cfgData, contextCfg.headRow(), translationCatalog(),
                sourceStructure::getJsonFilesByTable);
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
