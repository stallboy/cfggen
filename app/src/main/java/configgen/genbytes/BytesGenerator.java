package configgen.genbytes;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.genjava.ConfigOutput;
import configgen.genjava.Schema;
import configgen.genjava.SchemaParser;
import configgen.genjava.SchemaSerializer;
import configgen.i18n.LangSwitchable;
import configgen.i18n.LangSwitchableRuntime;
import configgen.util.CachedFileOutputStream;
import configgen.util.XorCipherOutputStream;
import configgen.value.CfgValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class BytesGenerator extends GeneratorWithTag {
    private final String dir;
    private final String cipher;
    private final boolean hasSchema;
    private final boolean isLangSeparated;

    public BytesGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", ".");
        cipher = parameter.get("cipher", "");
        hasSchema = parameter.has("schema");
        isLangSeparated = parameter.has("langSeparated");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        LangSwitchable langSwitch = ctx.nullableLangSwitch();

        // 初始化运行时
        LangSwitchableRuntime langSwitchRuntime = null;
        LangTextPool langTextPool;

        if (langSwitch != null) {
            langSwitchRuntime = new LangSwitchableRuntime(langSwitch);
            langTextPool = new LangTextPool(langSwitch.languages());
        } else {
            langTextPool = new LangTextPool(List.of("default"));
        }

        // schema
        ByteArrayOutputStream schemaContent = null;
        if (hasSchema) {
            // 序列化 schema（使用小端序 ConfigOutput）
            schemaContent = new ByteArrayOutputStream(1024 * 64);
            try (ConfigOutput schemaOutput = new ConfigOutput(schemaContent)) {
                Schema schema = SchemaParser.parse(cfgValue, langSwitch);
                new SchemaSerializer(schemaOutput).serialize(schema);
            }
        }

        // 收集表数据和文本
        StringPool stringPool = new StringPool(); // 必然启用
        ByteArrayOutputStream content = new ByteArrayOutputStream(1024 * 1024);
        try (ConfigOutput valueStream = new ConfigOutput(content)) {
            CfgValueSerializer serializer = new CfgValueSerializer(valueStream,
                    stringPool, langTextPool, langSwitchRuntime);
            serializer.serialize(cfgValue);
        }

        // 写入文件
        if (isLangSeparated && langTextPool.getTextPools().length > 1) {
            // 分离模式：主文件 + 语言文件
            writeConfigBytes(schemaContent, stringPool, langTextPool, true, content);
            writeRestLangFiles(langTextPool);
        } else {
            // 合并模式：单个文件
            writeConfigBytes(schemaContent, stringPool, langTextPool, false, content);
        }
    }

    private void writeConfigBytes(ByteArrayOutputStream schemaContent,
                                  StringPool stringPool,
                                  LangTextPool langTextPool,
                                  boolean isOnlyFirstLang,
                                  ByteArrayOutputStream content) {

        try (ConfigOutput configOutput = createConfigOutput("config.bytes")) {
            // 1. Schema（可选）
            if (schemaContent != null) {
                // 序列化 schema
                byte[] schemaBytes = schemaContent.toByteArray();
                configOutput.writeInt(schemaBytes.length);
                configOutput.writeRawBytes(schemaBytes);
            } else {
                // 表明 无schema
                configOutput.writeInt(0);
            }

            // 2. StringPool
            stringPool.serialize(configOutput);

            // 3. LangTextPool
            if (isOnlyFirstLang) {
                langTextPool.serializeFirst(configOutput);
            } else {
                langTextPool.serialize(configOutput);
            }
            // 4. 表数据
            configOutput.writeRawBytes(content.toByteArray());
        }
    }


    private void writeRestLangFiles(LangTextPool langTextPool) {
        for (int i = 1; i < langTextPool.getTextPools().length; i++) {
            TextPool textPool = langTextPool.getTextPools()[i];
            try (ConfigOutput out = createConfigOutput(textPool.getLangName() + ".bytes")) {
                textPool.serialize(out);
            }
        }
    }

    private ConfigOutput createConfigOutput(String fileName) {
        CachedFileOutputStream stream = new CachedFileOutputStream((Path.of(dir, fileName)));
        if (cipher.isEmpty()) {
            return new ConfigOutput(stream);
        } else {
            return new ConfigOutput(new XorCipherOutputStream(stream, cipher));
        }
    }
}
