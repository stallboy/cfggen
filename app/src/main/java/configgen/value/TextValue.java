package configgen.value;

import configgen.ctx.Context;
import configgen.ctx.LangTextFinder;
import configgen.ctx.TextFinder;
import configgen.schema.HasText;
import configgen.value.ForeachValue.ValueVisitorForPrimitive;

import java.util.List;

import static configgen.value.CfgValue.*;

public class TextValue {

    public static boolean hasText(Value value) {
        switch (value) {
            case PrimitiveValue pv -> {
                return pv instanceof VText;
            }
            case VStruct vStruct -> {
                if (!HasText.hasText(vStruct.schema())) {
                    return false;
                }

                return vStruct.values().stream().anyMatch(TextValue::hasText);
            }
            case VInterface vInterface -> {
                if (!HasText.hasText(vInterface.schema())) {
                    return false;
                }
                return vInterface.child().values().stream().anyMatch(TextValue::hasText);
            }
            case VList vList -> {
                return vList.valueList().stream().anyMatch(TextValue::hasText);
            }
            case VMap vMap -> {
                // Map的key不会有Text，在CfgSchemaResolve时检查
                return vMap.valueMap().values().stream().anyMatch(TextValue::hasText);
            }
        }
    }


    public static void setTranslatedForTable(VTable vTable, Context context, CfgValueStat cfgValueStat) {
        LangTextFinder langTextFinder = context.nullableLangTextFinder();
        if (langTextFinder == null) {
            return;
        }
        TextFinder textFinder = langTextFinder.getTableTextFinder(vTable.name());
        if (textFinder == null) {
            return;
        }

        if (!HasText.hasText(vTable.schema())) {
            return;
        }

        ForeachValue.foreachVTable(new SetTextTranslatedVisitor(textFinder, cfgValueStat), vTable);

    }


    public static class SetTextTranslatedVisitor extends ValueVisitorForPrimitive {
        private final TextFinder textFinder;
        private final CfgValueStat cfgValueStat;

        public SetTextTranslatedVisitor(TextFinder textFinder, CfgValueStat cfgValueStat) {
            this.textFinder = textFinder;
            this.cfgValueStat = cfgValueStat;
        }

        @Override
        public void visitPrimitive(PrimitiveValue primitiveValue, Value pk, List<String> fieldChain) {
            if (primitiveValue instanceof VText vText) {
                String translated = textFinder.findText(pk.packStr(), fieldChain, vText.original());
                if (translated != null) {
                    vText.setTranslated(translated);
                } else if (cfgValueStat != null){
                    cfgValueStat.incTranslateMissCount();
                }
            }
        }

    }
}
