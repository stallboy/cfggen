package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import configgen.i18n.LangSwitchableRuntime;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

import static configgen.value.CfgValue.*;

public final class CfgValueSerializer {
    private CfgValueSerializer() {}
    public static byte @NotNull [] serializeTableBytes(VTable vTable, StringPool stringPool,
                                                     LangTextPool langTextPool, LangSwitchableRuntime langSwitchRuntime) {
        if (langSwitchRuntime != null) {
            langSwitchRuntime.enterTable(vTable.name());
        }

        ByteArrayOutputStream tableStream = new ByteArrayOutputStream(1024);
        try (ConfigOutput tableOut = new ConfigOutput(tableStream)) {
            if (langSwitchRuntime == null) {
                // 不需要多语言切换时，用这个会更高效
                new TableSerializer(tableOut, stringPool, langTextPool)
                        .serialize(vTable);
            } else {
                // 可能需要pk&fieldChain作为id,去取多语言的text
                new MultiLangTableSerializer(tableOut, stringPool, langTextPool, langSwitchRuntime)
                        .serialize(vTable);
            }
        }

        return tableStream.toByteArray();
    }


}
