package configgen.gen;

import configgen.util.Logger;
import configgen.data.CfgData;
import configgen.data.CfgDataReader;
import configgen.data.CfgSchemaAlignToData;
import configgen.schema.*;
import configgen.schema.cfg.Cfgs;
import configgen.value.TextI18n;
import configgen.value.CfgValue;
import configgen.value.CfgValueParser;
import configgen.value.ValueErrs;

import java.nio.file.Path;
import java.util.Objects;


public class Context {
    private final Path dataDir;
    private final CfgSchema cfgSchema;
    private final CfgData cfgData;

    private TextI18n nullableI18n = null;
    private LangSwitch nullableLangSwitch = null;

    public Path dataDir() {
        return dataDir;
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
    public TextI18n nullableI18n() {
        return nullableI18n;
    }

    /**
     * 这个是要实现客户端可在多国语言间切换语言，所以客户端服务器都需要完整的多国语言信息，而不能如i18n那样直接替换
     */
    public LangSwitch nullableLangSwitch() {
        return nullableLangSwitch;
    }


    public Context(Path dataDir, int headRow, boolean usePoi , String defaultEncoding) {
        this.dataDir = dataDir;
        Path cfgPath = dataDir.resolve("config.cfg");
        CfgSchema schema = Cfgs.readFrom(cfgPath, true);
        Logger.profile("schema read");
        SchemaErrs errs = schema.resolve();
        if (!errs.errs().isEmpty()) {
            errs.print("schema");
        }
        Stat stat = new SchemaStat(schema);
        stat.print();
        Logger.profile("schema resolve");


        CfgData data = CfgDataReader.INSTANCE.readCfgData(dataDir, schema, headRow, usePoi, defaultEncoding);
        data.print();

        SchemaErrs alignErr = SchemaErrs.of();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(schema, data, alignErr).align();
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.print("aligned schema");
        Logger.profile("schema aligned by data");
        if (!schema.equals(alignedSchema)) {
            // schema.printDiff(alignedSchema);
            Cfgs.writeTo(cfgPath, true, alignedSchema);
            Logger.profile("schema write");
        }

        this.cfgData = data;
        this.cfgSchema = alignedSchema;
    }

    void setI18nOrLangSwitch(String i18nFile, String i18nEncoding, boolean crlfaslf,
                             String langSwitchDir, String defaultLang) {
        if (i18nFile != null) {
            nullableI18n = LangSwitch.loadTextI18n(Path.of(i18nFile), i18nEncoding, crlfaslf);
        } else if (langSwitchDir != null) {
            nullableLangSwitch = LangSwitch.loadLangSwitch(Path.of(langSwitchDir), defaultLang, i18nEncoding, crlfaslf);
        }
    }


    /**
     * 优化，避免gen多次时，重复生成value
     * 注意这里不再立马生成fullValue，因为很费内存，在用到时再生成。
     */
    private CfgValue lastCfgValue;
    private String lastCfgValueTag;

    public CfgValue makeValue(String tag){
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
            SchemaErrs errs = SchemaErrs.of();
            tagSchema = new CfgSchemaFilterByTag(cfgSchema, tag, errs).filter();
            new CfgSchemaResolver(tagSchema, errs).resolve();
            errs.print(String.format("[%s] filtered schema", tag));
            Logger.profile(String.format("schema filtered by %s", tag));
        } else {
            tagSchema = cfgSchema;
        }

        ValueErrs valueErrs = ValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(tagSchema, this, valueErrs);
        CfgValue cfgValue = clientValueParser.parseCfgValue();
        String prefix = tag == null ? "value" : String.format("[%s] filtered value", tag);
        valueErrs.print(prefix, allowErr);

        lastCfgValue = cfgValue;
        lastCfgValueTag = tag;
        return lastCfgValue;
    }
}
