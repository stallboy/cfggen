package configgen.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ReadResult(@NotNull List<OneSheet> sheets,
                         @NotNull CfgDataStat stat,
                         String nullableAddTag) {

    public record OneSheet(@NotNull String tableName,
                           @NotNull CfgData.DRawSheet sheet) {
    }
}
