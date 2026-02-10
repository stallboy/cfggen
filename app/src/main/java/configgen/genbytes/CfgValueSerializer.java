package configgen.genbytes;

import configgen.genjava.ConfigOutput;
import configgen.i18n.LangSwitchableRuntime;
import configgen.util.Logger;
import configgen.value.CfgValue;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;

import static configgen.value.CfgValue.*;

public record CfgValueSerializer(@NotNull ConfigOutput output,
                                 @NotNull StringPool stringPool,
                                 @NotNull LangTextPool langTextPool,
                                 LangSwitchableRuntime langSwitchRuntime) {

    public void serialize(CfgValue cfgValue) {
        //1. 写入table count
        output.writeInt(cfgValue.vTableMap().size());

        for (VTable vTable : cfgValue.sortedTables()) {
            byte[] tableBytes = serializeTableBytes(vTable);

            Logger.verbose(vTable.name() + ": " + tableBytes.length);
            //2. 写入table name
            output.writeString(vTable.name());

            //3. 写入总大小
            output.writeInt(tableBytes.length);
            //4. 具体数据
            output.writeRawBytes(tableBytes);
        }
    }

    private byte @NotNull [] serializeTableBytes(VTable vTable) {
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
