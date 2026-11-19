package configgen.data;

import org.jspecify.annotations.Nullable;


import java.util.List;

public record ReadResult(List<OneSheet> sheets,
                         CfgDataStat stat,
                         @Nullable String nullableAddTag) {

    public record OneSheet(String tableName,
                           CfgData.DRawSheet sheet) {
    }
}
