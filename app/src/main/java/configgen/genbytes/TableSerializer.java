package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import configgen.value.CfgValue;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record TableSerializer(@NotNull ConfigOutput output,
                              @NotNull StringPool stringPool,
                              @NotNull LangTextPool langTextPool) {

    public void serialize(CfgValue.VTable vTable) {
        output.writeInt(vTable.valueList().size());
        for (CfgValue.VStruct v : vTable.valueList()) {
            writeValue(v);
        }
    }

    private void writeValue(CfgValue.Value value) {
        switch (value) {
            case CfgValue.VBool vBool -> output.writeBool(vBool.value());
            case CfgValue.VInt vInt -> output.writeInt(vInt.value());
            case CfgValue.VLong vLong -> output.writeLong(vLong.value());
            case CfgValue.VFloat vFloat -> output.writeFloat(vFloat.value());
            case CfgValue.VString vStr -> writeStringInPool(vStr.value());
            case CfgValue.VText vText -> {
                int idx = langTextPool.addText(new String[]{vText.value()});
                output.writeInt(idx);
            }

            case CfgValue.VStruct vStruct -> {
                for (CfgValue.Value v : vStruct.values()) {
                    writeValue(v);
                }
            }
            case CfgValue.VInterface vInterface -> {
                writeStringInPool(vInterface.child().name());
                for (CfgValue.Value v : vInterface.child().values()) {
                    writeValue(v);
                }
            }
            case CfgValue.VList vList -> {
                output.writeInt(vList.valueList().size());
                for (CfgValue.SimpleValue v : vList.valueList()) {
                    writeValue(v);
                }
            }

            case CfgValue.VMap vMap -> {
                output.writeInt(vMap.valueMap().size());
                for (Map.Entry<CfgValue.SimpleValue, CfgValue.SimpleValue> e : vMap.valueMap().entrySet()) {
                    writeValue(e.getKey());
                    writeValue(e.getValue());
                }
            }
        }
    }

    private void writeStringInPool(String v) {
        int idx = stringPool.addString(v);
        output.writeInt(idx);
    }

}
