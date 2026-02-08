package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import configgen.i18n.LangSwitchableRuntime;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MultiLangVTableSerializer implements ForeachValue.ValueVisitor {
    private final @NotNull ConfigOutput output;
    private final @NotNull LangSwitchableRuntime langSwitchRuntime;
    private final @NotNull StringPool stringPool;
    private final @NotNull LangTextPool langTextPool;

    public MultiLangVTableSerializer(@NotNull ConfigOutput output,
                                     @NotNull LangSwitchableRuntime langSwitchRuntime,
                                     @NotNull StringPool stringPool,
                                     @NotNull LangTextPool langTextPool) {
        this.output = output;
        this.langSwitchRuntime = langSwitchRuntime;
        this.stringPool = stringPool;
        this.langTextPool = langTextPool;
    }


    public void serialize(CfgValue.VTable vTable) {
        ForeachValue.foreachVTable(this, vTable);
    }


    @Override
    public void visitPrimitive(CfgValue.PrimitiveValue primitiveValue, CfgValue.Value pk, List<String> fieldChain) {
        switch (primitiveValue) {
            case CfgValue.VBool vBool -> output.writeBool(vBool.value());
            case CfgValue.VInt vInt -> output.writeInt(vInt.value());
            case CfgValue.VLong vLong -> output.writeLong(vLong.value());
            case CfgValue.VFloat vFloat -> output.writeFloat(vFloat.value());

            case CfgValue.VString vStr -> {
                writeStringInPool(vStr.value());
            }

            case CfgValue.VText vText -> {
                String[] i18nStrings = langSwitchRuntime.findAllLangText(pk.packStr(), fieldChain, vText.value());
                int idx = langTextPool.addText(i18nStrings);
                output.writeInt(idx);
            }
        }
    }

    @Override
    public void visitVList(CfgValue.VList vList, CfgValue.Value pk, List<String> fieldChain) {
        output.writeInt(vList.valueList().size());
    }

    @Override
    public void visitVMap(CfgValue.VMap vMap, CfgValue.Value pk, List<String> fieldChain) {
        output.writeInt(vMap.valueMap().size());
    }


    @Override
    public void visitVInterface(CfgValue.VInterface vInterface, CfgValue.Value pk, List<String> fieldChain) {
        writeStringInPool(vInterface.child().name());
    }

    @Override
    public void visitVStruct(CfgValue.VStruct vStruct, CfgValue.Value pk, List<String> fieldChain) {

    }

    private void writeStringInPool(String v) {
        int idx = stringPool.addString(v);
        output.writeInt(idx);
    }

}
