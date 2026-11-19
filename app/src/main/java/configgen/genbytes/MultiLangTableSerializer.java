package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import configgen.i18n.TableTranslationLookup;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;

import java.util.List;

public record MultiLangTableSerializer(ConfigOutput output,
                                       StringPool stringPool,
                                       LangTextPool langTextPool,
                                       TableTranslationLookup tableTranslationLookup)
        implements ForeachValue.ValueVisitor {


    public void serialize(CfgValue.VTable vTable) {
        output.writeInt(vTable.valueList().size());
        ForeachValue.foreachVTable(this, vTable);
    }


    @Override
    public void visitPrimitive(CfgValue.PrimitiveValue primitiveValue, CfgValue.Value pk, List<String> fieldChain) {
        switch (primitiveValue) {
            case CfgValue.VBool vBool -> output.writeBool(vBool.value());
            case CfgValue.VInt vInt -> output.writeInt(vInt.value());
            case CfgValue.VLong vLong -> output.writeLong(vLong.value());
            case CfgValue.VFloat vFloat -> output.writeFloat(vFloat.value());

            case CfgValue.VString vStr -> writeStringInPool(vStr.value());

            case CfgValue.VText vText -> {
                String[] localizedTexts = tableTranslationLookup.resolveTextsForAllLanguages(
                        pk.packStr(), fieldChain, vText.value());
                int idx = langTextPool.addText(localizedTexts);
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
        // ignore
    }

    private void writeStringInPool(String v) {
        int idx = stringPool.addString(v);
        output.writeInt(idx);
    }

}
