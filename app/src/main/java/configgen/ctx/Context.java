package configgen.ctx;

import configgen.data.*;
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
                             ExplicitDir explicitDir,
                             HeadRow headRow,
                             String csvOrTsvDefaultEncoding,

                             String i18nFilename,
                             String langSwitchDir,
                             String langSwitchDefaultLang) {

        public ContextCfg {
            Objects.requireNonNull(dataDir);
            Objects.requireNonNull(headRow);
            Objects.requireNonNull(csvOrTsvDefaultEncoding);
        }

        public static ContextCfg of(Path dataDir) {
            return new ContextCfg(dataDir, null, HeadRows.A2_Default, "UTF-8", null, null, null);
        }
    }

    private final ContextCfg contextCfg;

    private DirectoryStructure sourceStructure;
    private LangTextFinder nullableLangTextFinder;
    private LangSwitchable nullableLangSwitch;

    private final ExcelReader excelReader;
    private final ReadCsv csvReader;
    private CfgSchema cfgSchema;
    private volatile CfgData cfgData;

    /**
     * 优化，避免gen多次时，重复生成value
     */
    private volatile CfgValue lastCfgValue;
    private volatile String lastCfgValueTag;

    public Context(Path dataDir) {
        this(ContextCfg.of(dataDir));
    }

    public Context(ContextCfg cfg) {
        this(cfg, new DirectoryStructure(cfg.dataDir, cfg.explicitDir));
    }

    public Context(ContextCfg cfg, DirectoryStructure sourceStructure) {
        this.contextCfg = cfg;
        this.sourceStructure = sourceStructure;

        if (cfg.i18nFilename != null) {
            nullableLangTextFinder = LangTextFinder.read(Path.of(cfg.i18nFilename));
        } else if (cfg.langSwitchDir != null) {
            nullableLangSwitch = LangSwitchable.read(Path.of(cfg.langSwitchDir), cfg.langSwitchDefaultLang);
        }

        excelReader = ReadByFastExcel.INSTANCE;
        csvReader = new ReadCsv(cfg.csvOrTsvDefaultEncoding);
        CfgDataReader dataReader = new CfgDataReader(cfg.headRow, csvReader, excelReader);
        boolean ok = readSchemaAndData(dataReader, true);
        if (!ok) {
            readSchemaAndData(dataReader, false);
        }
    }

    private boolean readSchemaAndData(CfgDataReader dataReader, boolean autoFix) {
        CfgSchema schema = CfgSchemas.readFromDir(sourceStructure);
        Logger.profile("schema read");

        CfgSchemaErrs errs = schema.resolve();
        errs.checkErrors("schema");
        schema.verbosePrintStat();
        Logger.profile("schema resolve");


        CfgSchemaErrs alignErr = CfgSchemaErrs.of();
        CfgData data = dataReader.readCfgData(sourceStructure, schema, alignErr);
        data.verbosePrintStat();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(contextCfg.headRow()).align(schema, data, alignErr);
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.checkErrors("aligned schema");


        if (schema.equals(alignedSchema)) {
            this.cfgData = data;
            this.cfgSchema = schema;
            return true;
        } else if (autoFix) {
            Logger.profile("schema aligned by data");
            // schema.printDiff(alignedSchema);
            CfgSchemas.writeToDir(rootDir().resolve(DirectoryStructure.ROOT_CONFIG_FILENAME),
                    alignedSchema);
            sourceStructure = sourceStructure.reload();
            Logger.profile("schema write");
            return false;
        } else {
            throw new RuntimeException("schema align failed");
        }
    }

    public ContextCfg contextCfg() {
        return contextCfg;
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
    public LangTextFinder nullableLangTextFinder() {
        return nullableLangTextFinder;
    }

    /**
     * 这个是要实现客户端可在多国语言间切换语言，所以客户端服务器都需要完整的多国语言信息，而不能如i18n那样直接替换
     */
    public LangSwitchable nullableLangSwitch() {
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

    public void updateDataAndValue(CfgData cfgData, CfgValue cfgValue) {
        this.cfgData = cfgData;
        this.lastCfgValue = cfgValue;
        this.lastCfgValueTag = null;
    }
}
