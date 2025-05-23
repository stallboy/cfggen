package configgen.ctx;

import configgen.data.*;
import configgen.gen.BuildSettings;
import configgen.i18n.*;
import configgen.util.Logger;
import configgen.schema.*;
import configgen.schema.CfgSchemas;
import configgen.value.CfgValue;
import configgen.value.CfgValueParser;
import configgen.value.CfgValueErrs;

import java.nio.file.Path;
import java.util.Objects;


public class Context {
    public record ContextCfg(Path dataDir,
                             boolean tryUsePoi,
                             int headRow,
                             String csvDefaultEncoding,

                             String i18nFilename,
                             boolean crLfAsLf,
                             String langSwitchDir,
                             String langSwitchDefaultLang) {

        public ContextCfg {
            Objects.requireNonNull(dataDir);
            if (headRow < 2) {
                throw new IllegalArgumentException("head row < 2");
            }
            Objects.requireNonNull(csvDefaultEncoding);
        }

        public static ContextCfg of(Path dataDir) {
            return of(dataDir, "UTF-8");
        }

        public static ContextCfg of(Path dataDir, String csvDefaultEncoding) {
            return new ContextCfg(dataDir, false, 2, csvDefaultEncoding, null, false, null, null);
        }
    }

    private final ContextCfg contextCfg;

    private DirectoryStructure sourceStructure;
    private LangTextFinder nullableLangTextFinder;
    private LangSwitch nullableLangSwitch;
    private CfgSchema cfgSchema;
    private CfgData cfgData;

    /**
     * 优化，避免gen多次时，重复生成value
     */
    private CfgValue lastCfgValue;
    private String lastCfgValueTag;

    public Context(Path dataDir) {
        this(ContextCfg.of(dataDir));
    }

    public Context(ContextCfg cfg) {
        this(cfg, new DirectoryStructure(cfg.dataDir));
    }

    public Context(ContextCfg cfg, DirectoryStructure sourceStructure) {
        this.contextCfg = cfg;
        this.sourceStructure = sourceStructure;

        if (cfg.i18nFilename != null) {
            nullableLangTextFinder = TextFinders.loadOneLang(cfg.i18nFilename, cfg.crLfAsLf);
        } else if (cfg.langSwitchDir != null) {
            nullableLangSwitch = TextFinders.loadLangSwitch(cfg.langSwitchDir, cfg.langSwitchDefaultLang, cfg.crLfAsLf);
        }

        ExcelReader excelReader = (cfg.tryUsePoi && BuildSettings.isIncludePoi()) ?
                BuildSettings.getPoiReader() : ReadByFastExcel.INSTANCE;
        CfgDataReader dataReader = new CfgDataReader(cfg.headRow, new ReadCsv(cfg.csvDefaultEncoding), excelReader);
        boolean ok = readSchemaAndData(dataReader, true);
        if (!ok) {
            readSchemaAndData(dataReader, false);
        }
    }

    private boolean readSchemaAndData(CfgDataReader dataReader, boolean autoFix) {
        CfgSchema schema = CfgSchemas.readFromDir(sourceStructure);
        Logger.profile("schema read");
        CfgSchemaErrs errs = schema.resolve();
        if (!errs.errs().isEmpty()) {
            errs.checkErrors("schema");
        }
        schema.verbosePrintStat();

        Logger.profile("schema resolve");

        CfgData data = dataReader.readCfgData(sourceStructure, schema);
        data.verbosePrintStat();

        CfgSchemaErrs alignErr = CfgSchemaErrs.of();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(schema, data, alignErr).align();
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.checkErrors("aligned schema");
        if (schema.equals(alignedSchema)) {
            this.cfgData = data;
            this.cfgSchema = schema;
            return true;
        } else if (autoFix) {
            Logger.profile("schema aligned by data");
            // schema.printDiff(alignedSchema);
            CfgSchemas.writeToDir(sourceStructure.getRootDir().resolve(DirectoryStructure.ROOT_CONFIG_FILENAME),
                    alignedSchema);
            sourceStructure = new DirectoryStructure(sourceStructure.getRootDir());
            Logger.profile("schema write");
            return false;
        } else {
            throw new RuntimeException("schema align failed");
        }
    }

    public ContextCfg getContextCfg() {
        return contextCfg;
    }

    public DirectoryStructure getSourceStructure() {
        return sourceStructure;
    }

    public CfgSchema cfgSchema() {
        return cfgSchema;
    }

    public CfgData cfgData() {
        return cfgData;
    }

    /**
     * 直接国际化,直接改成对应国家语言
     */
    public LangTextFinder nullableLangTextFinder() {
        return nullableLangTextFinder;
    }

    /**
     * 这个是要实现客户端可在多国语言间切换语言，所以客户端服务器都需要完整的多国语言信息，而不能如i18n那样直接替换
     */
    public LangSwitch nullableLangSwitch() {
        return nullableLangSwitch;
    }

    public CfgValue makeValue() {
        return makeValue(null);
    }

    public CfgValue makeValue(String tag) {
        return makeValue(tag, false);
    }

    public CfgValue makeValue(String tag, boolean allowErr) {
        if (tag != null && tag.isEmpty()) {
            throw new IllegalArgumentException("tag不能为空");
        }

        if (lastCfgValue != null) {
            if (Objects.equals(tag, lastCfgValueTag)) {
                return lastCfgValue;
            }
        }
        lastCfgValue = null; //让它可以被尽快gc

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
        CfgValueParser clientValueParser = new CfgValueParser(tagSchema, this, valueErrs);
        CfgValue cfgValue = clientValueParser.parseCfgValue();
        String prefix = tag == null ? "value" : String.format("[%s] filtered value", tag);
        valueErrs.checkErrors(prefix, allowErr);

        lastCfgValue = cfgValue;
        lastCfgValueTag = tag;
        return lastCfgValue;
    }
}
