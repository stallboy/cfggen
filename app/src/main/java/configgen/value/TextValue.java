package configgen.value;

import configgen.i18n.LangTextFinder;
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

    public static void setTranslated(CfgValue cfgValue, LangTextFinder langFinder){
        for (VTable vTable : cfgValue.tables()) {
            setTranslatedForTable(vTable, langFinder);
        }
    }

    public static void setTranslatedForTable(VTable vTable, LangTextFinder langFinder) {
        if (langFinder == null) {
            return;
        }
        LangTextFinder.TextFinder textFinder = langFinder.getTextFinder(vTable.name());
        if (textFinder == null) {
            return;
        }

        if (!HasText.hasText(vTable.schema())) {
            return;
        }

        ForeachValue.foreachVTable(new SetTextTranslatedVisitor(textFinder), vTable);
    }


    private static class SetTextTranslatedVisitor extends ValueVisitorForPrimitive {
        private final LangTextFinder.TextFinder textFinder;

        public SetTextTranslatedVisitor(LangTextFinder.TextFinder textFinder) {
            this.textFinder = textFinder;
        }

        @Override
        public void visitPrimitive(PrimitiveValue primitiveValue, Value pk, List<String> fieldChain) {
            if (primitiveValue instanceof VText vText) {
                String translated = textFinder.findText(pk.packStr(), fieldChain, vText.original());
                vText.setTranslated(translated);
            }
        }

    }
}
