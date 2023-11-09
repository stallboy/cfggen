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

    private final CfgSchema cfgSchema;
    private final CfgData cfgData;
    private final boolean checkComma;

    /**
     * 直接国际化,直接改成对应国家语言
     */
    private TextI18n i18n = null;

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

    public Context(Path dataDir, int headRow, boolean checkComma, String defaultEncoding) {
        this.checkComma = checkComma;
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


        CfgData data = CfgDataReader.INSTANCE.readCfgData(dataDir, schema, headRow, checkComma, defaultEncoding);
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
            i18n = LangSwitch.loadTextI18n(Path.of(i18nFile), i18nEncoding, crlfaslf);
        } else if (langSwitchDir != null) {
            langSwitch = LangSwitch.loadLangSwitch(Path.of(langSwitchDir), defaultLang, i18nEncoding, crlfaslf);
        }
    }

    public LangSwitch getLangSwitch() {
        return langSwitch;
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
            tagSchema = new CfgSchemaFilterByTag(cfgSchema, tag, errs).filter();
            new CfgSchemaResolver(tagSchema, errs).resolve();
            errs.print(STR. "[\{ tag }] filtered schema" );
            Logger.profile(STR. "schema filtered by \{ tag }" );
        } else {
            tagSchema = cfgSchema;
        }

        ValueErrs valueErrs = ValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(tagSchema, cfgData, cfgSchema, i18n, checkComma, valueErrs);
        CfgValue value = clientValueParser.parseCfgValue();
        String prefix = tag == null ? "value" : String.format("[%s] filtered value", tag);
        valueErrs.print(prefix);

        lastCfgValue = value;
        lastCfgValueTag = tag;
        return lastCfgValue;
    }
}
