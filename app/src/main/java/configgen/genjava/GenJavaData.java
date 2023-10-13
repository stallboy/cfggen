package configgen.genjava;

import configgen.util.Logger;
import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.LangSwitch;
import configgen.gen.Parameter;
import configgen.util.CachedFileOutputStream;
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
        file = new File(parameter.get("file", "config.data", "文件名"));
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        try (ConfigOutput output = new ConfigOutput(new DataOutputStream(new CachedFileOutputStream(file, 2048 * 1024)))) {
            Schema schema = SchemaParser.parse(cfgValue, ctx.getLangSwitch());
            schema.write(output);
            writeCfgValue(cfgValue, ctx.getLangSwitch(), output);
        }
    }

    private void writeCfgValue(CfgValue cfgValue, LangSwitch nullableLS, ConfigOutput output) throws IOException {
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
            if (nullableLS != null) {
                nullableLS.enterTable(vTable.name());
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ConfigOutput otherOutput = new ConfigOutput(new DataOutputStream(byteArrayOutputStream))) {
                otherOutput.writeInt(vTable.valueList().size());
                for (VStruct v : vTable.valueList()) {
                    writeValue(v, nullableLS, otherOutput);
                }
                byte[] bytes = byteArrayOutputStream.toByteArray();
                output.writeStr(vTable.name());
                output.writeInt(bytes.length);
                output.write(bytes, 0, bytes.length);
            }
        }
    }

    private void writeValue(Value value, LangSwitch nullableLS, ConfigOutput output) {
        switch (value) {
            case VBool vBool -> output.writeBool(vBool.value());
            case VInt vInt -> output.writeInt(vInt.value());
            case VLong vLong -> output.writeLong(vLong.value());
            case VFloat vFloat -> output.writeFloat(vFloat.value());
            case VString vStr -> output.writeStr(vStr.value());
            case VText vText -> {
                if (nullableLS != null) { //这里全部写进去，作为一个Text的Bean
                    String[] i18nStrings = nullableLS.findAllLangText(vText.value());
                    for (String i18nStr : i18nStrings) {
                        output.writeStr(i18nStr);
                    }
                } else {
                    output.writeStr(vText.value());
                }
            }
            case VStruct vStruct -> {
                for (Value v : vStruct.values()) {
                    writeValue(v, nullableLS, output);
                }
            }
            case VInterface vInterface -> {
                output.writeStr(vInterface.child().name());
                for (Value v : vInterface.child().values()) {
                    writeValue(v, nullableLS, output);
                }
            }
            case VList vList -> {
                output.writeInt(vList.valueList().size());
                for (SimpleValue v : vList.valueList()) {
                    writeValue(v, nullableLS, output);
                }
            }

            case VMap vMap -> {
                output.writeInt(vMap.valueMap().size());
                for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                    writeValue(e.getKey(), nullableLS, output);
                    writeValue(e.getValue(), nullableLS, output);
                }
            }
        }
    }
}
