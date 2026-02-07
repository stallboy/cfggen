package configgen.genjava;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.i18n.LangSwitchable;
import configgen.i18n.LangSwitchableRuntime;
import configgen.gen.Parameter;
import configgen.util.CachedFileOutputStream;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public final class GenJavaData extends GeneratorWithTag {
    private final File file;

    public GenJavaData(Parameter parameter) {
        super(parameter);
        file = new File(parameter.get("file", "config.data"));
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);
        LangSwitchable langSwitch = ctx.nullableLangSwitch();
        try (ConfigOutput output = new ConfigOutput(new DataOutputStream(new CachedFileOutputStream(file, 2048 * 1024)))) {
            Schema schema = SchemaParser.parse(cfgValue, langSwitch);
            new SchemaSerializer(output).serialize(schema);
            LangSwitchableRuntime langSwitchRuntime = langSwitch != null ?
                    new LangSwitchableRuntime(langSwitch) : null;
            writeCfgValue(cfgValue, output, langSwitchRuntime);
        }
    }

    private static void writeCfgValue(CfgValue cfgValue, ConfigOutput output, LangSwitchableRuntime langSwitchRuntime) throws IOException {
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

                if (langSwitchRuntime == null) {
                    // 不需要多语言切换时，用这个会更高效
                    for (VStruct v : vTable.valueList()) {
                        writeValue(v, otherOutput);
                    }
                } else {
                    // 可能需要pk&fieldChain作为id,去取多语言的text
                    ForeachValue.foreachVTable(new ValueVisitorWithPkAndFieldChain(otherOutput, langSwitchRuntime), vTable);
                }


                byte[] bytes = byteArrayOutputStream.toByteArray();
                output.writeStr(vTable.name());
                output.writeInt(bytes.length);
                output.write(bytes, 0, bytes.length);
            }
        }
    }


    private static void writeValue(Value value, ConfigOutput output) {
        switch (value) {
            case VBool vBool -> output.writeBool(vBool.value());
            case VInt vInt -> output.writeInt(vInt.value());
            case VLong vLong -> output.writeLong(vLong.value());
            case VFloat vFloat -> output.writeFloat(vFloat.value());
            case VString vStr -> output.writeStr(vStr.value());
            case VText vText -> output.writeStr(vText.value());

            case VStruct vStruct -> {
                for (Value v : vStruct.values()) {
                    writeValue(v, output);
                }
            }
            case VInterface vInterface -> {
                output.writeStr(vInterface.child().name());
                for (Value v : vInterface.child().values()) {
                    writeValue(v, output);
                }
            }
            case VList vList -> {
                output.writeInt(vList.valueList().size());
                for (SimpleValue v : vList.valueList()) {
                    writeValue(v, output);
                }
            }

            case VMap vMap -> {
                output.writeInt(vMap.valueMap().size());
                for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                    writeValue(e.getKey(), output);
                    writeValue(e.getValue(), output);
                }
            }
        }
    }

    private record ValueVisitorWithPkAndFieldChain(
            ConfigOutput output,
            LangSwitchableRuntime langSwitchRuntime) implements ForeachValue.ValueVisitor {

        @Override
        public void visitPrimitive(PrimitiveValue primitiveValue, Value pk, List<String> fieldChain) {
            switch (primitiveValue) {
                case VBool vBool -> output.writeBool(vBool.value());
                case VInt vInt -> output.writeInt(vInt.value());
                case VLong vLong -> output.writeLong(vLong.value());
                case VFloat vFloat -> output.writeFloat(vFloat.value());
                case VString vStr -> output.writeStr(vStr.value());
                case VText vText -> {
                    //这里全部写进去，作为一个Text的Bean
                    String[] i18nStrings = langSwitchRuntime.findAllLangText(pk.packStr(), fieldChain, vText.value());
                    for (String i18nStr : i18nStrings) {
                        output.writeStr(i18nStr);
                    }
                }
            }
        }

        @Override
        public void visitVList(VList vList, Value pk, List<String> fieldChain) {
            output.writeInt(vList.valueList().size());
        }

        @Override
        public void visitVMap(VMap vMap, Value pk, List<String> fieldChain) {
            output.writeInt(vMap.valueMap().size());
        }

        @Override
        public void visitVInterface(VInterface vInterface, Value pk, List<String> fieldChain) {
            output.writeStr(vInterface.child().name());
        }

        @Override
        public void visitVStruct(VStruct vStruct, Value pk, List<String> fieldChain) {

        }
    }
}
