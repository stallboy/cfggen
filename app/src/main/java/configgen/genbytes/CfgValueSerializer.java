package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import configgen.i18n.LangSwitchableRuntime;
import configgen.value.CfgValue;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class CfgValueSerializer {
    private final @NotNull ConfigOutput output;
    private final LangSwitchableRuntime langSwitchRuntime;
    private final @NotNull StringPool stringPool;
    private final @NotNull LangTextPool langTextPool;

    public CfgValueSerializer(@NotNull ConfigOutput output,
                              LangSwitchableRuntime langSwitchRuntime,
                              @NotNull StringPool stringPool,
                              @NotNull LangTextPool langTextPool) {
        this.output = output;
        this.langSwitchRuntime = langSwitchRuntime;
        this.stringPool = stringPool;
        this.langTextPool = langTextPool;
    }

    public void serialize(CfgValue cfgValue) {
        // 写入表数量
        output.writeInt(cfgValue.vTableMap().size());

        for (VTable vTable : cfgValue.sortedTables()) {
            if (langSwitchRuntime != null) {
                langSwitchRuntime.enterTable(vTable.name());
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            try (ConfigOutput otherOutput = new ConfigOutput(byteArrayOutputStream)) {
                otherOutput.writeInt(vTable.valueList().size());

                if (langSwitchRuntime == null) {
                    // 不需要多语言切换时，用这个会更高效
                    for (VStruct v : vTable.valueList()) {
                        writeValue(v, otherOutput);
                    }
                } else {
                    // 可能需要pk&fieldChain作为id,去取多语言的text
                    new MultiLangVTableSerializer(otherOutput, langSwitchRuntime, stringPool, langTextPool)
                            .serialize(vTable);
                }

                byte[] bytes = byteArrayOutputStream.toByteArray();
                output.writeString(vTable.name()); // 表名不放在StringPool
                output.writeInt(bytes.length);
                output.writeRawBytes(bytes);
            }
        }
    }


    private void writeValue(Value value, ConfigOutput output) {
        switch (value) {
            case VBool vBool -> output.writeBool(vBool.value());
            case VInt vInt -> output.writeInt(vInt.value());
            case VLong vLong -> output.writeLong(vLong.value());
            case VFloat vFloat -> output.writeFloat(vFloat.value());
            case VString vStr -> writeStringInPool(output, vStr.value());
            case VText vText -> {
                int idx = langTextPool.addText(new String[]{vText.value()});
                output.writeInt(idx);
            }

            case VStruct vStruct -> {
                for (Value v : vStruct.values()) {
                    writeValue(v, output);
                }
            }
            case VInterface vInterface -> {
                writeStringInPool(output, vInterface.child().name());
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

    private void writeStringInPool(ConfigOutput out, String v) {
        int idx = stringPool.addString(v);
        out.writeInt(idx);
    }

}
