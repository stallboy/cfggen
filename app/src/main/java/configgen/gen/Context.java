package configgen.gen;

import configgen.util.Logger;
import configgen.data.CfgData;
import configgen.data.CfgDataReader;
import configgen.data.CfgSchemaAlignToData;
import configgen.schema.*;
import configgen.schema.cfg.Cfgs;
import configgen.value.CfgValue;
import configgen.value.CfgValueParser;
import configgen.value.ValueErrs;

import java.nio.file.Path;
import java.util.Objects;


public class Context {

    private final CfgSchema cfgSchema;
    private final CfgData cfgData;

    /**
     * 直接国际化,直接改成对应国家语言
     */
    private I18n i18n = null;

    /**
     * 这个是要实现客户端可在多国语言间切换语言，所以客户端服务器都需要完整的多国语言信息，而不能如i18n那样直接替换
     */
    private LangSwitch langSwitch = null;


    /**
     * 优化，避免gen多次时，重复生成value
     * 注意这里不再立马生成fullValue，因为很费内存，在用到时再生成。
     */
    private CfgValue lastCfgValue;
    private String lastCfgValueTag;

    public Context(Path dataDir, int headRow, String defaultEncoding) {
        Path cfgPath = dataDir.resolve("config.cfg");
        CfgSchema schema = Cfgs.readFrom(cfgPath, true);
        Logger.profile("schema read");
        SchemaErrs errs = schema.resolve();
        errs.print();
        Stat stat = new SchemaStat(schema);
        stat.print();
        Logger.profile("schema resolve");


        CfgData data = CfgDataReader.INSTANCE.readCfgData(dataDir, schema, headRow, defaultEncoding);
        data.stat().print();
        if (Logger.verboseLevel() > 1) {
            data.print();
        }

        SchemaErrs alignErr = SchemaErrs.of();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(schema, data, alignErr).align();
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.print();
        Logger.profile("schema aligned by data");
        if (!schema.equals(alignedSchema)) {
            // schema.printDiff(alignedSchema);
            Cfgs.writeTo(cfgPath, true, alignedSchema);
            Logger.profile("schema write");
        }

        this.cfgData = data;
        this.cfgSchema = alignedSchema;
    }

    void setI18nOrLangSwitch(String i18nFile, String langSwitchDir, String i18nEncoding, boolean crlfaslf) {
        if (i18nFile != null) {
            i18n = new I18n(Path.of(i18nFile), i18nEncoding, crlfaslf);
        } else if (langSwitchDir != null) {
            langSwitch = new LangSwitch(Path.of(langSwitchDir), i18nEncoding, crlfaslf);
        }
    }

    public LangSwitch getLangSwitch() {
        return langSwitch;
    }

    public I18n getI18n() {
        return i18n;
    }

    public CfgValue makeValue() {
        return makeValue(null);
    }

    public CfgValue makeValue(String tag) {
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
            tagSchema = new CfgSchemaFilterByTag(cfgSchema, "client", errs).filter();
            new CfgSchemaResolver(tagSchema, errs).resolve();
            errs.print();
            Logger.profile(STR. "schema filtered by \{ tag }" );
        } else {
            tagSchema = cfgSchema;
        }

        ValueErrs valueErrs = ValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(tagSchema, cfgData, cfgSchema, valueErrs);
        CfgValue value = clientValueParser.parseCfgValue();
        valueErrs.print();

        lastCfgValue = value;
        lastCfgValueTag = tag;
        return lastCfgValue;
    }
}
