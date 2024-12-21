package configgen.genjava;

import configgen.ctx.Context;
import configgen.ctx.LangSwitch;
import configgen.gen.Generator;
import configgen.ctx.LangSwitchRuntime;
import configgen.gen.Parameter;
import configgen.util.CachedFileOutputStream;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static configgen.value.CfgValue.*;

public final class GenJavaData extends Generator {
    private final File file;

    public GenJavaData(Parameter parameter) {
        super(parameter);
        file = new File(parameter.get("file", "config.data"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        LangSwitch langSwitch = ctx.nullableLangSwitch();
        try (ConfigOutput output = new ConfigOutput(new DataOutputStream(new CachedFileOutputStream(file, 2048 * 1024)))) {
            Schema schema = SchemaParser.parse(cfgValue, langSwitch);
            schema.write(output);
            LangSwitchRuntime langSwitchRuntime = langSwitch != null ?
                    new LangSwitchRuntime(langSwitch) : null;
            writeCfgValue(cfgValue, output, langSwitchRuntime);
        }
    }

    private static void writeCfgValue(CfgValue cfgValue, ConfigOutput output, LangSwitchRuntime langSwitchRuntime) throws IOException {
        int cnt = 0;
        for (VTable vTable : cfgValue.tables()) {
            if (GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(vTable.schema())) {
                Logger.verbose2("ignore write data" + vTable.name());
            } else {
                cnt++;
            }
        }

        output.writeInt(cnt);
        for (VTable vTable : cfgValue.sortedTables()) {
            if (GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(vTable.schema())) {
                continue;
            }

            if (langSwitchRuntime != null) {
                langSwitchRuntime.enterTable(vTable.name());
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ConfigOutput otherOutput = new ConfigOutput(new DataOutputStream(byteArrayOutputStream))) {
                otherOutput.writeInt(vTable.valueList().size());
                for (VStruct v : vTable.valueList()) {
                    writeValue(v, otherOutput, langSwitchRuntime);
                }
                byte[] bytes = byteArrayOutputStream.toByteArray();
                output.writeStr(vTable.name());
                output.writeInt(bytes.length);
                output.write(bytes, 0, bytes.length);
            }
        }
    }


    private static void writeValue(Value value, ConfigOutput output, LangSwitchRuntime langSwitchRuntime) {
        switch (value) {
            case VBool vBool -> output.writeBool(vBool.value());
            case VInt vInt -> output.writeInt(vInt.value());
            case VLong vLong -> output.writeLong(vLong.value());
            case VFloat vFloat -> output.writeFloat(vFloat.value());
            case VString vStr -> output.writeStr(vStr.value());
            case VText vText -> {
                if (langSwitchRuntime != null) {
                    //这里全部写进去，作为一个Text的Bean
                    String[] i18nStrings = langSwitchRuntime.findAllLangText(vText.value());
                    for (String i18nStr : i18nStrings) {
                        output.writeStr(i18nStr);
                    }
                } else {
                    output.writeStr(vText.value());
                }
            }
            case VStruct vStruct -> {
                for (Value v : vStruct.values()) {
                    writeValue(v, output, langSwitchRuntime);
                }
            }
            case VInterface vInterface -> {
                output.writeStr(vInterface.child().name());
                for (Value v : vInterface.child().values()) {
                    writeValue(v, output, langSwitchRuntime);
                }
            }
            case VList vList -> {
                output.writeInt(vList.valueList().size());
                for (SimpleValue v : vList.valueList()) {
                    writeValue(v, output, langSwitchRuntime);
                }
            }

            case VMap vMap -> {
                output.writeInt(vMap.valueMap().size());
                for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                    writeValue(e.getKey(), output, langSwitchRuntime);
                    writeValue(e.getValue(), output, langSwitchRuntime);
                }
            }
        }
    }
}
