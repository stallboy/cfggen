package configgen.value;

import org.jspecify.annotations.Nullable;

import configgen.i18n.TranslationCatalog;
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

    public static void setTranslated(CfgValue cfgValue, @Nullable TranslationCatalog catalog){
        for (VTable vTable : cfgValue.tables()) {
            setTranslatedForTable(vTable, catalog);
        }
    }

    public static void setTranslatedForTable(VTable vTable, @Nullable TranslationCatalog catalog) {
        if (catalog == null) {
            return;
        }
        TranslationCatalog.TextFinder textFinder = catalog.getTextFinder(vTable.name());
        if (textFinder == null) {
            return;
        }

        if (!HasText.hasText(vTable.schema())) {
            return;
        }

        ForeachValue.foreachVTable(new SetTextTranslatedVisitor(textFinder), vTable);
    }


    private static class SetTextTranslatedVisitor extends ValueVisitorForPrimitive {
        private final TranslationCatalog.TextFinder textFinder;

        public SetTextTranslatedVisitor(TranslationCatalog.TextFinder textFinder) {
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
